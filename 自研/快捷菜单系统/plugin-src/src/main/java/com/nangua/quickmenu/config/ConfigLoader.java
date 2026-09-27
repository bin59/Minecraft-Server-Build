package com.nangua.quickmenu.config;

import com.nangua.quickmenu.QuickMenuPlugin;
import com.nangua.quickmenu.menu.Action;
import com.nangua.quickmenu.menu.ActionType;
import com.nangua.quickmenu.menu.Menu;
import com.nangua.quickmenu.menu.MenuItem;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 配置解析器。将 YAML 解析为内存中的 {@link Menu} 对象树。
 *
 * <p>解析只在启动和 /qm reload 时发生一次，之后打开界面全程零 IO、零解析，
 * 这是本插件相比运行时解析表达式类菜单插件（如 DeluxeMenus）的性能优势来源。
 */
public final class ConfigLoader {

    private final QuickMenuPlugin plugin;

    public ConfigLoader(QuickMenuPlugin plugin) {
        this.plugin = plugin;
    }

    /** 加载主设置与全部菜单 */
    public void loadAll(PluginSettings settings) {
        FileConfiguration config = plugin.getConfig();

        loadSettings(config, settings);

        settings.clearMenus();
        loadMenusFromConfig(config, settings);
        loadMenusFromFolder(settings);

        if (settings.getMenus().isEmpty()) {
            plugin.getLogger().warning("未加载到任何菜单，请检查 config.yml 的 menus 节点或 menus/ 目录");
        }
    }

    private void loadSettings(FileConfiguration config, PluginSettings settings) {
        settings.setTriggerItemMaterial(config.getString("trigger-item.material", "COMPASS"));
        settings.setTriggerItemName(config.getString("trigger-item.name", "&6&l快捷菜单"));
        settings.setTriggerItemLore(config.getStringList("trigger-item.lore"));
        settings.setTriggerItemGlowing(config.getBoolean("trigger-item.glowing", true));

        settings.setGiveOnJoin(config.getBoolean("trigger-item.give-on-join", true));
        settings.setGiveSlot(config.getInt("trigger-item.give-slot", 8));
        settings.setStrictItemMatch(config.getBoolean("trigger-item.strict-match", true));

        settings.setClickSound(config.getString("sound.click", "ui.button.click"));
        settings.setClickSoundVolume((float) config.getDouble("sound.click-volume", 0.6d));
        settings.setClickSoundPitch((float) config.getDouble("sound.click-pitch", 1.0d));

        settings.setDebug(config.getBoolean("debug", false));
        settings.setDefaultMenu(config.getString("default-menu", "main"));
        settings.setBedrockUseNativeForm(config.getBoolean("platform.bedrock-native-form", true));
        settings.setJavaUseChestGui(config.getBoolean("platform.java-chest-gui", true));

        settings.setPlayerSelectorTitle(config.getString("player-selector.title", "&6&l选择玩家"));
        settings.setPlayerSelectorContent(config.getString("player-selector.content", "请选择要传送的玩家"));
        settings.setPlayerSelectorLore(config.getString("player-selector.lore", "&7点击向 TA 发送传送请求"));
        settings.setPlayerSelectorNoPlayers(config.getString("player-selector.no-players", "&c当前没有其他在线玩家"));

        settings.setItemSelectorTitle(config.getString("item-selector.title", "&6&l选择物品"));
        settings.setItemSelectorLore(config.getString("item-selector.lore", "&7点击后对所选物品执行操作"));
        settings.setItemSelectorNoItems(config.getString("item-selector.no-items", "&c背包里没有可选择的物品"));
    }

    /** 从主 config.yml 的 menus 节点加载 */
    private void loadMenusFromConfig(FileConfiguration config, PluginSettings settings) {
        if (!config.isConfigurationSection("menus")) {
            return;
        }
        ConfigurationSection root = config.getConfigurationSection("menus");
        if (root == null) {
            return;
        }
        Set<String> keys = root.getKeys(false);
        for (String id : keys) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) {
                continue;
            }
            Menu menu = parseMenu(id, section);
            if (menu != null) {
                settings.addMenu(menu);
            }
        }
    }

    /**
     * 从 plugins/QuickMenu/menus/*.yml 加载额外菜单。
     * 便于把大菜单拆分成独立文件管理，同名 id 时后加载的会覆盖先加载的。
     */
    private void loadMenusFromFolder(PluginSettings settings) {
        File folder = new File(plugin.getDataFolder(), "menus");
        if (!folder.exists() || !folder.isDirectory()) {
            return;
        }
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (files == null || files.length == 0) {
            return;
        }
        for (File file : files) {
            try {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
                ConfigurationSection root = yaml.getConfigurationSection("menu");
                if (root == null) {
                    plugin.getLogger().warning("菜单文件 " + file.getName() + " 缺少 menu 节点，已跳过");
                    continue;
                }
                String id = root.getString("id", file.getName().replace(".yml", ""));
                Menu menu = parseMenu(id, root);
                if (menu != null) {
                    settings.addMenu(menu);
                    if (settings.isDebug()) {
                        plugin.getLogger().info("从 " + file.getName() + " 加载菜单: " + id);
                    }
                }
            } catch (Exception ex) {
                plugin.getLogger().warning("解析菜单文件 " + file.getName() + " 失败: " + ex.getMessage());
            }
        }
    }

    private Menu parseMenu(String id, ConfigurationSection section) {
        Menu menu = new Menu();
        menu.setId(id);
        menu.setTitle(section.getString("title", "快捷菜单"));
        menu.setBedrockContent(section.getString("bedrock-content", ""));

        int size = section.getInt("size", 27);
        menu.setSize(normalizeSize(size));

        menu.setFillerMaterial(section.getString("filler.material", "BLACK_STAINED_GLASS_PANE"));
        menu.setFillerName(section.getString("filler.name", " "));
        menu.setBackMenu(section.getString("back-menu", ""));

        ConfigurationSection itemsRoot = section.getConfigurationSection("items");
        if (itemsRoot != null) {
            List<MenuItem> items = new ArrayList<>();
            for (String itemId : itemsRoot.getKeys(false)) {
                ConfigurationSection itemSection = itemsRoot.getConfigurationSection(itemId);
                if (itemSection == null) {
                    continue;
                }
                MenuItem item = parseItem(itemId, itemSection, menu.getSize());
                if (item != null) {
                    items.add(item);
                }
            }
            menu.setItems(items);
        }

        if (menu.getItems().isEmpty()) {
            plugin.getLogger().warning("菜单 " + id + " 没有任何有效菜单项");
        }
        return menu;
    }

    private MenuItem parseItem(String itemId, ConfigurationSection section, int menuSize) {
        MenuItem item = new MenuItem(itemId);

        String materialName = section.getString("material", "STONE");
        if (!isValidMaterial(materialName)) {
            plugin.getLogger().warning("菜单项 " + itemId + " 的材质无效: " + materialName + "，已回退为 STONE");
            materialName = "STONE";
        }
        item.setMaterial(materialName);

        item.setDisplayName(section.getString("display-name", itemId));
        item.setLore(section.getStringList("lore"));
        item.setGlowing(section.getBoolean("glowing", false));
        item.setPermission(section.getString("permission", ""));
        item.setButtonLabel(section.getString("button-label", ""));

        int slot = section.getInt("slot", -1);
        if (slot < 0 || slot >= menuSize) {
            plugin.getLogger().warning("菜单项 " + itemId + " 的 slot=" + slot
                    + " 超出菜单容量 " + menuSize + "，已忽略该物品槽位（基岩端按钮不受影响）");
            slot = -1;
        }
        item.setSlot(slot);

        item.setActions(parseActions(section.getStringList("actions")));
        return item;
    }

    /**
     * 解析动作字符串列表。格式：
     * <pre>
     *   无条件动作（任何客户端都执行）：
     *     "[player] home"         以玩家身份执行 /home
     *     "[console] eco give X"  以控制台身份执行
     *     "[menu] teleport"       打开 id 为 teleport 的菜单
     *     "[message] &a提示文本"  发送消息
     *     "[close]"               关闭界面
     *
     *   选择器动作（任何客户端都执行）：
     *     "[player-selector] tpa {target}"   打开在线玩家选择器，
     *                                         点击玩家后执行 /tpa 玩家名
     *     "[item-selector] worth {item}"     打开背包物品选择器，
     *                                         点击物品后执行 /worth 材质名
     *     "[ah-sell] 5000 elytra"            拍卖行上架：自动把背包里的
     *                                         该材质物品换到主手执行
     *                                         /ah sell 5000，再还原主手
     *
     *   平台条件动作（仅对应客户端执行）：
     *     "[bedrock-player] warpgui"   仅基岩玩家执行
     *     "[java-player] warp"         仅 Java 玩家执行
     *     "[bedrock-console] ..."      仅基岩玩家，控制台身份
     *     "[java-console] ..."         仅 Java 玩家，控制台身份
     * </pre>
     * 平台条件动作用于两端需要调用不同指令的场景，例如基岩端有
     * BedrockPlayerSupport 的原生表单指令、Java 端用 EssentialsX 普通指令。
     *
     * <p>未识别的前缀按玩家命令处理，兼容漏写前缀的情况。
     */
    private List<Action> parseActions(List<String> raw) {
        List<Action> actions = new ArrayList<>();
        if (raw == null) {
            return actions;
        }
        for (String line : raw) {
            if (line == null) {
                continue;
            }
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            String lower = trimmed.toLowerCase();

            // 平台条件动作前缀。这些前缀与无条件前缀互不包含
            // （"[bedrock-player]" 并不以 "[player]" 开头），因此判断顺序不影响正确性；
            // 此处仍把平台前缀列在前面，便于阅读时先看特殊规则。
            if (lower.startsWith("[player-selector]")) {
                actions.add(new Action(ActionType.PLAYER_SELECTOR,
                        strip(trimmed, "[player-selector]")));
            } else if (lower.startsWith("[item-selector]")) {
                actions.add(new Action(ActionType.ITEM_SELECTOR,
                        strip(trimmed, "[item-selector]")));
            } else if (lower.startsWith("[ah-sell]")) {
                actions.add(new Action(ActionType.AH_SELL,
                        strip(trimmed, "[ah-sell]")));
            } else if (lower.startsWith("[bedrock-player]")) {
                actions.add(new Action(ActionType.BEDROCK_PLAYER_COMMAND,
                        strip(trimmed, "[bedrock-player]")));
            } else if (lower.startsWith("[bedrock-console]")) {
                actions.add(new Action(ActionType.BEDROCK_CONSOLE_COMMAND,
                        strip(trimmed, "[bedrock-console]")));
            } else if (lower.startsWith("[java-player]")) {
                actions.add(new Action(ActionType.JAVA_PLAYER_COMMAND,
                        strip(trimmed, "[java-player]")));
            } else if (lower.startsWith("[java-console]")) {
                actions.add(new Action(ActionType.JAVA_CONSOLE_COMMAND,
                        strip(trimmed, "[java-console]")));
            } else if (lower.startsWith("[player]")) {
                actions.add(new Action(ActionType.PLAYER_COMMAND, strip(trimmed, "[player]")));
            } else if (lower.startsWith("[console]")) {
                actions.add(new Action(ActionType.CONSOLE_COMMAND, strip(trimmed, "[console]")));
            } else if (lower.startsWith("[menu]")) {
                actions.add(new Action(ActionType.OPEN_MENU, strip(trimmed, "[menu]")));
            } else if (lower.startsWith("[message]")) {
                actions.add(new Action(ActionType.MESSAGE, strip(trimmed, "[message]")));
            } else if (lower.startsWith("[close]")) {
                actions.add(new Action(ActionType.CLOSE, ""));
            } else {
                // 无前缀：当作玩家命令，去掉可能存在的斜杠
                String value = trimmed.startsWith("/") ? trimmed.substring(1) : trimmed;
                actions.add(new Action(ActionType.PLAYER_COMMAND, value.trim()));
            }
        }
        return actions;
    }

    private String strip(String input, String prefix) {
        return input.substring(prefix.length()).trim();
    }

    /** 把任意 size 规整为 9 的倍数且落在 9~54 区间 */
    private int normalizeSize(int size) {
        if (size <= 9) {
            return 9;
        }
        if (size >= 54) {
            return 54;
        }
        int rounded = ((size + 8) / 9) * 9;
        return Math.min(54, Math.max(9, rounded));
    }

    private boolean isValidMaterial(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        Material material = Material.matchMaterial(name);
        return material != null;
    }
}
