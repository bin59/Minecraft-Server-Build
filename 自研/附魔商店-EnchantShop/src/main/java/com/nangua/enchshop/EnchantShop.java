package com.nangua.enchshop;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class EnchantShop extends JavaPlugin implements Listener {

    private static final int INPUT_SLOT = 4;
    private static final int ENCHANT_START = 9;

    public static class EnchantEntry {
        public final Enchantment enchant;
        public final String id;
        public final String displayName;
        public final int cost;
        public final String supported;
        public final int maxLevel;
        EnchantEntry(Enchantment e, String id, String dn, int cost, String supported, int maxLevel) {
            this.enchant = e; this.id = id; this.displayName = dn; this.cost = cost; this.supported = supported; this.maxLevel = maxLevel;
        }
    }

    public static class ShopHolder implements InventoryHolder {
        private Inventory inv;
        @Override public Inventory getInventory() { return inv; }
        public void setInventory(Inventory i) { this.inv = i; }
    }

    private final List<EnchantEntry> enchants = new ArrayList<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadEnchants();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("附魔商店已加载，共 " + enchants.size() + " 个可购买附魔");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("只有玩家能用"); return true; }
        openShop(p);
        return true;
    }

    private void loadEnchants() {
        File eeDir = new File("plugins/ExcellentEnchants/enchants");
        enchants.clear();
        int def = getConfig().getInt("default-cost", 3);
        for (Enchantment e : Registry.ENCHANTMENT) {
            if (!e.getKey().getNamespace().equals("excellentenchants")) continue;
            String id = e.getKey().getKey();
            String dn = id;
            int cost = def;
            String supported = "any";
            int maxLevel = 1;
            File f = new File(eeDir, id + ".yml");
            if (!f.exists()) f = new File(eeDir, "_disabled_" + File.separator + id + ".yml");
            if (f.exists()) {
                YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
                dn = stripTags(y.getString("Definition.DisplayName", id));
                cost = y.getInt("AnvilCost", def);
                supported = y.getString("SupportedItems", "any").toLowerCase().trim();
                maxLevel = y.getInt("Definition.MaxLevel", 1);
            }
            enchants.add(new EnchantEntry(e, id, dn, cost, supported, maxLevel));
        }
        enchants.sort(Comparator.comparing(a -> a.id));
    }

    private String stripTags(String s) {
        return s == null ? "" : s.replaceAll("<[^>]+>", "");
    }

    public void openShop(Player p) {
        ShopHolder holder = new ShopHolder();
        Inventory inv = Bukkit.createInventory(holder, 54, ChatColor.DARK_PURPLE + "附魔商店 · 把装备放中间格子");
        holder.setInventory(inv);

        // 顶部装饰 + 输入格提示
        ItemStack hint = new ItemStack(Material.NETHER_STAR);
        ItemMeta hm = hint.getItemMeta();
        hm.setDisplayName(ChatColor.YELLOW + "把要附魔的装备放进上方中间格子");
        List<String> hl = new ArrayList<>();
        hl.add(ChatColor.GRAY + "然后点下方附魔购买");
        hm.setLore(hl);
        hint.setItemMeta(hm);
        inv.setItem(0, pane());
        inv.setItem(1, pane());
        inv.setItem(2, pane());
        inv.setItem(3, hint);
        // slot 4 = 空输入格
        inv.setItem(5, hint);
        inv.setItem(6, pane());
        inv.setItem(7, pane());
        inv.setItem(8, pane());

        // 附魔列表
        int slot = ENCHANT_START;
        for (EnchantEntry en : enchants) {
            if (slot > 44) break;
            ItemStack it = new ItemStack(Material.ENCHANTED_BOOK);
            ItemMeta m = it.getItemMeta();
            m.setDisplayName(ChatColor.AQUA + en.displayName);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "类型: " + en.supported + " · 满级 " + en.maxLevel);
            lore.add(ChatColor.GREEN + "每升一级: " + en.cost + " 级经验");
            lore.add(ChatColor.YELLOW + "点击升级");
            m.setLore(lore);
            it.setItemMeta(m);
            inv.setItem(slot++, it);
        }
        // 底部填充
        for (int i = 45; i < 54; i++) inv.setItem(i, pane());
        p.openInventory(inv);
    }

    private ItemStack pane() {
        ItemStack p = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta pm = p.getItemMeta();
        pm.setDisplayName(" ");
        p.setItemMeta(pm);
        return p;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof ShopHolder)) return;
        if (!(e.getWhoClicked() instanceof Player p)) return;
        int slot = e.getRawSlot();
        int topSize = e.getInventory().getSize();

        // 输入格：允许玩家放/取物品
        if (slot == INPUT_SLOT) return;
        // 玩家自己背包（底部）：放行普通拿取，只禁止 shift-click 塞进 GUI
        if (slot >= topSize) {
            if (e.isShiftClick()) e.setCancelled(true);
            return;
        }
        // 顶部 GUI 其它格子一律取消
        e.setCancelled(true);
        if (e.getClick().isShiftClick()) return;

        int idx = slot - ENCHANT_START;
        if (idx < 0 || idx >= enchants.size()) return;
        EnchantEntry en = enchants.get(idx);

        ItemStack input = e.getInventory().getItem(INPUT_SLOT);
        if (input == null || input.getType() == Material.AIR) {
            p.sendMessage(ChatColor.RED + "请先把要附魔的装备放进上方中间格子");
            return;
        }
        if (!isSupported(input.getType(), en.supported)) {
            p.sendMessage(ChatColor.RED + "「" + en.displayName + "」不能用在 " + input.getType().name() + " 上");
            return;
        }
        if (p.getLevel() < en.cost) {
            p.sendMessage(ChatColor.RED + "经验等级不够！需要 " + en.cost + " 级，你只有 " + p.getLevel() + " 级");
            return;
        }
        ItemMeta meta = input.getItemMeta();
        int cur = (meta != null && meta.hasEnchant(en.enchant)) ? meta.getEnchantLevel(en.enchant) : 0;
        if (cur >= en.maxLevel) {
            p.sendMessage(ChatColor.YELLOW + "这件装备的 " + en.displayName + " 已满级（" + en.maxLevel + " 级）");
            return;
        }
        p.giveExpLevels(-en.cost);
        if (meta != null) {
            meta.addEnchant(en.enchant, cur + 1, true);
            input.setItemMeta(meta);
        }
        e.getInventory().setItem(INPUT_SLOT, input);
        p.sendMessage(ChatColor.GREEN + "已花费 " + en.cost + " 级经验，" + en.displayName + (cur + 1) + " 级（" + input.getType().name() + "）");
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getInventory().getHolder() instanceof ShopHolder)) return;
        ItemStack input = e.getInventory().getItem(INPUT_SLOT);
        if (input != null && input.getType() != Material.AIR && e.getPlayer() instanceof Player p) {
            p.getInventory().addItem(input).values().forEach(it -> p.getWorld().dropItemNaturally(p.getLocation(), it));
            e.getInventory().setItem(INPUT_SLOT, null);
        }
    }

    private boolean isSupported(Material m, String cat) {
        String n = m.name().toLowerCase();
        switch (cat) {
            case "helmet": return n.endsWith("_helmet");
            case "chestplate_elytra": case "chestplate": return n.endsWith("_chestplate") || n.equals("elytra");
            case "leggings": return n.endsWith("_leggings");
            case "boots": return n.endsWith("_boots");
            case "armor": return n.endsWith("_helmet")||n.endsWith("_chestplate")||n.endsWith("_leggings")||n.endsWith("_boots");
            case "swords_axes": return n.endsWith("_sword")||n.endsWith("_axe");
            case "sword": return n.endsWith("_sword");
            case "axe": return n.endsWith("_axe");
            case "pickaxe": return n.endsWith("_pickaxe");
            case "shovel": return n.endsWith("_shovel");
            case "hoe": return n.endsWith("_hoe");
            case "tool": case "tools_weapons": case "mining_tools":
                return n.endsWith("_pickaxe")||n.endsWith("_axe")||n.endsWith("_shovel")||n.endsWith("_hoe");
            case "bow": return n.equals("bow");
            case "bow_crossbow": return n.equals("bow")||n.equals("crossbow");
            case "fishing_rod": return n.equals("fishing_rod");
            case "trident": return n.equals("trident");
            case "breakable": case "any": default: return true;
        }
    }
}