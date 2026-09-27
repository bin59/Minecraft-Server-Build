import org.bukkit.Material;

/**
 * 材质名校验探针：检查 config.yml 中用到的所有 Material 名称是否有效。
 * 仅用于本地构建验证，不随插件打包。
 */
public class MatProbe {

    public static void main(String[] args) {
        String[] names = {
                // 触发物品与填充物
                "COMPASS", "NETHER_STAR", "CLOCK", "BOOK", "NAME_TAG", "ARROW", "STONE",
                "BLACK_STAINED_GLASS_PANE", "BLUE_STAINED_GLASS_PANE",
                "GREEN_STAINED_GLASS_PANE", "YELLOW_STAINED_GLASS_PANE",
                "ORANGE_STAINED_GLASS_PANE", "MAGENTA_STAINED_GLASS_PANE",
                "LIGHT_BLUE_STAINED_GLASS_PANE",
                // 主菜单
                "ENDER_PEARL", "RED_BED", "CHEST", "GOLD_INGOT",
                // 传送
                "ENDER_EYE", "BEACON", "GRASS_BLOCK",
                // 家园
                "PAPER", "OAK_DOOR",
                // 工具包
                "IRON_SWORD", "GOLDEN_APPLE", "DIAMOND_SWORD",
                // 经济
                "GOLD_NUGGET", "NETHERITE_INGOT", "EMERALD",
                // 设置与信息
                "WRITABLE_BOOK", "PLAYER_HEAD", "PAINTING", "SIGN", "SUNFLOWER"
        };

        int missing = 0;
        for (String name : names) {
            if (Material.matchMaterial(name) == null) {
                System.out.println("MISSING: " + name);
                missing++;
            }
        }
        System.out.println("checked=" + names.length + " missing=" + missing);
        if (missing > 0) {
            System.exit(1);
        }
    }
}
