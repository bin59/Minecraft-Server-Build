package com.nangua.quickmenu.gui;

import com.nangua.quickmenu.QuickMenuPlugin;
import com.nangua.quickmenu.menu.ActionExecutor;
import com.nangua.quickmenu.menu.Menu;
import com.nangua.quickmenu.menu.MenuItem;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.util.FormImage;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 基岩端原生表单的统一外观。
 *
 * <p>集中处理四件事，让所有表单（主菜单 / 说明页 / 三个选择器 / 输入表单）
 * 看起来是同一套 UI：
 * <ol>
 *   <li><b>按钮图标</b> — 通过 Cumulus 的 {@code FormImage}，按菜单项的
 *       Java 材质推导基岩客户端贴图路径；可在 {@code bedrock-form.icon-paths}
 *       里逐个覆盖。路径不存在时会显示空白图标，因此提供 {@code icons: false}
 *       一键回退纯文字按钮。</li>
 *   <li><b>两行按钮</b> — 第一行按钮名，第二行取 lore 首行作说明。
 *       原生按钮不支持 &amp; / § 颜色码，故全部去色，只用纯文本排版。</li>
 *   <li><b>标题与正文</b> — 统一前缀、分隔线、按钮数量提示。</li>
 *   <li><b>返回 / 关闭 / 空态</b> — 统一文案与图标。</li>
 * </ol>
 *
 * <p>配置每次渲染前调用 {@link #reload()} 重新读取内存中的 Bukkit 配置，
 * 因此 {@code /qm reload} 后外观改动也能立即生效（无需重启）。
 */
public final class BedrockFormStyle {

    private final QuickMenuPlugin plugin;

    private boolean icons = true;
    private boolean autoPath = true;
    private boolean twoLine = true;
    private String titlePrefix = "✦ ";
    private String separator = "§8────────────────";
    private String hint = "§7点按按钮执行 · 共 %d 项";
    private String infoTitlePrefix = "ℹ ";
    private String infoFooter = "§7看完点下方按钮返回菜单";
    private String backLabel = "« 返回菜单";
    private String closeLabel = "✕ 关闭";
    private String submitLabel = "§8填好后点右下角提交";
    private String emptyLabel = "（暂无可选项）";

    private final Map<String, String> iconPaths = new HashMap<>();

    /**
     * 基岩客户端贴图路径与 Java 材质名不一致时的别名表。
     * 只列常见的、跨版本比较稳定的一批；其余由 {@code auto-path} 推导。
     * 服主可随时在 config.yml 的 {@code bedrock-form.icon-paths} 里覆盖或补充。
     */
    private static final Map<String, String> DEFAULT_ICONS = new LinkedHashMap<>();

    static {
        DEFAULT_ICONS.put("BOOK", "textures/items/book_normal");
        DEFAULT_ICONS.put("WRITABLE_BOOK", "textures/items/book_writable");
        DEFAULT_ICONS.put("WRITTEN_BOOK", "textures/items/book_written");
        DEFAULT_ICONS.put("ENCHANTED_BOOK", "textures/items/book_enchanted");
        DEFAULT_ICONS.put("KNOWLEDGE_BOOK", "textures/items/book_normal");
        DEFAULT_ICONS.put("CLOCK", "textures/items/clock_item");
        DEFAULT_ICONS.put("COMPASS", "textures/items/compass_item");
        DEFAULT_ICONS.put("RECOVERY_COMPASS", "textures/items/compass_item");
        DEFAULT_ICONS.put("SPYGLASS", "textures/items/spyglass");
        DEFAULT_ICONS.put("PLAYER_HEAD", "textures/items/head_steve");
        DEFAULT_ICONS.put("BARRIER", "textures/items/barrier");
        DEFAULT_ICONS.put("FILLED_MAP", "textures/items/map_filled");
        DEFAULT_ICONS.put("MAP", "textures/items/map_empty");
        DEFAULT_ICONS.put("ENCHANTING_TABLE", "textures/blocks/enchanting_table_top");
        DEFAULT_ICONS.put("GRASS_BLOCK", "textures/blocks/grass_side_carried");
        DEFAULT_ICONS.put("END_PORTAL_FRAME", "textures/blocks/end_portal_frame_top");
        DEFAULT_ICONS.put("REDSTONE", "textures/items/redstone_dust");
        DEFAULT_ICONS.put("REPEATER", "textures/items/repeater");
        DEFAULT_ICONS.put("COMPARATOR", "textures/items/comparator");
        DEFAULT_ICONS.put("WHEAT", "textures/items/wheat");
        DEFAULT_ICONS.put("PUMPKIN", "textures/blocks/pumpkin_face_off");
        DEFAULT_ICONS.put("OAK_SIGN", "textures/items/oak_sign");
        DEFAULT_ICONS.put("OAK_DOOR", "textures/items/oak_door");
        DEFAULT_ICONS.put("IRON_DOOR", "textures/items/iron_door");
        DEFAULT_ICONS.put("ELYTRA", "textures/items/elytra");
        DEFAULT_ICONS.put("TRIDENT", "textures/items/trident");
        DEFAULT_ICONS.put("SHIELD", "textures/items/shield");
        DEFAULT_ICONS.put("NETHER_STAR", "textures/items/nether_star");
        DEFAULT_ICONS.put("ENDER_EYE", "textures/items/ender_eye");
        DEFAULT_ICONS.put("ENDER_PEARL", "textures/items/ender_pearl");
        DEFAULT_ICONS.put("END_CRYSTAL", "textures/items/end_crystal");
        DEFAULT_ICONS.put("GOLDEN_APPLE", "textures/items/apple_golden");
        DEFAULT_ICONS.put("GOLDEN_CARROT", "textures/items/carrot_golden");
        DEFAULT_ICONS.put("MILK_BUCKET", "textures/items/bucket_milk");
        DEFAULT_ICONS.put("WATER_BUCKET", "textures/items/bucket_water");
        DEFAULT_ICONS.put("POTION", "textures/items/potion_bottle_drinkable");
        DEFAULT_ICONS.put("WOODEN_AXE", "textures/items/wood_axe");
        DEFAULT_ICONS.put("STONE_PICKAXE", "textures/items/stone_pickaxe");
        DEFAULT_ICONS.put("IRON_AXE", "textures/items/iron_axe");
        DEFAULT_ICONS.put("DIAMOND_SWORD", "textures/items/diamond_sword");
        DEFAULT_ICONS.put("DIAMOND_PICKAXE", "textures/items/diamond_pickaxe");
        DEFAULT_ICONS.put("NAME_TAG", "textures/items/name_tag");
        DEFAULT_ICONS.put("SADDLE", "textures/items/saddle");
        DEFAULT_ICONS.put("SHEARS", "textures/items/shears");
        DEFAULT_ICONS.put("FISHING_ROD", "textures/items/fishing_rod");
        DEFAULT_ICONS.put("COAL", "textures/items/coal");
        DEFAULT_ICONS.put("BRICK", "textures/items/brick");
        DEFAULT_ICONS.put("STRING", "textures/items/string");
        DEFAULT_ICONS.put("FEATHER", "textures/items/feather");
        DEFAULT_ICONS.put("BONE", "textures/items/bone");
        DEFAULT_ICONS.put("ARROW", "textures/items/arrow");
        DEFAULT_ICONS.put("SPECTRAL_ARROW", "textures/items/arrow_spectral");
        DEFAULT_ICONS.put("FIREWORK_ROCKET", "textures/items/fireworks");
        DEFAULT_ICONS.put("GOAT_HORN", "textures/items/goat_horn");
        DEFAULT_ICONS.put("LEATHER_BOOTS", "textures/items/leather_boots");
        DEFAULT_ICONS.put("COOKED_BEEF", "textures/items/beef_cooked");
        DEFAULT_ICONS.put("GOLD_NUGGET", "textures/items/gold_nugget");
        DEFAULT_ICONS.put("GOLD_INGOT", "textures/items/gold_ingot");
        DEFAULT_ICONS.put("NETHERITE_INGOT", "textures/items/netherite_ingot");
        DEFAULT_ICONS.put("EMERALD", "textures/items/emerald");
        DEFAULT_ICONS.put("DIAMOND", "textures/items/diamond");
        DEFAULT_ICONS.put("PAPER", "textures/items/paper");
        DEFAULT_ICONS.put("BOOKSHELF", "textures/blocks/bookshelf");
        DEFAULT_ICONS.put("CHEST", "textures/blocks/chest_front");
        DEFAULT_ICONS.put("CHEST_MINECART", "textures/items/minecart_chest");
        DEFAULT_ICONS.put("FURNACE", "textures/blocks/furnace_front_off");
        DEFAULT_ICONS.put("BEACON", "textures/blocks/beacon");
        DEFAULT_ICONS.put("BELL", "textures/blocks/bell");
        DEFAULT_ICONS.put("NOTE_BLOCK", "textures/blocks/noteblock");
        DEFAULT_ICONS.put("TNT", "textures/blocks/tnt_side");
        DEFAULT_ICONS.put("TORCH", "textures/blocks/torch_on");
        DEFAULT_ICONS.put("SUNFLOWER", "textures/items/sunflower");
        DEFAULT_ICONS.put("PAINTING", "textures/items/painting");
        DEFAULT_ICONS.put("ITEM_FRAME", "textures/items/item_frame");
        DEFAULT_ICONS.put("FLOWER_POT", "textures/items/flower_pot");
        DEFAULT_ICONS.put("COMMAND_BLOCK", "textures/blocks/command_block");
        DEFAULT_ICONS.put("HOPPER", "textures/blocks/hopper_top");
        DEFAULT_ICONS.put("ANVIL", "textures/blocks/anvil_top_damaged_0");
        DEFAULT_ICONS.put("OBSERVER", "textures/blocks/observer_back");
        DEFAULT_ICONS.put("REDSTONE_LAMP", "textures/blocks/redstone_lamp_off");
        DEFAULT_ICONS.put("END_STONE", "textures/blocks/end_stone");
        DEFAULT_ICONS.put("PURPUR_BLOCK", "textures/blocks/purpur_block");
        DEFAULT_ICONS.put("IRON_BARS", "textures/blocks/iron_bars");
        DEFAULT_ICONS.put("LADDER", "textures/blocks/ladder");
        DEFAULT_ICONS.put("LEVER", "textures/blocks/lever");
        DEFAULT_ICONS.put("JUKEBOX", "textures/blocks/jukebox_top");
        DEFAULT_ICONS.put("OAK_LOG", "textures/blocks/log_oak");
        DEFAULT_ICONS.put("OAK_FENCE", "textures/blocks/planks_oak");
        DEFAULT_ICONS.put("OAK_SAPLING", "textures/blocks/sapling_oak");
        DEFAULT_ICONS.put("IRON_BLOCK", "textures/blocks/iron_block");
        DEFAULT_ICONS.put("GOLD_BLOCK", "textures/blocks/gold_block");
        DEFAULT_ICONS.put("DIAMOND_BLOCK", "textures/blocks/diamond_block");
        DEFAULT_ICONS.put("EMERALD_BLOCK", "textures/blocks/emerald_block");
        DEFAULT_ICONS.put("REDSTONE_BLOCK", "textures/blocks/redstone_block");
        DEFAULT_ICONS.put("BRICKS", "textures/blocks/brick");
    }

    public BedrockFormStyle(QuickMenuPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    /** 从内存中的 config.yml 重新读取外观设置（/qm reload 后无需重启）。 */
    public void reload() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("bedrock-form");
        if (section == null) {
            return;
        }
        icons = section.getBoolean("icons", true);
        autoPath = section.getBoolean("auto-path", true);
        twoLine = section.getBoolean("two-line-buttons", true);
        titlePrefix = section.getString("title-prefix", "✦ ");
        separator = ActionExecutor.color(section.getString("separator", "§8────────────────"));
        hint = ActionExecutor.color(section.getString("hint", "§7点按按钮执行 · 共 %d 项"));
        infoTitlePrefix = section.getString("info-title-prefix", "ℹ ");
        infoFooter = ActionExecutor.color(section.getString("info-footer", "§7看完点下方按钮返回菜单"));
        backLabel = section.getString("back-label", "« 返回菜单");
        closeLabel = section.getString("close-label", "✕ 关闭");
        submitLabel = ActionExecutor.color(section.getString("submit-label", "§8填好后点右下角提交"));
        emptyLabel = section.getString("empty-label", "（暂无可选项）");

        iconPaths.clear();
        iconPaths.putAll(DEFAULT_ICONS);
        ConfigurationSection paths = section.getConfigurationSection("icon-paths");
        if (paths != null) {
            for (String key : paths.getKeys(false)) {
                String value = paths.getString(key, "");
                if (value != null && !value.isEmpty()) {
                    iconPaths.put(key.toUpperCase(Locale.ROOT), value);
                } else {
                    // 配置里写空值 = 明确不要这个按钮的图标
                    iconPaths.remove(key.toUpperCase(Locale.ROOT));
                }
            }
        }
    }

    /** 表单标题：统一前缀 + 原菜单标题（支持 & 颜色码）。 */
    public String title(String rawTitle) {
        return ActionExecutor.color(titlePrefix + (rawTitle == null ? "" : rawTitle));
    }

    /** 说明页标题。 */
    public String infoTitle(String rawTitle) {
        return ActionExecutor.color(infoTitlePrefix + (rawTitle == null ? "" : rawTitle));
    }

    /** 菜单正文：分隔线 + 原说明 + 按钮数量提示。 */
    public String content(String base, int buttonCount) {
        String body = (base == null || base.isEmpty()) ? "" : ActionExecutor.color(base);
        return separator + "\n" + body + "\n" + separator + "\n"
                + String.format(hint, buttonCount);
    }

    /** 说明页正文：把原来的多行 [message] 整理成一段带分隔线的说明。 */
    public String infoContent(List<String> messages) {
        StringBuilder sb = new StringBuilder(separator).append('\n');
        for (String line : messages) {
            sb.append(ActionExecutor.color(line)).append('\n');
        }
        sb.append(separator).append('\n').append(infoFooter);
        return sb.toString();
    }

    /** 输入表单的说明标签（CustomForm 没有 content 字段，用 label 组件呈现）。 */
    public String submitLabel() {
        return submitLabel;
    }

    public String backLabel() {
        return backLabel;
    }

    public String closeLabel() {
        return closeLabel;
    }

    public String emptyLabel() {
        return emptyLabel;
    }

    /**
     * 按钮文本：第一行名称，第二行取 lore 首行（去色、限长）。
     * 原生 Form 按钮不支持颜色码，一行与两行都只用纯文本。
     */
    public String buttonText(MenuItem item) {
        String label = item.getButtonLabel();
        if (label == null || label.isEmpty()) {
            label = ActionExecutor.stripColor(item.getDisplayName());
        } else {
            label = ActionExecutor.stripColor(label);
        }
        if (!twoLine || item.getLore() == null || item.getLore().isEmpty()) {
            return label;
        }
        String sub = ActionExecutor.stripColor(item.getLore().get(0)).trim();
        if (sub.isEmpty()) {
            return label;
        }
        if (sub.length() > 30) {
            sub = sub.substring(0, 29) + "…";
        }
        return label + "\n" + sub;
    }

    /** 按 Java 材质名解析按钮图标；返回 null 表示这个按钮不加图标。 */
    public FormImage imageOf(String material) {
        if (!icons || material == null || material.isEmpty()) {
            return null;
        }
        String key = material.toUpperCase(Locale.ROOT);
        String path = iconPaths.get(key);
        if (path == null && autoPath) {
            Material bukkitMaterial = Material.matchMaterial(key);
            String lower = key.toLowerCase(Locale.ROOT);
            path = (bukkitMaterial != null && bukkitMaterial.isBlock())
                    ? "textures/blocks/" + lower
                    : "textures/items/" + lower;
        }
        return path == null || path.isEmpty()
                ? null
                : FormImage.of(FormImage.Type.PATH, path);
    }

    /** 显式贴图路径（返回 / 关闭 / 空态等固定按钮用）。 */
    public FormImage imageOfPath(String path) {
        if (!icons || path == null || path.isEmpty()) {
            return null;
        }
        return FormImage.of(FormImage.Type.PATH, path);
    }

    /** 添加一个按钮，自动决定带不带图标。 */
    public void button(SimpleForm.Builder builder, String text, FormImage image) {
        if (icons && image != null) {
            builder.button(text, image);
        } else {
            builder.button(text);
        }
    }

    /** 添加一个菜单项按钮（图标取自材质，文本为两行）。 */
    public void itemButton(SimpleForm.Builder builder, MenuItem item) {
        button(builder, buttonText(item), imageOf(item.getMaterial()));
    }

    /** 固定用途按钮：返回 / 关闭 / 空态。 */
    public void backButton(SimpleForm.Builder builder) {
        button(builder, backLabel, imageOf("ARROW"));
    }

    public void closeButton(SimpleForm.Builder builder) {
        button(builder, closeLabel, imageOf("BARRIER"));
    }

    public void emptyButton(SimpleForm.Builder builder, String text) {
        String label = (text == null || text.isEmpty()) ? emptyLabel : ActionExecutor.stripColor(text);
        button(builder, label, imageOf("PAPER"));
    }

    /** 菜单正文的兜底：未配 bedrock-content 时用菜单标题。 */
    public String contentFor(Menu menu, int buttonCount) {
        String base = menu.getBedrockContent();
        if (base == null || base.isEmpty()) {
            base = menu.getTitle();
        }
        return content(base, buttonCount);
    }
}
