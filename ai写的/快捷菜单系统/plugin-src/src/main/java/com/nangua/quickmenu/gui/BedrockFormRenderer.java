package com.nangua.quickmenu.gui;

import com.nangua.quickmenu.QuickMenuPlugin;
import com.nangua.quickmenu.menu.Action;
import com.nangua.quickmenu.menu.ActionExecutor;
import com.nangua.quickmenu.menu.Menu;
import com.nangua.quickmenu.menu.MenuItem;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.response.SimpleFormResponse;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * 基岩端原生 Form 渲染器。
 *
 * <p>通过 Floodgate 提供的 Cumulus API 向基岩客户端发送原生 SimpleForm。
 * 表单由基岩客户端自身渲染，具备完整的触屏滚动与点击反馈，
 * 不存在 Geyser 箱子界面的拖出物品、高亮异常、无法打开等缺陷。
 *
 * <p><b>线程模型</b>：Cumulus 的响应回调在 Netty 网络线程触发，
 * 因此所有动作执行都通过 {@link ActionExecutor#runAsyncSafe} 切回主线程。
 * 这是基岩端最容易踩的坑——直接在回调里操作背包会导致服务器崩溃。
 *
 * <p><b>颜色码处理</b>：原生 Form 按钮不支持 Minecraft 颜色码（& 或 §），
 * 会显示为乱码字符。因此按钮文本统一使用 {@link ActionExecutor#stripColor} 去除颜色码。
 * 表单标题与正文则保留 § 形式（基岩客户端支持标题颜色）。
 */
public final class BedrockFormRenderer {

    private final QuickMenuPlugin plugin;

    public BedrockFormRenderer(QuickMenuPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 判断玩家是否为基岩版玩家（通过 Floodgate 认证）。
     *
     * <p>Floodgate 未安装时恒返回 false，插件自动回退到箱子界面。
     */
    public boolean isBedrockPlayer(Player player) {
        if (!plugin.isFloodgateAvailable()) {
            return false;
        }
        try {
            FloodgateApi api = FloodgateApi.getInstance();
            return api.isFloodgatePlayer(player.getUniqueId());
        } catch (NoClassDefFoundError | ExceptionInInitializerError ex) {
            // Floodgate 已卸载或初始化失败
            return false;
        }
    }

    /**
     * 向基岩玩家发送指定菜单的原生表单。
     *
     * @return true 表示发送成功；false 表示玩家不是基岩玩家或发送失败
     */
    public boolean render(Player player, Menu menu) {
        if (!isBedrockPlayer(player)) {
            return false;
        }

        // 按顺序收集玩家有权限看到的菜单项。
        // 必须维护这个列表：SimpleFormResponse.clickedButtonId() 返回的是
        // 玩家点击的按钮序号，而非菜单项 id，若菜单项因权限被跳过则序号会错位。
        //
        // 这里刻意不使用 button(text, consumer) 的逐按钮回调，
        // 而是统一在 validResultHandler 中按索引分发。原因：两种机制并存时
        // 难以保证只触发一次，统一入口可避免动作被重复执行。
        List<MenuItem> visibleItems = new ArrayList<>();
        SimpleForm.Builder builder = SimpleForm.builder()
                .title(ActionExecutor.color(menu.getTitle()));

        String content = menu.getBedrockContent();
        if (content == null || content.isEmpty()) {
            content = menu.getTitle();
        }
        builder.content(ActionExecutor.color(content));

        for (MenuItem item : menu.getItems()) {
            if (!plugin.getActionExecutor().hasItemPermission(player, item)) {
                continue;
            }
            visibleItems.add(item);

            String label = item.getButtonLabel();
            if (label == null || label.isEmpty()) {
                // 未配置专用按钮文本时，用显示名并去除颜色码
                label = ActionExecutor.stripColor(item.getDisplayName());
            } else {
                label = ActionExecutor.stripColor(label);
            }
            builder.button(label);
        }

        // 返回按钮：有上级菜单时追加，固定位于列表末尾
        String backMenu = menu.getBackMenu();
        boolean hasBackButton = backMenu != null && !backMenu.isEmpty();
        if (hasBackButton) {
            builder.button(ActionExecutor.stripColor("&c返回"));
        }

        final int visibleCount = visibleItems.size();

        // 统一的结果处理：区分正常点击、返回、关闭表单、无效响应
        builder.validResultHandler((SimpleFormResponse response) -> {
            int index = response.clickedButtonId();

            // 返回按钮
            if (hasBackButton && index == visibleCount) {
                List<Action> backAction = new ArrayList<>(1);
                backAction.add(new Action(
                        com.nangua.quickmenu.menu.ActionType.OPEN_MENU, backMenu));
                plugin.getActionExecutor().runAsyncSafe(player, backAction);
                return;
            }

            if (index < 0 || index >= visibleCount) {
                if (plugin.getSettings().isDebug()) {
                    plugin.getLogger().info("基岩表单点击索引越界: " + index
                            + " (菜单 " + menu.getId() + ", 可见项 " + visibleCount + ")");
                }
                return;
            }

            // 此回调在 Netty 网络线程执行，执行器内部会切回主线程
            MenuItem clicked = visibleItems.get(index);
            plugin.getActionExecutor().runAsyncSafe(player, clicked.getActions());
        });

        builder.closedResultHandler(() -> {
            if (plugin.getSettings().isDebug()) {
                plugin.getLogger().info("玩家 " + player.getName() + " 关闭了基岩表单: " + menu.getId());
            }
        });

        builder.invalidResultHandler(() -> {
            plugin.getLogger().warning("玩家 " + player.getName()
                    + " 的基岩表单响应无效，菜单: " + menu.getId());
        });

        // 发送表单
        try {
            FloodgateApi api = FloodgateApi.getInstance();
            FloodgatePlayer floodgatePlayer = api.getPlayer(player.getUniqueId());
            if (floodgatePlayer == null) {
                return false;
            }
            return floodgatePlayer.sendForm(builder);
        } catch (NoClassDefFoundError | ExceptionInInitializerError ex) {
            plugin.getLogger().warning("Floodgate API 不可用，无法发送原生表单: " + ex.getMessage());
            return false;
        }
    }

    /**
     * 获取基岩玩家的客户端信息（用于 /qm info 调试）。
     *
     * <p>此处刻意只调用返回 String 的 API 方法。Floodgate 的
     * getDeviceOs() / getInputMode() 返回的枚举类位于 geyser common 模块，
     * 未随 floodgate-api 一同发布，直接调用会导致编译期缺少类文件。
     * 因此改用 getVersion() 与 getLanguageCode()，二者均在 API jar 内。
     *
     * @return 客户端信息；无法获取时返回 null
     */
    public String getDeviceInfo(Player player) {
        if (!isBedrockPlayer(player)) {
            return null;
        }
        try {
            FloodgateApi api = FloodgateApi.getInstance();
            FloodgatePlayer floodgatePlayer = api.getPlayer(player.getUniqueId());
            if (floodgatePlayer == null) {
                return null;
            }
            return "客户端 " + floodgatePlayer.getVersion()
                    + " / 语言 " + floodgatePlayer.getLanguageCode();
        } catch (NoClassDefFoundError ex) {
            return null;
        }
    }
}
