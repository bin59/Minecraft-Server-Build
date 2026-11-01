import com.nangua.pumpkinmail.RewardApplier;

public class T {
    public static void main(String[] a) {
        String[] cases = {
            "DIAMOND:64", "diamond:64", "minecraft:diamond:64", "minecraft:diamond",
            "DIAMOND", "golden_apple:2", "enchanted_book", "netherite_scrap:5",
            "minecraft:enchanted_book:3", "DIAMOND_BLOCK", "minecraft:netherite_ingot",
            "bogus_item", "", null, "DIAMOND:999", "DIAMOND:0"
        };
        for (String c : cases) {
            String[] r;
            try { r = RewardApplier.splitMaterialAndAmount(c); }
            catch (Throwable t) { System.out.println(fmt(c) + "  ->  抛异常 " + t); continue; }
            System.out.println(fmt(c) + "  ->  " + (r == null ? "null(无效)" : "材质=" + r[0] + "  数量=" + r[1]));
        }
    }
    static String fmt(String s) { return s == null ? "null" : "\"" + s + "\""; }
}
