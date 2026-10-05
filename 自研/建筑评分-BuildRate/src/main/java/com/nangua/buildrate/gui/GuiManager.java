package com.nangua.buildrate.gui;

import com.nangua.buildrate.BuildRate;
import com.nangua.buildrate.RewardDispatcher;
import com.nangua.buildrate.storage.Db;
import com.nangua.buildrate.storage.Models;
import com.nangua.buildrate.storage.Stats;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 箱子 GUI：玩家打分列表/打分面板 + 管理员管理面板（开启/截止/排名/参与度/历史）。 */
public final class GuiManager implements Listener {
    public enum Mode { LIST, SCORE, ADMIN, RESULT, VOTES, HISTORY }

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
    /** 等待聊天输入活动名的管理员 */
    private final Map<UUID, Boolean> pendingCreate = new HashMap<>();

    public GuiManager(BuildRate plugin) {
        this.plugin = plugin;
    }

    private Db db() { return plugin.db(); }

    // ================= 玩家打分 =================

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
        inv.setItem(13, currentInfo(b, my));
        inv.setItem(22, backButton());
        p.openInventory(inv);
    }

    // ================= 管理员面板 =================

    public void openAdmin(Player p) {
        Models.Event ev = db().getActiveEvent();
        Ctx ctx = new Ctx(Mode.ADMIN, -1);
        Inventory inv = Bukkit.createInventory(ctx, 27, ChatColor.DARK_PURPLE + "建筑评分 · 管理面板");
        ctx.inv = inv;

        inv.setItem(4, adminHead(ev));
        inv.setItem(10, adminBtn(Material.WRITABLE_BOOK, "§e创建活动",
                "在聊天栏输入新活动的名字"));
        inv.setItem(12, adminBtn(Material.EMERALD_BLOCK, "§a开启评分",
                ev == null ? "当前无活动，先创建" : "把「" + ev.name() + "」切到进行中，玩家可打分"));
        inv.setItem(14, adminBtn(Material.REDSTONE_BLOCK, "§c截止并结算",
                ev == null ? "当前无活动" : "结束「" + ev.name() + "」，按名次把奖励发到邮箱"));
        inv.setItem(16, adminBtn(Material.GOLD_INGOT, "§6查看排名", "按均分排名展示参评建筑"));
        inv.setItem(21, adminBtn(Material.BOOK, "§b参与度", "查看每位评委投了几票"));
        inv.setItem(23, adminBtn(Material.BOOKSHELF, "§d活动历史", "查看历史活动列表"));
        for (int i = 0; i < 27; i++) if (inv.getItem(i) == null) inv.setItem(i, pane());

        p.openInventory(inv);
    }

    public void openResult(Player p) {
        Models.Event ev = currentOrLatest();
        if (ev == null) { p.sendMessage(ChatColor.RED + "[建筑评分] 还没有任何活动。"); return; }
        List<Models.Build> builds = db().getBuilds(ev.id());
        Map<Integer, List<Models.Score>> scores = db().getScoresByBuild(ev.id());
        List<Models.BuildResult> results = Stats.compute(builds, scores, plugin.dropHiLo());
        Ctx ctx = new Ctx(Mode.RESULT, -1);
        Inventory inv = Bukkit.createInventory(ctx, 54, ChatColor.DARK_PURPLE + "排名 · " + ev.name());
        ctx.inv = inv;
        int slot = 0;
        int rank = 1;
        for (Models.BuildResult r : results) {
            if (slot >= 45) break;
            inv.setItem(slot++, rankItem(r, rank++));
        }
        inv.setItem(49, backButton());
        for (int i = 0; i < 54; i++) if (inv.getItem(i) == null) inv.setItem(i, pane());
        p.openInventory(inv);
    }

    public void openVotes(Player p) {
        Models.Event ev = currentOrLatest();
        if (ev == null) { p.sendMessage(ChatColor.RED + "[建筑评分] 还没有任何活动。"); return; }
        Map<String, Integer> votes = db().getVoterStats(ev.id());
        Ctx ctx = new Ctx(Mode.VOTES, -1);
        Inventory inv = Bukkit.createInventory(ctx, 54, ChatColor.DARK_PURPLE + "参与度 · " + ev.name());
        ctx.inv = inv;
        int slot = 0;
        for (Map.Entry<String, Integer> en : votes.entrySet()) {
            if (slot >= 45) break;
            inv.setItem(slot++, voterItem(en.getKey(), en.getValue()));
        }
        inv.setItem(49, backButton());
        for (int i = 0; i < 54; i++) if (inv.getItem(i) == null) inv.setItem(i, pane());
        p.openInventory(inv);
    }

    public void openHistory(Player p) {
        List<Models.Event> all = db().listEvents();
        Ctx ctx = new Ctx(Mode.HISTORY, -1);
        Inventory inv = Bukkit.createInventory(ctx, 54, ChatColor.DARK_PURPLE + "活动历史");
        ctx.inv = inv;
        int slot = 0;
        for (Models.Event ev : all) {
            if (slot >= 45) break;
            inv.setItem(slot++, historyItem(ev));
        }
        inv.setItem(49, backButton());
        for (int i = 0; i < 54; i++) if (inv.getItem(i) == null) inv.setItem(i, pane());
        p.openInventory(inv);
    }

    private Models.Event currentOrLatest() {
        Models.Event ev = db().getActiveEvent();
        if (ev != null) return ev;
        List<Models.Event> all = db().listEvents();
        return all.isEmpty() ? null : all.get(0);
    }

    // ---------- 管理动作 ----------

    private void doStart(Player p) {
        Models.Event ev = db().getOpenEvent();
        if (ev == null) { p.sendMessage(ChatColor.RED + "[建筑评分] 先创建活动（管理面板「创建活动」）。"); return; }
        db().setEventStatus(ev.id(), 1);
        p.sendMessage(ChatColor.GREEN + "[建筑评分] 活动「" + ev.name() + "」已开启评分，玩家可打分。");
        com.nangua.buildrate.cmd.BuildRateCommand.broadcastStart(ev.name());
        openAdmin(p);
    }

    private void doStop(Player p) {
        Models.Event ev = db().getActiveEvent();
        if (ev == null) { p.sendMessage(ChatColor.RED + "[建筑评分] 当前没有进行中的活动。"); return; }
        db().setEventStatus(ev.id(), 2);
        if (new RewardDispatcher(plugin).enabled()) {
            new RewardDispatcher(plugin).settleEvent(ev);
            p.sendMessage(ChatColor.GREEN + "[建筑评分] 「" + ev.name() + "」已截止，奖励已按名次发到邮箱。");
        } else {
            p.sendMessage(ChatColor.GRAY + "[建筑评分] rewards.enabled=false，仅截止未发奖。");
        }
        openAdmin(p);
    }

    private void promptCreate(Player p) {
        p.closeInventory();
        pendingCreate.put(p.getUniqueId(), true);
        p.sendMessage(ChatColor.YELLOW + "[建筑评分] 请在聊天栏直接输入新活动的名字（10 秒内）：");
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
        long sum = 0; int mn = Integer.MAX_VALUE, mx = Integer.MIN_VALUE;
        for (Models.Score s : scores) { sum += s.score(); mn = Math.min(mn, s.score()); mx = Math.max(mx, s.score()); }
        int denom = scores.size();
        if (drop && scores.size() >= 3) { sum -= (mn + mx); denom = scores.size() - 2; }
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
        meta.setDisplayName(ChatColor.GRAY + "← 返回管理面板");
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

    private ItemStack adminHead(Models.Event ev) {
        ItemStack it = new ItemStack(Material.BEACON);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "建筑评分管理");
        List<String> lore = new ArrayList<>();
        if (ev == null) lore.add(ChatColor.GRAY + "当前：§f无进行中活动");
        else lore.add(ChatColor.GRAY + "当前：§f" + ev.name() + " §7（" + (ev.isActive() ? "进行中" : "已截止") + "）");
        meta.setLore(lore);
        it.setItemMeta(meta);
        return it;
    }

    private ItemStack adminBtn(Material m, String name, String... loreLines) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(name);
        List<String> lore = new ArrayList<>();
        for (String l : loreLines) lore.add(ChatColor.GRAY + l);
        meta.setLore(lore);
        it.setItemMeta(meta);
        return it;
    }

    private ItemStack rankItem(Models.BuildResult r, int rank) {
        ItemStack it = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "#" + rank + " §f" + r.build().name());
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "玩家：§f" + r.build().player());
        lore.add(ChatColor.GRAY + "均分：§a" + String.format("%.1f", r.avg()) + " §7· " + r.votes() + " 票");
        if (r.dropped() > 0) lore.add(ChatColor.DARK_GRAY + "（去最高最低后，原始 " + r.rawVotes() + " 票）");
        meta.setLore(lore);
        it.setItemMeta(meta);
        return it;
    }

    private ItemStack voterItem(String name, int votes) {
        ItemStack it = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + name);
        meta.setLore(List.of(ChatColor.GRAY + "投了 §f" + votes + " §7票"));
        it.setItemMeta(meta);
        return it;
    }

    private ItemStack historyItem(Models.Event ev) {
        ItemStack it = new ItemStack(Material.BOOK);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "#" + ev.id() + " " + ev.name());
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        meta.setLore(List.of(
                ChatColor.GRAY + "建筑：§f" + db().getBuilds(ev.id()).size() + " 座",
                ChatColor.GRAY + "时间：§f" + df.format(new Date(ev.createdAt())),
                ChatColor.GRAY + "状态：§f" + (ev.isActive() ? "进行中" : "已截止")));
        it.setItemMeta(meta);
        return it;
    }

    // ---------- 点击处理 ----------

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof Ctx ctx)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;

        switch (ctx.mode) {
            case LIST -> {
                int slot = e.getRawSlot();
                if (slot < 0 || slot >= 53) return;
                Models.Event ev = db().getActiveEvent();
                if (ev == null) { p.closeInventory(); return; }
                List<Models.Build> builds = db().getBuilds(ev.id());
                if (slot < builds.size()) openScore(p, builds.get(slot).id());
            }
            case SCORE -> {
                int slot = e.getRawSlot();
                if (slot < 0 || slot >= 10) {
                    if (slot == 22) openList(p);
                    return;
                }
                int score = plugin.minScore() + slot;
                doScore(p, ctx.buildId, score);
            }
            case ADMIN -> {
                int slot = e.getRawSlot();
                switch (slot) {
                    case 10 -> promptCreate(p);
                    case 12 -> doStart(p);
                    case 14 -> doStop(p);
                    case 16 -> openResult(p);
                    case 21 -> openVotes(p);
                    case 23 -> openHistory(p);
                }
            }
            case RESULT, VOTES, HISTORY -> {
                if (e.getRawSlot() == 49) openAdmin(p);
            }
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
        if (b.player().equalsIgnoreCase(p.getName()) && !plugin.allowSelfVote()) {
            p.sendMessage(ChatColor.RED + "[建筑评分] 不能给自己的建筑打分。");
            return;
        }
        db().upsertScore(ev.id(), buildId, p.getName(), score);
        p.sendMessage(ChatColor.GREEN + "[建筑评分] 你已给「" + b.name() + "」打了 " + score + " 分。");
        openScore(p, buildId);
    }

    // ---------- 聊天输入活动名 ----------

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        if (!pendingCreate.containsKey(e.getPlayer().getUniqueId())) return;
        e.setCancelled(true);
        pendingCreate.remove(e.getPlayer().getUniqueId());
        String name = e.getMessage().trim();
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (name.isEmpty()) { p.sendMessage(ChatColor.RED + "[建筑评分] 活动名不能为空。"); return; }
            Models.Event ev = db().createEvent(name);
            p.sendMessage(ChatColor.GREEN + "[建筑评分] 已创建活动「" + ev.name() + "」（待开始，玩家可先报名）。");
            com.nangua.buildrate.cmd.BuildRateCommand.broadcastCreate(ev.name());
            openAdmin(p);
        });
    }
}
