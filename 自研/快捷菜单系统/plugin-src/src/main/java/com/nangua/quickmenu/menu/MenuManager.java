package com.nangua.quickmenu.menu;

import com.nangua.quickmenu.QuickMenuPlugin;
import com.nangua.quickmenu.gui.BedrockFormRenderer;
import com.nangua.quickmenu.gui.ChestMenuRenderer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;

/**
 * 菜单管理器 —— 两端分发的决策中枢。
 *
 * <p>分发逻辑：
 * <pre>
 *   玩家请求打开菜单
 *        ↓
 *   菜单是否存在？ ──否──→ 提示错误
 *        ↓是
 *   是基岩玩家 且 启用原生表单 且 Floodgate 可用？
 *        ├─是─→ 发送 Cumulus SimpleForm
 *        └─否─→ 打开箱子 GUI（Java 玩家，或基岩端回退）
 * </pre>
 *
 * <p>回退设计：若原生表单发送失败（Floodgate 异常、客户端版本不兼容等），
 * 自动回退到箱子界面，保证玩家至少能用到菜单功能。
 */
public final class MenuManager {

    private final QuickMenuPlugin plugin;
    private final ChestMenuRenderer chestRenderer;

    /**
     * 基岩端渲染器。<b>可能为 null</b>。
     *
     * <p>BedrockFormRenderer 的方法签名直接引用 Cumulus 类型（SimpleForm 等）。
     * 若服务器未安装 Floodgate，这些类在 classpath 中不存在，
     * JVM 在链接该类时会抛出 NoClassDefFoundError —— 这会导致插件启动失败。
     *
     * <p>因此这里做两件事：
     * <ol>
     *   <li>只在确认 Floodgate 可用后才实例化该类（延迟加载）</li>
     *   <li>实例化过程用 try/catch 包住，失败则保持 null 并回退箱子界面</li>
     * </ol>
     * 这样插件在纯 Java 服务器（无 Floodgate）上也能正常加载运行。
     */
    private BedrockFormRenderer formRenderer;

    public MenuManager(QuickMenuPlugin plugin) {
        this.plugin = plugin;
        this.chestRenderer = new ChestMenuRenderer(plugin);
        initFormRenderer();
    }

    /**
     * 尝试初始化基岩端渲染器。失败不影响插件运行。
     */
    private void initFormRenderer() {
        if (!plugin.isFloodgateAvailable()) {
            return;
        }
        try {
            this.formRenderer = new BedrockFormRenderer(plugin);
        } catch (NoClassDefFoundError | ExceptionInInitializerError ex) {
            this.formRenderer = null;
            plugin.getLogger().warning("Floodgate 存在但 Cumulus API 加载失败，"
                    + "基岩端将回退使用箱子界面: " + ex.getMessage());
        }
    }

    public ChestMenuRenderer getChestRenderer() {
        return chestRenderer;
    }

    /**
     * 获取基岩端渲染器，<b>可能返回 null</b>。
     * 调用方必须做空判断，或直接改用本类的 {@link #isBedrockPlayer} 等安全封装方法。
     */
    public BedrockFormRenderer getFormRenderer() {
        return formRenderer;
    }

    /**
     * 空安全地判断玩家是否为基岩玩家。
     * Floodgate 未安装或渲染器初始化失败时恒返回 false。
     */
    public boolean isBedrockPlayer(Player player) {
        return formRenderer != null && formRenderer.isBedrockPlayer(player);
    }

    /**
     * 空安全地获取基岩客户端信息，用于 /qm info。
     *
     * @return 客户端信息；玩家非基岩版或渲染器不可用时返回 null
     */
    public String getDeviceInfo(Player player) {
        return formRenderer == null ? null : formRenderer.getDeviceInfo(player);
    }

    /**
     * 打开玩家配置的默认菜单。
     */
    public void openDefaultMenu(Player player) {
        openMenu(player, plugin.getSettings().getDefaultMenu());
    }

    /**
     * 打开指定 id 的菜单，按客户端类型自动分发。
     *
     * @param player 目标玩家
     * @param menuId 菜单 id
     */
    public void openMenu(Player player, String menuId) {
        Menu menu = plugin.getSettings().findMenu(menuId);
        if (menu == null) {
            player.sendMessage(ActionExecutor.color(
                    plugin.getConfig().getString("messages.menu-not-found", "&c菜单不存在")
                            .replace("{menu}", String.valueOf(menuId))));
            return;
        }

        // 基岩端：优先原生 Form
        // 使用空安全的 isBedrockPlayer 封装方法，formRenderer 为 null 时恒返回 false
        boolean bedrockNative = plugin.getSettings().isBedrockUseNativeForm()
                && isBedrockPlayer(player);

        if (bedrockNative && formRenderer != null) {
            boolean sent = formRenderer.render(player, menu);
            if (sent) {
                if (plugin.getSettings().isDebug()) {
                    plugin.getLogger().info("向基岩玩家 " + player.getName()
                            + " 发送原生表单: " + menu.getId());
                }
                return;
            }
            // 发送失败，回退到箱子界面
            plugin.getLogger().warning("向 " + player.getName()
                    + " 发送原生表单失败，回退到箱子界面 (菜单: " + menu.getId() + ")");
        }

        // Java 端（或基岩端回退）：箱子 GUI
        if (!plugin.getSettings().isJavaUseChestGui() && !bedrockNative) {
            player.sendMessage(ActionExecutor.color(
                    plugin.getConfig().getString("messages.chest-gui-disabled",
                            "&c箱子菜单已被禁用")));
            return;
        }
        chestRenderer.render(player, menu);
    }

    /**
     * 打开在线玩家选择器（tpa / tpahere 选人界面）。
     *
     * <p>动态列出当前在线玩家（排除自己），玩家点击某人后执行
     * 传入的命令模板（{@code {target}} 会被替换为所点玩家的名字）。
     * 两端分发规则与 {@link #openMenu} 一致：基岩玩家优先原生表单，
     * 发送失败自动回退箱子界面。
     *
     * @param player          目标玩家
     * @param commandTemplate 命令模板，如 {@code tpa {target}}
     */
    public void openPlayerSelector(Player player, String commandTemplate) {
        boolean bedrockNative = plugin.getSettings().isBedrockUseNativeForm()
                && isBedrockPlayer(player);

        if (bedrockNative && formRenderer != null) {
            boolean sent = formRenderer.renderPlayerSelector(player, commandTemplate);
            if (sent) {
                return;
            }
            plugin.getLogger().warning("向 " + player.getName()
                    + " 发送玩家选择器表单失败，回退到箱子界面");
        }

        chestRenderer.renderPlayerSelector(player, commandTemplate);
    }

    /**
     * 打开背包物品选择器（估价等选物界面）。
     *
     * <p>动态列出玩家背包中的物品，玩家点击某物品后执行传入的命令模板
     * （{@code {item}} 会被替换为所点物品的材质名小写）。
     * 两端分发规则与 {@link #openPlayerSelector} 一致。
     *
     * @param player          目标玩家
     * @param commandTemplate 命令模板，如 {@code worth {item}}
     */
    public void openItemSelector(Player player, String commandTemplate) {
        boolean bedrockNative = plugin.getSettings().isBedrockUseNativeForm()
                && isBedrockPlayer(player);

        if (bedrockNative && formRenderer != null) {
            boolean sent = formRenderer.renderItemSelector(player, commandTemplate);
            if (sent) {
                return;
            }
            plugin.getLogger().warning("向 " + player.getName()
                    + " 发送物品选择器表单失败，回退到箱子界面");
        }

        chestRenderer.renderItemSelector(player, commandTemplate);
    }

    /**
     * 关闭所有玩家打开中的菜单界面（服务器关闭时调用）。
     */
    public void closeAll() {
        try {
            Collection<? extends Player> players = Bukkit.getOnlinePlayers();
            for (Player player : players) {
                player.closeInventory();
            }
        } catch (Exception ex) {
            // 关闭阶段出现异常不应阻塞服务器停止流程
            plugin.getLogger().warning("关闭菜单界面时出现异常: " + ex.getMessage());
        }
    }
}
