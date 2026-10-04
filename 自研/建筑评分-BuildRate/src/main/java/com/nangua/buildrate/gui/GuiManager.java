package com.nangua.buildrate.gui;

import com.nangua.buildrate.BuildRate;
import com.nangua.buildrate.storage.Db;
import com.nangua.buildrate.storage.Models;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/** 评分箱子 GUI：活动列表 → 单座建筑打分面板（基岩经 Geyser 自动转表单） */
public final class GuiManager implements Listener {
    public enum Mode { LIST, SCORE }

    /** 用自定义 Holder 标记我们的界面并携带上下文 */
    public static final class Ctx implements InventoryHolder {
        final Mode mode;
        final int buildId;      // SCORE 模式下要打分的建筑
        public Inventory inv;

        Ctx(Mode mode, int buildId) {
            this.mode = mode;
            this.buildId = buildId;
        }
        @Override public Inventory getInventory() { return inv; }
    }

    private final BuildRate plugin;

    public GuiManager(BuildRate plugin) {
        this.plugin = plugin;
    }

    private Db db() { return plugin.db(); }

    public void openList(Player p) {
        Models.Event ev = db().getActiveEvent();
        if (ev == null) {
            p.sendMessage(ChatColor.RED + "[建筑评分] 当前没有进行中的活动，等管理员创建后再来打分。");
            return;
        }
        List<Models.Build> builds = db().getBuilds(ev.id());
        Ctx ctx = new Ctx(Mode.LIST, -1);
        Inventory inv = Bukkit.createInventory(ctx, 54, ChatColor.DARK_PURPLE + "建筑评分 · " + ev.name());
        ctx.inv = inv;

        for (int i = 0; i < builds.size(); i++) {
            Models.Build b = builds.get(i);
            inv.setItem(i, buildItem(b));
        }
        for (int i = builds.size(); i < 53; i++) {
            inv.setItem(i, pane());
        }
        // 底部信息条
        inv.setItem(49, infoItem(ev, builds.size()));

        p.openInventory(inv);
    }

    public void openScore(Player p, int buildId) {
        Models.Build b = db().getBuild(buildId);
        Models.Event ev = db().getActiveEvent();
        if (b == null || ev == null || b.eventId() != ev.id() || !ev.isActive()) {
            p.sendMessage(ChatColor.RED + "[建筑评分] 该建筑或活动已不可评分。");
            return;
        }
        Ctx ctx = new Ctx(Mode.SCORE, buildId);
        Inventory inv = Bukkit.createInventory(ctx, 27, ChatColor.DARK_PURPLE + "给「" + b.name() + "」打分");
        ctx.inv = inv;

        int min = plugin.minScore();
        int max = plugin.maxScore();
        int my = db().getMyScore(buildId, p.getName());

        for (int s = min; s <= max; s++) {
            inv.setItem(s - min, scoreButton(s, my));
        }
        // 当前分说明
        inv.setItem(13, currentInfo(b, my));
        // 返回
        inv.setItem(22, backButton());
        p.openInventory(inv);
    }

    // ---------- 物品构建 ----------

    private ItemStack buildItem(Models.Build b) {
        ItemStack it = new ItemStack(Material.BRICKS);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "「" + b.name() + "」");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "参评玩家：§f" + b.player());
        List<Models.Score> scores = db().getScores(b.id());
        lore.add(ChatColor.GRAY + "当前：§f" + avgText(scores) + " §7（" + scores.size() + " 票）");
        lore.add(ChatColor.GRAY + "点击打分");
        meta.setLore(lore);
        it.setItemMeta(meta);
        return it;
    }

    private String avgText(List<Models.Score> scores) {
        if (scores.isEmpty()) return "暂无评分";
        boolean drop = plugin.dropHiLo();
        long sum = 0; int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (Models.Score s : scores) { sum += s.score(); min = Math.min(min, s.score()); max = Math.max(max, s.score()); }
        int denom = scores.size();
        if (drop && scores.size() >= 3) { sum -= (min + max); denom = scores.size() - 2; }
        return String.format("%.1f 分", denom > 0 ? (double) sum / denom : 0.0);
    }

    private ItemStack scoreButton(int score, int my) {
        Material mat;
        if (score <= 3) mat = Material.RED_WOOL;
        else if (score <= 6) mat = Material.YELLOW_WOOL;
        else if (score <= 9) mat = Material.LIME_WOOL;
        else mat = Material.EMERALD_BLOCK;
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "打 " + score + " 分");
        List<String> lore = new ArrayList<>();
        lore.add(my >= 0
                ? ChatColor.GRAY + "当前你给的是：§f" + my + " §7分（再点一次改为 " + score + " 分）"
                : ChatColor.GRAY + "点击提交 " + score + " 分");
        meta.setLore(lore);
        it.setItemMeta(meta);
        return it;
    }

    private ItemStack currentInfo(Models.Build b, int my) {
        ItemStack it = new ItemStack(Material.BOOK);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "我的评分");
        List<String> lore = new ArrayList<>();
        if (my >= 0) {
            lore.add(ChatColor.GRAY + "你已给「" + b.name() + "」打了 §f" + my + " §7分");
            lore.add(ChatColor.GRAY + "可以随时改分，记录最新一次");
        } else {
            lore.add(ChatColor.GRAY + "你还没给这座建筑打分");
        }
        meta.setLore(lore);
        it.setItemMeta(meta);
        return it;
    }

    private ItemStack backButton() {
        ItemStack it = new ItemStack(Material.ARROW);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(ChatColor.GRAY + "← 返回活动列表");
        it.setItemMeta(meta);
        return it;
    }

    private ItemStack pane() {
        ItemStack it = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(" ");
        it.setItemMeta(meta);
        return it;
    }

    private ItemStack infoItem(Models.Event ev, int count) {
        ItemStack it = new ItemStack(Material.BEACON);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "活动：§f" + ev.name());
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "参评建筑：§f" + count + " 座");
        lore.add(ChatColor.GRAY + "打分范围：§f" + plugin.minScore() + " ~ " + plugin.maxScore() + " 分");
        lore.add(ChatColor.GRAY + (plugin.dropHiLo() ? "统计将去掉最高分与最低分" : "统计直接取平均"));
        meta.setLore(lore);
        it.setItemMeta(meta);
        return it;
    }

    // ---------- 点击处理 ----------

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof Ctx ctx)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;

        if (ctx.mode == Mode.LIST) {
            int slot = e.getRawSlot();
            if (slot < 0 || slot >= 53) return;
            Models.Event ev = db().getActiveEvent();
            if (ev == null) { p.closeInventory(); return; }
            List<Models.Build> builds = db().getBuilds(ev.id());
            if (slot < builds.size()) {
                openScore(p, builds.get(slot).id());
            }
        } else if (ctx.mode == Mode.SCORE) {
            int slot = e.getRawSlot();
            if (slot < 0 || slot >= 10) { // 非打分按钮（含返回）
                if (slot == 22) { openList(p); }
                return;
            }
            int score = plugin.minScore() + slot;
            doScore(p, ctx.buildId, score);
        }
    }

    private void doScore(Player p, int buildId, int score) {
        Models.Build b = db().getBuild(buildId);
        Models.Event ev = db().getActiveEvent();
        if (b == null || ev == null || b.eventId() != ev.id() || !ev.isActive()) {
            p.sendMessage(ChatColor.RED + "[建筑评分] 该建筑或活动已不可评分。");
            p.closeInventory();
            return;
        }
        if (score < plugin.minScore() || score > plugin.maxScore()) {
            p.sendMessage(ChatColor.RED + "[建筑评分] 分数需在 " + plugin.minScore() + "~" + plugin.maxScore() + " 之间。");
            return;
        }
        // 自评限制
        if (b.player().equalsIgnoreCase(p.getName()) && !plugin.allowSelfVote()) {
            p.sendMessage(ChatColor.RED + "[建筑评分] 不能给自己的建筑打分。");
            return;
        }
        db().upsertScore(ev.id(), buildId, p.getName(), score);
        p.sendMessage(ChatColor.GREEN + "[建筑评分] 你已给「" + b.name() + "」打了 " + score + " 分。");
        // 停留在打分面板，方便继续改分
        openScore(p, buildId);
    }
}
