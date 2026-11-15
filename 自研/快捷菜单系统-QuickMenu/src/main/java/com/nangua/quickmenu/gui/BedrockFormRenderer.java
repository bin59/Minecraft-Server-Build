package com.nangua.quickmenu.gui;

import com.nangua.quickmenu.QuickMenuPlugin;
import com.nangua.quickmenu.menu.Action;
import com.nangua.quickmenu.menu.ActionExecutor;
import com.nangua.quickmenu.menu.ActionType;
import com.nangua.quickmenu.menu.InputForm;
import com.nangua.quickmenu.menu.Menu;
import com.nangua.quickmenu.menu.MenuItem;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.component.util.ComponentType;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.response.CustomFormResponse;
import org.geysermc.cumulus.response.SimpleFormResponse;
import org.geysermc.cumulus.util.FormImage;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * 基岩端原生 Form 渲染器。
 *
 * <p>通过 Floodgate 提供的 Cumulus API 向基岩客户端发送原生表单。
 * 表单由基岩客户端自身渲染，具备完整的触屏滚动与点击反馈，
 * 不存在 Geyser 箱子界面的拖出物品、高亮异常、无法打开等缺陷。
 *
 * <p><b>线程模型</b>：Cumulus 的响应回调在 Netty 网络线程触发，
 * 因此所有动作执行都通过 {@link ActionExecutor#runAsyncSafe} 或
 * {@code Bukkit.getScheduler().runTask} 切回主线程。
 *
 * <p><b>外观</b>：按钮图标、两行按钮文本、标题分隔线、返回/关闭/空态
 * 全部由 {@link BedrockFormStyle} 统一提供；配置改动后 {@code /qm reload} 即生效。
 *
 * <p><b>说明页</b>：动作只有 {@code [close]} + 若干 {@code [message]} 的菜单项
 * （旧版会在聊天栏刷用法文字），基岩端改为弹一张说明表单：正文是整理后的说明，
 * 底部两个按钮「返回菜单 / 关闭」。Java 端仍走原来的聊天提示，行为不变。
 *
 * <p><b>依赖说明</b>：cumulus 与 floodgate API 均由服务器运行时提供，
 * 编译期以 systemPath 参与、不打进插件 jar。未安装 Floodgate 时本类不会被实例化。
 */
public final class BedrockFormRenderer {

    private final QuickMenuPlugin plugin;
    private final BedrockFormStyle style;

    public BedrockFormRenderer(QuickMenuPlugin plugin) {
        this.plugin = plugin;
        this.style = new BedrockFormStyle(plugin);
    }

    /** 判断玩家是否为基岩版玩家（通过 Floodgate 认证）。 */
    public boolean isBedrockPlayer(Player player) {
        if (!plugin.isFloodgateAvailable()) {
            return false;
        }
        try {
            FloodgateApi api = FloodgateApi.getInstance();
            return api.isFloodgatePlayer(player.getUniqueId());
        } catch (NoClassDefFoundError | ExceptionInInitializerError ex) {
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
        style.reload();

        // 按顺序收集玩家有权限看到的菜单项。权限过滤会让按钮序号前移，
        // 因此必须维护 visibleItems，SimpleFormResponse.clickedButtonId() 才能正确对应。
        List<MenuItem> visibleItems = new ArrayList<>();
        SimpleForm.Builder builder = SimpleForm.builder()
                .title(style.title(menu.getTitle()));

        String backMenu = menu.getBackMenu();
        boolean hasBackButton = backMenu != null && !backMenu.isEmpty();

        for (MenuItem item : menu.getItems()) {
            if (!plugin.getActionExecutor().hasItemPermission(player, item)) {
                continue;
            }
            if (!item.visibleToday()) {
                continue;
            }
            visibleItems.add(item);
            style.itemButton(builder, item);
        }

        if (hasBackButton) {
            style.backButton(builder);
        }

        // 重新设置带准确按钮数量的正文（SimpleForm.Builder 允许重复调用 content）
        builder.content(style.contentFor(menu, visibleItems.size() + (hasBackButton ? 1 : 0)));

        final int visibleCount = visibleItems.size();

        builder.validResultHandler((SimpleFormResponse response) -> {
            int index = response.clickedButtonId();

            if (hasBackButton && index == visibleCount) {
                List<Action> backAction = new ArrayList<>(1);
                backAction.add(new Action(ActionType.OPEN_MENU, backMenu));
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

            MenuItem clicked = visibleItems.get(index);

            // 纯说明项：不再往聊天栏刷文字，改成一张带按钮的说明表单
            if (isInfoOnly(clicked)) {
                Bukkit.getScheduler().runTask(plugin, () -> renderInfoForm(player, menu, clicked));
                return;
            }

            plugin.getActionExecutor().runAsyncSafe(player, clicked.getActions());
        });

        builder.closedResultHandler(() -> {
            if (plugin.getSettings().isDebug()) {
                plugin.getLogger().info("玩家 " + player.getName() + " 关闭了基岩表单: " + menu.getId());
            }
        });

        builder.invalidResultHandler(() ->
                plugin.getLogger().warning("玩家 " + player.getName()
                        + " 的基岩表单响应无效，菜单: " + menu.getId()));

        return send(player, builder);
    }

    /**
     * 说明表单：把菜单项里所有 [message] 文本整理成一段正文，
     * 底部给「返回菜单 / 关闭」两个按钮。原聊天提示不再发送。
     */
    public boolean renderInfoForm(Player player, Menu parent, MenuItem item) {
        if (!isBedrockPlayer(player)) {
            return false;
        }
        style.reload();

        List<String> messages = new ArrayList<>();
        for (Action action : item.getActions()) {
            if (action.getType() == ActionType.MESSAGE && !action.getValue().isEmpty()) {
                messages.add(action.getValue());
            }
        }
        if (messages.isEmpty()) {
            return false;
        }

        SimpleForm.Builder builder = SimpleForm.builder()
                .title(style.infoTitle(ActionExecutor.stripColor(item.getDisplayName())))
                .content(style.infoContent(messages));

        style.backButton(builder);
        style.closeButton(builder);

        builder.validResultHandler((SimpleFormResponse response) -> {
            if (response.clickedButtonId() == 0) {
                List<Action> back = new ArrayList<>(1);
                back.add(new Action(ActionType.OPEN_MENU, parent.getId()));
                plugin.getActionExecutor().runAsyncSafe(player, back);
            }
            // index 1 = 关闭，不做任何事
        });

        builder.closedResultHandler(() -> {
            if (plugin.getSettings().isDebug()) {
                plugin.getLogger().info("玩家 " + player.getName() + " 关闭了说明表单: "
                        + parent.getId() + "." + item.getId());
            }
        });

        builder.invalidResultHandler(() ->
                plugin.getLogger().warning("玩家 " + player.getName()
                        + " 的说明表单响应无效: " + parent.getId() + "." + item.getId()));

        return send(player, builder);
    }

    /** 在线玩家选择器（带图标、两行按钮）。 */
    public boolean renderPlayerSelector(Player player, String commandTemplate) {
        if (!isBedrockPlayer(player)) {
            return false;
        }
        style.reload();

        SimpleForm.Builder builder = SimpleForm.builder()
                .title(style.title(plugin.getSettings().getPlayerSelectorTitle()));

        List<String> targets = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getUniqueId().equals(player.getUniqueId())) {
                continue;
            }
            targets.add(online.getName());
        }

        builder.content(style.content(plugin.getSettings().getPlayerSelectorContent(),
                targets.isEmpty() ? 1 : targets.size()));

        String sub = ActionExecutor.stripColor(plugin.getSettings().getPlayerSelectorLore());
        FormImage icon = style.imageOf("PLAYER_HEAD");
        for (String name : targets) {
            style.button(builder, name + (sub.isEmpty() ? "" : "\n" + sub), icon);
        }
        if (targets.isEmpty()) {
            style.emptyButton(builder, plugin.getSettings().getPlayerSelectorNoPlayers());
        }

        builder.validResultHandler((SimpleFormResponse response) -> {
            int index = response.clickedButtonId();
            if (index < 0 || index >= targets.size()) {
                return;
            }
            String target = targets.get(index);
            Bukkit.getScheduler().runTask(plugin, () ->
                    player.performCommand(commandTemplate.replace("{target}", target)));
        });

        attachDiagnostics(builder, player, "玩家选择器");
        return send(player, builder);
    }

    /** 背包物品选择器（按材质给出对应图标）。 */
    public boolean renderItemSelector(Player player, String commandTemplate) {
        if (!isBedrockPlayer(player)) {
            return false;
        }
        style.reload();

        SimpleForm.Builder builder = SimpleForm.builder()
                .title(style.title(plugin.getSettings().getItemSelectorTitle()));

        List<String> materials = new ArrayList<>();
        for (org.bukkit.inventory.ItemStack stack : player.getInventory().getContents()) {
            if (stack == null || stack.getType() == org.bukkit.Material.AIR) {
                continue;
            }
            if (plugin.getMenuManager().getChestRenderer().isTriggerItem(stack)) {
                continue;
            }
            String matName = stack.getType().name().toLowerCase();
            if (materials.contains(matName)) {
                continue;
            }
            materials.add(matName);
            org.bukkit.inventory.meta.ItemMeta meta = stack.getItemMeta();
            String display = (meta != null && meta.hasDisplayName())
                    ? ActionExecutor.stripColor(meta.getDisplayName())
                    : stack.getType().name();
            style.button(builder, display + "\n" + matName, style.imageOf(stack.getType().name()));
        }

        builder.content(style.content(plugin.getSettings().getItemSelectorLore(),
                materials.isEmpty() ? 1 : materials.size()));

        if (materials.isEmpty()) {
            style.emptyButton(builder, plugin.getSettings().getItemSelectorNoItems());
        }

        builder.validResultHandler((SimpleFormResponse response) -> {
            int index = response.clickedButtonId();
            if (index < 0 || index >= materials.size()) {
                return;
            }
            String item = materials.get(index);
            Bukkit.getScheduler().runTask(plugin, () ->
                    player.performCommand(commandTemplate.replace("{item}", item)));
        });

        attachDiagnostics(builder, player, "物品选择器");
        return send(player, builder);
    }

    /** 我的领地选择器（带图标、两行按钮、返回按钮）。 */
    public boolean renderResidenceSelector(Player player, String commandTemplate) {
        if (!isBedrockPlayer(player)) {
            return false;
        }
        style.reload();

        SimpleForm.Builder builder = SimpleForm.builder()
                .title(style.title(plugin.getSettings().getResidenceSelectorTitle()));

        List<String> residences = com.nangua.quickmenu.hook.ResidenceHook.getOwnResidenceNames(player);
        String sub = ActionExecutor.stripColor(plugin.getSettings().getResidenceSelectorLore());
        FormImage icon = style.imageOf("GRASS_BLOCK");

        builder.content(style.content(plugin.getSettings().getResidenceSelectorContent(),
                residences.isEmpty() ? 2 : residences.size() + 1));

        if (residences.isEmpty()) {
            style.emptyButton(builder, plugin.getSettings().getResidenceSelectorNoResidences());
        } else {
            for (String name : residences) {
                style.button(builder, name + (sub.isEmpty() ? "" : "\n" + sub), icon);
            }
        }
        style.backButton(builder);

        // 空列表时按钮顺序为 [提示, 返回]，返回按钮的 index 是 1；
        // 非空时返回按钮位于最后一个领地之后。
        final int backIndex = residences.isEmpty() ? 1 : residences.size();

        builder.validResultHandler((SimpleFormResponse response) -> {
            int index = response.clickedButtonId();

            if (index == backIndex) {
                String backMenu = plugin.getSettings().getResidenceSelectorBackMenu();
                if (backMenu != null && !backMenu.isEmpty()) {
                    List<Action> backAction = new ArrayList<>(1);
                    backAction.add(new Action(ActionType.OPEN_MENU, backMenu));
                    plugin.getActionExecutor().runAsyncSafe(player, backAction);
                }
                return;
            }

            if (index < 0 || index >= residences.size()) {
                return;
            }
            String residence = residences.get(index);
            Bukkit.getScheduler().runTask(plugin, () ->
                    player.performCommand(commandTemplate.replace("{residence}", residence)));
        });

        attachDiagnostics(builder, player, "领地选择器");
        return send(player, builder);
    }

    /**
     * 参数输入表单（CustomForm）：把需要参数的指令做成「点按钮 → 填参数 → 提交执行」。
     *
     * <p>第一个组件是说明 label（Cumulus 会让它占用一个响应索引），
     * 因此字段索引通过 {@code getComponentTypes()} 逐项算出，不写死下标。
     */
    public boolean renderInputForm(Player player, InputForm form) {
        if (!isBedrockPlayer(player)) {
            return false;
        }
        style.reload();

        List<InputForm.Field> fields = form.getFields();
        if (fields.isEmpty() || form.getCommand().isEmpty()) {
            return false;
        }

        CustomForm.Builder builder = CustomForm.builder()
                .title(style.title(form.getTitle()));
        FormImage icon = style.imageOf("WRITABLE_BOOK");
        if (icon != null) {
            builder.icon(icon);
        }

        String notice = form.getContent().isEmpty()
                ? style.submitLabel()
                : ActionExecutor.color(form.getContent());
        builder.label(notice);

        // 与 fields 等长：INPUT 记 null，DROPDOWN / PLAYER 记可选项
        List<List<String>> optionLists = new ArrayList<>();
        for (InputForm.Field field : fields) {
            switch (field.getType()) {
                case DROPDOWN: {
                    List<String> opts = new ArrayList<>(field.getOptions());
                    if (opts.isEmpty()) {
                        opts.add("");
                    }
                    optionLists.add(opts);
                    builder.dropdown(ActionExecutor.color(field.getLabel()), opts, 0);
                    break;
                }
                case PLAYER: {
                    List<String> opts = new ArrayList<>();
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        if (!online.getUniqueId().equals(player.getUniqueId())) {
                            opts.add(online.getName());
                        }
                    }
                    if (opts.isEmpty()) {
                        opts.add(player.getName());
                    }
                    optionLists.add(opts);
                    builder.dropdown(ActionExecutor.color(field.getLabel()), opts, 0);
                    break;
                }
                default:
                    optionLists.add(null);
                    builder.input(ActionExecutor.color(field.getLabel()),
                            ActionExecutor.stripColor(field.getPlaceholder()),
                            ActionExecutor.stripColor(field.getDefaultValue()));
            }
        }

        builder.validResultHandler((CustomFormResponse response) -> {
            // label 组件会占位，逐类型扫描出各字段的真实响应索引
            List<ComponentType> types = response.getComponentTypes();
            int[] indices = new int[fields.size()];
            int found = 0;
            for (int i = 0; i < types.size() && found < fields.size(); i++) {
                ComponentType type = types.get(i);
                if (type == ComponentType.INPUT || type == ComponentType.DROPDOWN) {
                    indices[found++] = i;
                }
            }
            if (found != fields.size()) {
                plugin.getLogger().warning("输入表单响应组件数不匹配: " + form.getId()
                        + "（期望 " + fields.size() + "，实得 " + found + "）");
                return;
            }

            String command = form.getCommand();
            for (int j = 0; j < fields.size(); j++) {
                InputForm.Field field = fields.get(j);
                String value;
                try {
                    if (field.getType() == InputForm.FieldType.INPUT) {
                        value = response.getInput(indices[j]);
                    } else {
                        int selected = response.getDropdown(indices[j]);
                        List<String> opts = optionLists.get(j);
                        value = (opts != null && selected >= 0 && selected < opts.size())
                                ? opts.get(selected) : "";
                    }
                } catch (RuntimeException ex) {
                    value = "";
                }
                if (value == null) {
                    value = "";
                }
                command = command.replace("{" + field.getKey() + "}", value.trim());
            }

            final String finalCommand = command.trim();
            if (finalCommand.isEmpty()) {
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> player.performCommand(finalCommand));
        });

        builder.closedResultHandler(() -> {
            if (plugin.getSettings().isDebug()) {
                plugin.getLogger().info("玩家 " + player.getName() + " 关闭了输入表单: " + form.getId());
            }
        });

        builder.invalidResultHandler(() ->
                plugin.getLogger().warning("玩家 " + player.getName()
                        + " 的输入表单响应无效: " + form.getId()));

        return send(player, builder);
    }

    /** 获取基岩玩家的客户端信息（用于 /qm info 调试）。 */
    public String getDeviceInfo(Player player) {
        if (!isBedrockPlayer(player)) {
            return null;
        }
        try {
            FloodgatePlayer floodgatePlayer = floodgate(player);
            if (floodgatePlayer == null) {
                return null;
            }
            return "客户端 " + floodgatePlayer.getVersion()
                    + " / 语言 " + floodgatePlayer.getLanguageCode();
        } catch (NoClassDefFoundError ex) {
            return null;
        }
    }

    // ==================== 内部工具 ====================

    /** 动作里只有 [close] + [message] 时视为「说明项」。 */
    private boolean isInfoOnly(MenuItem item) {
        boolean hasMessage = false;
        for (Action action : item.getActions()) {
            if (action.getType() == ActionType.MESSAGE) {
                hasMessage = true;
            } else if (action.getType() != ActionType.CLOSE) {
                return false;
            }
        }
        return hasMessage;
    }

    private void attachDiagnostics(SimpleForm.Builder builder, Player player, String formName) {
        builder.closedResultHandler(() -> {
            if (plugin.getSettings().isDebug()) {
                plugin.getLogger().info("玩家 " + player.getName() + " 关闭了" + formName + "表单");
            }
        });
        builder.invalidResultHandler(() ->
                plugin.getLogger().warning("玩家 " + player.getName()
                        + " 的" + formName + "表单响应无效"));
    }

    private FloodgatePlayer floodgate(Player player) {
        try {
            return FloodgateApi.getInstance().getPlayer(player.getUniqueId());
        } catch (NoClassDefFoundError | ExceptionInInitializerError ex) {
            plugin.getLogger().warning("Floodgate API 不可用，无法发送原生表单: " + ex.getMessage());
            return null;
        }
    }

    private boolean send(Player player, SimpleForm.Builder builder) {
        FloodgatePlayer floodgatePlayer = floodgate(player);
        return floodgatePlayer != null && floodgatePlayer.sendForm(builder);
    }

    private boolean send(Player player, CustomForm.Builder builder) {
        FloodgatePlayer floodgatePlayer = floodgate(player);
        return floodgatePlayer != null && floodgatePlayer.sendForm(builder);
    }
}
