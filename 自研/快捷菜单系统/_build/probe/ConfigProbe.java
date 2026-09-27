import org.bukkit.Material;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 配置文件校验探针。
 *
 * <p>用 snakeyaml 真实解析 config.yml，并按 ConfigLoader 的同款逻辑校验：
 * <ol>
 *   <li>所有 material 名称是否是有效的 Bukkit Material</li>
 *   <li>每个菜单的 size 是否为 9 的倍数且在 9~54 区间</li>
 *   <li>每个菜单项的 slot 是否落在所属菜单的 size 范围内（越界会导致 Java 端不显示）</li>
 *   <li>同一菜单内是否有 slot 冲突（后放置的物品会覆盖前一个，导致功能丢失）</li>
 *   <li>actions 前缀是否都是已支持的类型</li>
 *   <li>back-menu 引用的菜单 id 是否真实存在（否则返回按钮点了没反应）</li>
 *   <li>default-menu 是否存在</li>
 * </ol>
 * 仅用于本地构建验证，不随插件打包。
 */
public class ConfigProbe {

    private static final List<String> KNOWN_PREFIXES = Arrays.asList(
            "[player]", "[console]", "[menu]", "[message]", "[close]",
            // 选择器动作：打开动态列表界面
            "[player-selector]", "[item-selector]", "[ah-sell]",
            // 平台条件动作：仅对应客户端执行
            "[bedrock-player]", "[bedrock-console]",
            "[java-player]", "[java-console]");

    private static final List<String> ERRORS = new ArrayList<>();
    private static final List<String> WARNINGS = new ArrayList<>();

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        String configPath = args[0];
        File file = new File(configPath);
        if (!file.exists()) {
            System.out.println("FATAL: 配置文件不存在: " + configPath);
            System.exit(2);
            return;
        }

        Yaml yaml = new Yaml();
        Map<String, Object> root;
        try (InputStream in = new FileInputStream(file)) {
            root = yaml.load(in);
        }
        System.out.println("已解析: " + file.getName() + " (UTF-8, "
                + Files.size(file.toPath()) + " bytes)");

        if (root == null) {
            System.out.println("FATAL: 配置文件为空或格式错误");
            System.exit(2);
            return;
        }

        Object menusObj = root.get("menus");
        if (!(menusObj instanceof Map)) {
            System.out.println("FATAL: 缺少 menus 节点");
            System.exit(2);
            return;
        }
        Map<String, Object> menus = (Map<String, Object>) menusObj;
        Set<String> menuIds = menus.keySet();
        System.out.println("发现菜单数: " + menuIds.size() + " -> " + menuIds);

        // default-menu 校验
        Object defaultMenu = root.get("default-menu");
        if (defaultMenu == null) {
            WARNINGS.add("未配置 default-menu，将回退为 main");
        } else if (!menuIds.contains(String.valueOf(defaultMenu))) {
            ERRORS.add("default-menu='" + defaultMenu + "' 指向的菜单不存在");
        }

        int totalItems = 0;
        for (String menuId : menuIds) {
            Object menuObj = menus.get(menuId);
            if (!(menuObj instanceof Map)) {
                ERRORS.add("菜单 " + menuId + " 不是有效的映射结构");
                continue;
            }
            Map<String, Object> menu = (Map<String, Object>) menuObj;

            // size 校验
            int rawSize = asInt(menu.get("size"), 27);
            int normalized = normalizeSize(rawSize);
            if (rawSize != normalized) {
                WARNINGS.add("菜单 " + menuId + " 的 size=" + rawSize
                        + " 会被规整为 " + normalized);
            }
            if (rawSize % 9 != 0 || rawSize < 9 || rawSize > 54) {
                WARNINGS.add("菜单 " + menuId + " 的 size=" + rawSize
                        + " 不是 9 的合法倍数（9~54）");
            }

            // back-menu 引用校验
            Object backMenu = menu.get("back-menu");
            if (backMenu != null && !String.valueOf(backMenu).isEmpty()) {
                if (!menuIds.contains(String.valueOf(backMenu))) {
                    ERRORS.add("菜单 " + menuId + " 的 back-menu='" + backMenu
                            + "' 指向不存在的菜单，返回按钮将无效");
                }
                // 返回按钮固定在右下角 size-1，检查是否被其他物品占用
                int backSlot = normalized - 1;
                if (backSlot >= 0 && backSlot < normalized && menu.get("items") instanceof Map) {
                    for (Map.Entry<String, Object> e
                            : ((Map<String, Object>) menu.get("items")).entrySet()) {
                        if (e.getValue() instanceof Map
                                && asInt(((Map<String, Object>) e.getValue()).get("slot"), -1) == backSlot) {
                            WARNINGS.add("菜单 " + menuId + " 的返回按钮槽位 " + backSlot
                                    + " 与物品 " + e.getKey() + " 冲突");
                        }
                    }
                }
            }

            // items 校验
            Object itemsObj = menu.get("items");
            if (!(itemsObj instanceof Map)) {
                ERRORS.add("菜单 " + menuId + " 没有 items 节点");
                continue;
            }
            Map<String, Object> items = (Map<String, Object>) itemsObj;
            if (items.isEmpty()) {
                ERRORS.add("菜单 " + menuId + " 的 items 为空");
                continue;
            }

            Map<Integer, String> slotMap = new java.util.HashMap<>();
            for (String itemId : items.keySet()) {
                totalItems++;
                Object itemObj = items.get(itemId);
                if (!(itemObj instanceof Map)) {
                    ERRORS.add("菜单 " + menuId + " 的物品 " + itemId + " 不是有效映射");
                    continue;
                }
                Map<String, Object> item = (Map<String, Object>) itemObj;
                String tag = menuId + "." + itemId;

                // material 校验
                String material = String.valueOf(item.getOrDefault("material", "STONE"));
                if (Material.matchMaterial(material) == null) {
                    ERRORS.add(tag + " 的 material='" + material + "' 不是有效的 Bukkit 材质名");
                }

                // slot 校验
                int slot = asInt(item.get("slot"), -1);
                if (slot < 0) {
                    WARNINGS.add(tag + " 未配置 slot，Java 箱子界面不会显示该物品");
                } else if (slot >= normalized) {
                    ERRORS.add(tag + " 的 slot=" + slot + " 超出菜单容量 " + normalized
                            + "，Java 端该物品将不显示");
                } else {
                    String prev = slotMap.get(slot);
                    if (prev != null) {
                        ERRORS.add(tag + " 与 " + prev + " 槽位冲突（slot=" + slot + "）");
                    } else {
                        slotMap.put(slot, tag);
                    }
                }

                // filler material 校验（菜单级）
                if (!tag.endsWith("_filler_checked")) {
                    Object fillerObj = menu.get("filler");
                    if (fillerObj instanceof Map) {
                        String fm = String.valueOf(((Map<String, Object>) fillerObj)
                                .getOrDefault("material", "NONE"));
                        if (!fm.equalsIgnoreCase("NONE") && Material.matchMaterial(fm) == null) {
                            ERRORS.add("菜单 " + menuId + " 的 filler.material='"
                                    + fm + "' 无效");
                        }
                    }
                }

                // actions 校验
                Object actionsObj = item.get("actions");
                if (actionsObj instanceof List) {
                    // 平台覆盖追踪：判断某一端点击后是否「完全没有任何有效反馈」。
                    //
                    // 覆盖判定规则（刻意区分 close 与 message）：
                    //   [player] [console] 无前缀  → 两端都算覆盖
                    //   [message] [menu]           → 两端都算覆盖（提供实际反馈/指引）
                    //   [close]                    → 不算覆盖（仅界面管理，无功能价值）
                    //   [bedrock-*]                → 仅算基岩覆盖
                    //   [java-*]                   → 仅算 Java 覆盖
                    //
                    // 这样能精确区分两种情形：
                    //   "[close] + [bedrock-player] warpgui"
                    //     → Java 端只关界面、无功能 → 应告警（真实缺陷）
                    //   "[close] + [bedrock-player] tpgui + [message] 请用 /tpa"
                    //     → Java 端收到指引文字 → 不告警（有意的降级设计）
                    boolean hasAnyAction = false;
                    boolean coversBedrock = false;
                    boolean coversJava = false;

                    for (Object a : (List<?>) actionsObj) {
                        String line = String.valueOf(a).trim();
                        if (line.isEmpty()) {
                            continue;
                        }
                        String lower = line.toLowerCase();
                        boolean known = false;
                        for (String prefix : KNOWN_PREFIXES) {
                            if (lower.startsWith(prefix)) {
                                known = true;
                                break;
                            }
                        }
                        if (!known) {
                            WARNINGS.add(tag + " 的动作 '" + line
                                    + "' 无已知前缀，将被当作玩家命令执行");
                            // 无前缀按玩家命令处理，两端通用
                            hasAnyAction = true;
                            coversBedrock = true;
                            coversJava = true;
                        } else {
                            // 任何已知动作都算「有动作」，据此才会执行覆盖检查。
                            // 注意：[close] 走到这里只置 hasAnyAction，不置任何 covers 标记，
                            // 因此「只有 [close]」会被判定为两端均无有效反馈而告警。
                            // 若把 hasAnyAction 放到下面的分支里，[close] 会因不匹配任何分支
                            // 而使其保持 false，导致整项检查被跳过（漏报）。
                            hasAnyAction = true;
                            if (lower.startsWith("[bedrock-")) {
                                coversBedrock = true;
                            } else if (lower.startsWith("[java-")) {
                                coversJava = true;
                            } else if (lower.startsWith("[player]") || lower.startsWith("[console]")
                                    || lower.startsWith("[message]") || lower.startsWith("[menu]")
                                    || lower.startsWith("[player-selector]")
                                    || lower.startsWith("[item-selector]")
                                    || lower.startsWith("[ah-sell]")) {
                                // 提供实际功能或反馈，两端通用
                                coversBedrock = true;
                                coversJava = true;
                            }
                            // [close] 刻意不计入覆盖
                        }

                        // [menu] 动作的目标校验
                        if (lower.startsWith("[menu]")) {
                            String target = line.substring("[menu]".length()).trim();
                            if (!menuIds.contains(target)) {
                                ERRORS.add(tag + " 的 [menu] 目标 '" + target + "' 不存在");
                            }
                        }
                    }

                    // 覆盖缺口检测：某端点击后完全没有有效反馈时告警
                    if (hasAnyAction) {
                        if (!coversBedrock) {
                            WARNINGS.add(tag + " 的动作对基岩玩家无任何有效反馈"
                                    + "（仅 [close] 或只配了 [java-*]）");
                        }
                        if (!coversJava) {
                            WARNINGS.add(tag + " 的动作对 Java 玩家无任何有效反馈"
                                    + "（仅 [close] 或只配了 [bedrock-*]）");
                        }
                    }
                } else {
                    WARNINGS.add(tag + " 没有 actions，点击后无任何效果");
                }

                // 基岩端按钮文本颜色码检查
                Object buttonLabel = item.get("button-label");
                if (buttonLabel != null && String.valueOf(buttonLabel).contains("&")) {
                    WARNINGS.add(tag + " 的 button-label 含颜色码 &，"
                            + "原生表单按钮不支持颜色码，插件会自动去除");
                }
            }
        }

        // trigger-item material 校验
        Object triggerObj = root.get("trigger-item");
        if (triggerObj instanceof Map) {
            String tm = String.valueOf(((Map<String, Object>) triggerObj)
                    .getOrDefault("material", "COMPASS"));
            if (Material.matchMaterial(tm) == null) {
                ERRORS.add("trigger-item.material='" + tm + "' 不是有效的 Bukkit 材质名");
            } else {
                System.out.println("触发物品材质 OK: " + tm);
            }
        }

        System.out.println("\n菜单项总数: " + totalItems);
        System.out.println("=== 校验结果 ===");
        System.out.println("ERROR   : " + ERRORS.size());
        for (String e : ERRORS) {
            System.out.println("  [X] " + e);
        }
        System.out.println("WARNING : " + WARNINGS.size());
        for (String w : WARNINGS) {
            System.out.println("  [!] " + w);
        }

        if (!ERRORS.isEmpty()) {
            System.exit(1);
        }
    }

    private static int asInt(Object value, int fallback) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    /** 与 ConfigLoader.normalizeSize 保持一致 */
    private static int normalizeSize(int size) {
        if (size <= 9) {
            return 9;
        }
        if (size >= 54) {
            return 54;
        }
        int rounded = ((size + 8) / 9) * 9;
        return Math.min(54, Math.max(9, rounded));
    }
}
