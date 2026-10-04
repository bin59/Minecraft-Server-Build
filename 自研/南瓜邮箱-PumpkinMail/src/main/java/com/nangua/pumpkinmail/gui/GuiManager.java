package com.nangua.pumpkinmail.gui;

import com.nangua.pumpkinmail.PumpkinMail;
import com.nangua.pumpkinmail.RewardApplier;
import com.nangua.pumpkinmail.storage.Preset;
import com.nangua.pumpkinmail.storage.Reward;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * GUI 管理：玩家邮箱（/mailbox）+ 管理端批量发放（/mailbox admin）。
 * Java 与基岩玩家均走箱子 GUI（Geyser 原生渲染 Bukkit 箱子界面）。
 */
public class GuiManager implements Listener {

    private static final int PER_PAGE = 45;          // 每页奖励格数（0-44）
    private static final int NAV_BACK = 48;          // 上一页
    private static final int NAV_CLOSE = 49;         // 关闭
    private static final int NAV_NEXT = 50;          // 下一页

    private final PumpkinMail plugin;
    private final RewardApplier applier;
    private final Map<UUID, MailboxSession> mailbox = new HashMap<>();
    private final Map<UUID, AdminSession> admin = new HashMap<>();

    private record MailboxSession(Inventory inv, int page, List<Reward> all) {}
    private record AdminSession(Inventory inv, String phase, String selectedPreset) {}

    public GuiManager(PumpkinMail plugin) {
        this.plugin = plugin;
        this.applier = new RewardApplier(plugin);
    }

    // ================= 玩家邮箱 =================

    public void openMailbox(Player p) {
        List<Reward> all = plugin.storage().getPending(p.getName(), p.getUniqueId());
        int page = 0;
        Inventory inv = buildMailboxInv(all, page);
        mailbox.put(p.getUniqueId(), new MailboxSession(inv, page, all));
        p.openInventory(inv);
    }

    private Inventory buildMailboxInv(List<Reward> all, int page) {
        Inventory inv = Bukkit.createInventory(null, 54, C("&0&l南瓜邮箱"));
        paintMailbox(inv, all, page);
        return inv;
    }

    private void paintMailbox(Inventory inv, List<Reward> all, int page) {
        inv.clear();
        int start = page * PER_PAGE;
        int totalPages = Math.max(1, (int) Math.ceil(all.size() / (double) PER_PAGE));
        for (int i = 0; i < PER_PAGE; i++) {
            int idx = start + i;
            if (idx < all.size()) {
                inv.setItem(i, rewardIcon(all.get(idx)));
            } else if (i <= 44) {
                inv.setItem(i, pane());
            }
        }
        for (int i = 45; i <= 47; i++) inv.setItem(i, pane());
        if (page > 0) inv.setItem(NAV_BACK, nav("&e« 上一页", Material.ARROW));
        inv.setItem(NAV_CLOSE, nav("&c关闭", Material.BARRIER));
        if (page + 1 < totalPages) inv.setItem(NAV_NEXT, nav("&e下一页 »", Material.ARROW));
    }

    private void refreshMailbox(Player p, MailboxSession s) {
        List<Reward> all = plugin.storage().getPending(p.getName(), p.getUniqueId());
        int page = Math.min(s.page(), Math.max(0, (int) Math.ceil(all.size() / (double) PER_PAGE) - 1));
        MailboxSession ns = new MailboxSession(s.inv(), page, all);
        mailbox.put(p.getUniqueId(), ns);
        paintMailbox(ns.inv(), all, page);
    }

    // ================= 管理端批量发放 =================

    public void openAdmin(Player p) {
        List<Preset> presets = plugin.storage().listPresets();
        if (presets.isEmpty()) {
            p.sendMessage(C("&c奖品库为空！先用 &e/mailbox preset add <ID> <奖品> [备注] &c添加奖品。"));
            return;
        }
        Inventory inv = Bukkit.createInventory(null, 54, C("&6活动奖品库 · 选择要发放的奖品"));
        int i = 0;
        for (Preset pr : presets) {
            inv.setItem(i++, presetIcon(pr));
        }
        for (; i <= 44; i++) inv.setItem(i, pane());
        inv.setItem(49, nav("&c关闭", Material.BARRIER));
        admin.put(p.getUniqueId(), new AdminSession(inv, "pick", null));
        p.openInventory(inv);
    }

    private void openAdminTarget(Player p, String presetId) {
        Inventory inv = Bukkit.createInventory(null, 27, C("&6发放 &e" + presetId + "&6 → 选择对象"));
        inv.setItem(0, nav("&a在线玩家", Material.PLAYER_HEAD));
        inv.setItem(1, nav("&a白名单玩家", Material.WHITE_WOOL));
        inv.setItem(2, nav("&e名单文件", Material.PAPER));
        inv.setItem(3, nav("&e手动输入名单", Material.OAK_SIGN));
        inv.setItem(8, nav("&c返回", Material.ARROW));
        admin.put(p.getUniqueId(), new AdminSession(inv, "target", presetId));
        p.openInventory(inv);
    }

    // ================= 点击处理 =================

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        UUID uuid = p.getUniqueId();

        MailboxSession ms = mailbox.get(uuid);
        if (ms != null && ms.inv().equals(e.getInventory())) {
            e.setCancelled(true);
            int slot = e.getRawSlot();
            if (slot == NAV_CLOSE) { p.closeInventory(); return; }
            if (slot == NAV_BACK && ms.page() > 0) { refreshMailbox(p, new MailboxSession(ms.inv(), ms.page() - 1, ms.all())); return; }
            if (slot == NAV_NEXT && (ms.page() + 1) * PER_PAGE < ms.all().size()) { refreshMailbox(p, new MailboxSession(ms.inv(), ms.page() + 1, ms.all())); return; }
            if (slot >= 0 && slot < PER_PAGE) {
                int idx = ms.page() * PER_PAGE + slot;
                if (idx < ms.all().size()) claimReward(p, ms.all().get(idx), ms);
            }
            return;
        }

        AdminSession as = admin.get(uuid);
        if (as != null && as.inv().equals(e.getInventory())) {
            e.setCancelled(true);
            int slot = e.getRawSlot();
            if ("pick".equals(as.phase())) {
                List<Preset> presets = plugin.storage().listPresets();
                if (slot >= 0 && slot < presets.size()) openAdminTarget(p, presets.get(slot).id());
                else if (slot == NAV_CLOSE) p.closeInventory();
            } else if ("target".equals(as.phase())) {
                String presetId = as.selectedPreset();
                switch (slot) {
                    case 0 -> give(p, presetId, "@online");
                    case 1 -> give(p, presetId, "@whitelist");
                    case 2 -> { p.closeInventory(); promptText(p, "&e请输入名单文件路径（相对 PumpkinMail 插件目录，如 data/名单.txt）：", input -> give(p, presetId, "@file:" + input.trim())); }
                    case 3 -> { p.closeInventory(); promptText(p, "&e请输入玩家名单（用英文逗号分隔）：", input -> give(p, presetId, input.trim())); }
                    case 8 -> openAdmin(p);
                    case 26 -> p.closeInventory();
                    default -> { }
                }
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        // 只清理"正在关闭的这个界面"对应的 session；
        // 界面间切换（如奖品库→选对象）时会先触发旧界面关闭，不能把新 session 一起清掉。
        Inventory inv = e.getInventory();
        MailboxSession ms = mailbox.get(uuid);
        if (ms != null && ms.inv().equals(inv)) mailbox.remove(uuid);
        AdminSession as = admin.get(uuid);
        if (as != null && as.inv().equals(inv)) admin.remove(uuid);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        mailbox.remove(uuid);
        admin.remove(uuid);
    }

    private void claimReward(Player p, Reward r, MailboxSession ms) {
        boolean ok = applier.apply(p, r);
        if (ok) {
            plugin.storage().markClaimed(r.id());
            p.sendMessage(C("&a已领取奖励：" + (r.desc() == null || r.desc().isBlank() ? "奖品" : r.desc())));
            p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        } else {
            p.sendMessage(C("&c领取失败，请联系管理员。"));
        }
        refreshMailbox(p, ms);
    }

    private void give(Player admin, String presetId, String target) {
        int n = new com.nangua.pumpkinmail.cmd.MailboxCommand(plugin).give(presetId, target, admin.getName());
        if (n > 0) {
            admin.sendMessage(C("&a已向 &e" + n + " &a名玩家发放 &e" + presetId));
            admin.playSound(admin.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        } else {
            admin.sendMessage(C("&c没有发放成功：检查奖品 ID 或名单是否有效。"));
        }
    }

    // ================= 辅助 =================

    private ItemStack rewardIcon(Reward r) {
        Material mat; String head;
        switch (r.rewardType()) {
            case "money" -> { mat = Material.GOLD_INGOT; head = "货币奖励"; }
            case "item" -> {
                String[] p = r.data().split(":", 2);
                mat = Material.matchMaterial(p[0].toUpperCase());
                if (mat == null) mat = Material.GRASS_BLOCK;
                head = p[0].toUpperCase() + " x" + (p.length > 1 ? p[1] : "1");
            }
            case "command" -> { mat = Material.PAPER; head = "命令奖励"; }
            default -> { mat = Material.BOOK; head = "奖励"; }
        }
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(C("&6" + head));
        List<String> lore = new ArrayList<>();
        if (r.desc() != null && !r.desc().isBlank()) lore.add(C("&f" + r.desc()));
        lore.add(C("&7发放人：" + (r.issuedBy() == null ? "系统" : r.issuedBy())));
        lore.add(C("&7时间：" + fmt(r.issuedTime())));
        lore.add(C("&a左键点击领取"));
        meta.setLore(lore);
        it.setItemMeta(meta);
        return it;
    }

    private ItemStack presetIcon(Preset pr) {
        ItemStack it = new ItemStack(iconForType(pr.rewardType()));
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(C("&6" + pr.id()));
        List<String> lore = new ArrayList<>();
        lore.add(C("&f" + pr.summary()));
        if (pr.desc() != null && !pr.desc().isBlank()) lore.add(C("&7备注：" + pr.desc()));
        lore.add(C("&a点击 → 选择发放对象"));
        meta.setLore(lore);
        it.setItemMeta(meta);
        return it;
    }

    private Material iconForType(String t) {
        return switch (t) {
            case "money" -> Material.GOLD_INGOT;
            case "item" -> Material.CHEST;
            case "command" -> Material.COMMAND_BLOCK;
            default -> Material.BOOK;
        };
    }

    private ItemStack pane() {
        ItemStack it = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(" ");
        it.setItemMeta(m);
        return it;
    }

    private ItemStack nav(String name, Material mat) {
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(C(name));
        it.setItemMeta(m);
        return it;
    }

    private void promptText(Player p, String msg, java.util.function.Consumer<String> consumer) {
        p.sendMessage(C(msg));
        p.sendMessage(C("&7（输入内容后回车；输入 &ccancel &7取消）"));
        com.nangua.pumpkinmail.ChatInput.await(p, consumer);
    }

    private static final SimpleDateFormat FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private static String fmt(long t) { return FMT.format(new Date(t)); }
    static String C(String s) { return ChatColor.translateAlternateColorCodes('&', s); }

    public void closeAll() {
        for (UUID uuid : mailbox.keySet()) { Player p = Bukkit.getPlayer(uuid); if (p != null) p.closeInventory(); }
        for (UUID uuid : admin.keySet()) { Player p = Bukkit.getPlayer(uuid); if (p != null) p.closeInventory(); }
        mailbox.clear();
        admin.clear();
    }
}
