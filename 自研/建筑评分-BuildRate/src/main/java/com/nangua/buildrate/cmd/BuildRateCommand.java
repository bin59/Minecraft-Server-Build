package com.nangua.buildrate.cmd;

import com.nangua.buildrate.BuildRate;
import com.nangua.buildrate.RewardDispatcher;
import com.nangua.buildrate.storage.Db;
import com.nangua.buildrate.storage.Models;
import com.nangua.buildrate.storage.Stats;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** /br 命令分发：活动创建/报名/打分/统计 */
public final class BuildRateCommand implements CommandExecutor, TabCompleter {

    private final BuildRate plugin;

    public BuildRateCommand(BuildRate plugin) {
        this.plugin = plugin;
    }

    private Db db() { return plugin.db(); }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
        if (args.length == 0) { help(s); return true; }
        String sub = args[0].toLowerCase();
        try {
            switch (sub) {
                case "create": return create(s, args);
                case "list": return list(s);
                case "join": return join(s, args);
                case "add": return add(s, args);
                case "remove": return remove(s, args);
                case "start": return start(s);
                case "stop": return stop(s);
                case "admin": return admin(s);
                case "open": return open(s);
                case "score": return score(s, args);
                case "result": return result(s);
                case "stats": return stats(s);
                case "history": return history(s);
                case "help": help(s); return true;
                default: help(s); return true;
            }
        } catch (RuntimeException ex) {
            s.sendMessage(ChatColor.RED + "[建筑评分] 操作出错：" + ex.getMessage());
            return true;
        }
    }

    // ---------- 管理员：活动 ----------

    private boolean create(CommandSender s, String[] args) {
        if (!requireAdmin(s)) return true;
        String name = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)).trim() : "";
        if (name.isEmpty()) name = "建筑大赛";
        Models.Event ev = db().createEvent(name);
        s.sendMessage(ChatColor.GREEN + "[建筑评分] 已创建活动「" + ev.name() + "」（待开始）。");
        s.sendMessage(ChatColor.GRAY + "玩家可先 /br join <建筑名> 报名；管理员 /br start 开启评分后才能打分。");
        broadcastCreate(ev.name());
        return true;
    }

    /** 创建活动后全服公告。 */
    public static void broadcastCreate(String eventName) {
        Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "「建筑评分」新活动开始报名：§e" + eventName
                + ChatColor.LIGHT_PURPLE + "！输入 §f/br join <建筑名> §7报名参评（评分开始后会另行公告）。");
    }

    /** 开启评分时全服公告。 */
    public static void broadcastStart(String eventName) {
        Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "「建筑评分」评分已开始：§e" + eventName
                + ChatColor.LIGHT_PURPLE + "！输入 §f/br open §7打开面板给建筑打分。");
    }

    private boolean start(CommandSender s) {
        if (!requireAdmin(s)) return true;
        Models.Event ev = db().getOpenEvent();
        if (ev == null) { s.sendMessage(ChatColor.RED + "[建筑评分] 当前没有活动，先用 /br create <名称> 创建。"); return true; }
        db().setEventStatus(ev.id(), 1);
        s.sendMessage(ChatColor.GREEN + "[建筑评分] 活动「" + ev.name() + "」已开启评分，玩家可打分。");
        broadcastStart(ev.name());
        return true;
    }

    private boolean stop(CommandSender s) {
        if (!requireAdmin(s)) return true;
        Models.Event ev = db().getActiveEvent();
        if (ev == null) { s.sendMessage(ChatColor.RED + "[建筑评分] 当前没有进行中的活动。"); return true; }
        db().setEventStatus(ev.id(), 2);
        // 自动按名次结算发奖（南瓜币与物品都发到邮箱）
        if (new RewardDispatcher(plugin).enabled()) {
            new RewardDispatcher(plugin).settleEvent(ev);
            s.sendMessage(ChatColor.GREEN + "[建筑评分] 活动「" + ev.name() + "」已截止，奖励已按名次发到获奖玩家邮箱（/mailbox 领取，含南瓜币）。");
        } else {
            s.sendMessage(ChatColor.GRAY + "[建筑评分] rewards.enabled 为 false，本次未发奖。活动「" + ev.name() + "」已截止。");
        }
        return true;
    }

    private boolean admin(CommandSender s) {
        if (!requireAdmin(s)) return true;
        if (!(s instanceof Player p)) { s.sendMessage(ChatColor.RED + "[建筑评分] 只有玩家能打开管理面板。"); return true; }
        plugin.gui().openAdmin(p);
        return true;
    }

    private boolean add(CommandSender s, String[] args) {
        if (!requireAdmin(s)) return true;
        if (args.length < 3) { s.sendMessage(ChatColor.YELLOW + "用法：/br add <玩家> <建筑名>"); return true; }
        Models.Event ev = db().getOpenEvent();
        if (ev == null) { s.sendMessage(ChatColor.RED + "[建筑评分] 当前没有可报名的活动。"); return true; }
        String player = args[1];
        String name = String.join(" ", Arrays.copyOfRange(args, 2, args.length)).trim();
        if (plugin.onePerPlayer() && db().getPlayerBuild(ev.id(), player) != null) {
            s.sendMessage(ChatColor.RED + "[建筑评分] 玩家 " + player + " 已在活动中报名。");
            return true;
        }
        Location loc = pickLocation(player, s);
        Models.Build b = db().addBuild(ev.id(), player, name,
                loc != null ? loc.getWorld().getName() : null,
                loc != null ? loc.getBlockX() : 0,
                loc != null ? loc.getBlockY() : 0,
                loc != null ? loc.getBlockZ() : 0);
        s.sendMessage(ChatColor.GREEN + "[建筑评分] 已为 " + b.player() + " 登记建筑「" + b.name() + "」（编号 " + b.id() + "）。");
        return true;
    }

    private boolean remove(CommandSender s, String[] args) {
        if (!requireAdmin(s)) return true;
        if (args.length < 2) { s.sendMessage(ChatColor.YELLOW + "用法：/br remove <编号>"); return true; }
        int id = parseInt(args[1], -1);
        if (id < 0) { s.sendMessage(ChatColor.RED + "[建筑评分] 编号需为数字。"); return true; }
        Models.Build b = db().getBuild(id);
        if (b == null) { s.sendMessage(ChatColor.RED + "[建筑评分] 没有编号 " + id + " 的建筑。"); return true; }
        db().removeBuild(id);
        s.sendMessage(ChatColor.GREEN + "[建筑评分] 已移除「" + b.name() + "」（含其全部评分）。");
        return true;
    }

    private boolean stats(CommandSender s) {
        if (!requireAdmin(s)) return true;
        Models.Event ev = db().getActiveEvent();
        if (ev == null) { s.sendMessage(ChatColor.RED + "[建筑评分] 当前没有活动。"); return true; }
        Map<String, Integer> votes = db().getVoterStats(ev.id());
        s.sendMessage(ChatColor.LIGHT_PURPLE + "===== 「" + ev.name() + "」投票参与度 =====");
        if (votes.isEmpty()) { s.sendMessage(ChatColor.GRAY + "（还没有任何人打分）"); return true; }
        for (Map.Entry<String, Integer> en : votes.entrySet()) {
            s.sendMessage(ChatColor.GRAY + en.getKey() + ChatColor.WHITE + " 投了 §f" + en.getValue() + " §7票");
        }
        return true;
    }

    // ---------- 玩家：报名 / 打分 ----------

    private boolean join(CommandSender s, String[] args) {
        if (!(s instanceof Player p)) { s.sendMessage(ChatColor.RED + "只有玩家能报名。"); return true; }
        if (args.length < 2) { s.sendMessage(ChatColor.YELLOW + "用法：/br join <建筑名>"); return true; }
        Models.Event ev = db().getOpenEvent();
        if (ev == null) { s.sendMessage(ChatColor.RED + "[建筑评分] 当前没有可报名的活动。"); return true; }
        String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).trim();
        Models.Build mine = db().getPlayerBuild(ev.id(), p.getName());
        if (mine != null) {
            if (plugin.onePerPlayer()) {
                db().updateBuildName(mine.id(), name);
                s.sendMessage(ChatColor.GREEN + "[建筑评分] 你已更新自己的参评建筑为「" + name + "」。");
                return true;
            }
            s.sendMessage(ChatColor.RED + "[建筑评分] 你已在活动中报名。");
            return true;
        }
        Location loc = p.getLocation();
        Models.Build b = db().addBuild(ev.id(), p.getName(), name,
                loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        s.sendMessage(ChatColor.GREEN + "[建筑评分] 报名成功：建筑「" + b.name() + "」（编号 " + b.id() + "）。");
        s.sendMessage(ChatColor.GRAY + "其他人可用 /br open 给你打分。");
        return true;
    }

    private boolean open(CommandSender s) {
        if (!(s instanceof Player p)) { s.sendMessage(ChatColor.RED + "只有玩家能打开评分面板。"); return true; }
        plugin.gui().openList(p);
        return true;
    }

    private boolean score(CommandSender s, String[] args) {
        if (!(s instanceof Player p)) { s.sendMessage(ChatColor.RED + "只有玩家能打分。"); return true; }
        if (args.length < 3) { s.sendMessage(ChatColor.YELLOW + "用法：/br score <建筑编号> <1~10>"); return true; }
        Models.Event ev = db().getActiveEvent();
        if (ev == null) { s.sendMessage(ChatColor.RED + "[建筑评分] 当前没有进行中的活动。"); return true; }
        int id = parseInt(args[1], -1);
        int sc = parseInt(args[2], -1);
        if (id < 0) { s.sendMessage(ChatColor.RED + "[建筑评分] 建筑编号需为数字（/br list 查看）。"); return true; }
        if (sc < plugin.minScore() || sc > plugin.maxScore()) {
            s.sendMessage(ChatColor.RED + "[建筑评分] 分数需在 " + plugin.minScore() + "~" + plugin.maxScore() + " 之间。");
            return true;
        }
        Models.Build b = db().getBuild(id);
        if (b == null || b.eventId() != ev.id() || !ev.isActive()) {
            s.sendMessage(ChatColor.RED + "[建筑评分] 该建筑不属于当前活动或已截止。");
            return true;
        }
        if (b.player().equalsIgnoreCase(p.getName()) && !plugin.allowSelfVote()) {
            s.sendMessage(ChatColor.RED + "[建筑评分] 不能给自己的建筑打分。");
            return true;
        }
        db().upsertScore(ev.id(), id, p.getName(), sc);
        p.sendMessage(ChatColor.GREEN + "[建筑评分] 你已给「" + b.name() + "」打了 " + sc + " 分。");
        return true;
    }

    // ---------- 查询 ----------

    private boolean list(CommandSender s) {
        Models.Event ev = db().getOpenEvent();
        if (ev == null) { s.sendMessage(ChatColor.RED + "[建筑评分] 当前没有活动。"); return true; }
        List<Models.Build> builds = db().getBuilds(ev.id());
        Map<Integer, List<Models.Score>> scores = db().getScoresByBuild(ev.id());
        List<Models.BuildResult> results = Stats.compute(builds, scores, plugin.dropHiLo());
        s.sendMessage(ChatColor.LIGHT_PURPLE + "===== 「" + ev.name() + "」参评列表 =====");
        s.sendMessage(ChatColor.GRAY + (ev.isActive() ? "状态：进行中" : "状态：已截止"));
        if (results.isEmpty()) { s.sendMessage(ChatColor.GRAY + "（还没有参评建筑，玩家用 /br join <建筑名> 报名）"); return true; }
        for (Models.BuildResult r : results) {
            s.sendMessage(ChatColor.YELLOW + "#" + r.build().id()
                    + " §f" + r.build().name()
                    + ChatColor.GRAY + "（" + r.build().player() + "）"
                    + ChatColor.AQUA + " 均分 " + fmt(r.avg())
                    + ChatColor.GRAY + " · " + r.votes() + "票"
                    + (r.dropped() > 0 ? ChatColor.DARK_GRAY + "（去最高最低后）" : ""));
        }
        return true;
    }

    private boolean result(CommandSender s) {
        Models.Event ev = db().getActiveEvent();
        if (ev == null) {
            // 无进行中活动时，展示最近一场历史活动结果
            List<Models.Event> all = db().listEvents();
            if (all.isEmpty()) { s.sendMessage(ChatColor.RED + "[建筑评分] 还没有任何活动。"); return true; }
            ev = all.get(0);
        }
        List<Models.Build> builds = db().getBuilds(ev.id());
        Map<Integer, List<Models.Score>> scores = db().getScoresByBuild(ev.id());
        List<Models.BuildResult> results = Stats.compute(builds, scores, plugin.dropHiLo());
        s.sendMessage(ChatColor.LIGHT_PURPLE + "===== 「" + ev.name() + "」评分排名 =====");
        s.sendMessage(ChatColor.GRAY + (ev.isActive() ? "状态：进行中" : "状态：已截止") + " · " + (plugin.dropHiLo() ? "已去掉最高/最低分" : "直接平均"));
        if (results.isEmpty()) { s.sendMessage(ChatColor.GRAY + "（暂无可统计的结果）"); return true; }
        int rank = 1;
        for (Models.BuildResult r : results) {
            s.sendMessage(ChatColor.GOLD + "#" + rank++ + "  " + ChatColor.WHITE + r.build().name()
                    + ChatColor.GRAY + "（" + r.build().player() + "）"
                    + ChatColor.GREEN + " " + fmt(r.avg()) + " 分"
                    + ChatColor.GRAY + " · " + r.votes() + "票"
                    + (r.dropped() > 0 ? "（去最高最低后，原始 " + r.rawVotes() + " 票）" : ""));
        }
        return true;
    }

    private boolean history(CommandSender s) {
        List<Models.Event> all = db().listEvents();
        if (all.isEmpty()) { s.sendMessage(ChatColor.RED + "[建筑评分] 还没有任何活动。"); return true; }
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        s.sendMessage(ChatColor.LIGHT_PURPLE + "===== 活动历史 =====");
        for (Models.Event ev : all) {
            int n = db().getBuilds(ev.id()).size();
            s.sendMessage(ChatColor.YELLOW + "#" + ev.id()
                    + ChatColor.WHITE + " " + ev.name()
                    + ChatColor.GRAY + " · " + n + " 座建筑"
                    + " · " + df.format(new Date(ev.createdAt()))
                    + " · " + (ev.isActive() ? "进行中" : "已截止"));
        }
        return true;
    }

    // ---------- 工具 ----------

    private boolean requireAdmin(CommandSender s) {
        if (plugin.isAdmin(s)) return true;
        s.sendMessage(ChatColor.RED + "[建筑评分] 需要管理员权限 buildrate.admin。");
        return false;
    }

    private Location pickLocation(String player, CommandSender sender) {
        Player target = plugin.getServer().getPlayerExact(player);
        if (target != null) return target.getLocation();
        if (sender instanceof Player sp) return sp.getLocation();
        return null;
    }

    private int parseInt(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return def; }
    }

    private String fmt(double d) { return String.format("%.1f", d); }

    private void help(CommandSender s) {
        s.sendMessage(ChatColor.LIGHT_PURPLE + "===== 建筑评分 BuildRate =====");
        s.sendMessage(ChatColor.GRAY + "玩家指令：");
        s.sendMessage(ChatColor.WHITE + "/br join <建筑名> §7报名参评（可改分/改名）");
        s.sendMessage(ChatColor.WHITE + "/br open §7打开打分面板");
        s.sendMessage(ChatColor.WHITE + "/br score <编号> <1~10> §7命令打分（基岩兜底）");
        s.sendMessage(ChatColor.WHITE + "/br list §7参评列表 /br result §7排名");
        s.sendMessage(ChatColor.GRAY + "管理员指令（buildrate.admin）：");
        s.sendMessage(ChatColor.WHITE + "/br admin §7打开管理箱子菜单（开启/截止/排名/参与度/历史）");
        s.sendMessage(ChatColor.WHITE + "/br create <活动名> §7创建活动（自动截止旧活动）");
        s.sendMessage(ChatColor.WHITE + "/br add <玩家> <建筑名> §7代报名 /br remove <编号> §7移除");
        s.sendMessage(ChatColor.WHITE + "/br start §7开启评分 /br stop §7截止并自动结算发奖");
        s.sendMessage(ChatColor.WHITE + "/br result §7排名 /br stats §7参与度 /br history §7历史");
    }

    // ---------- Tab 补全 ----------

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String label, String[] args) {
        List<String> subs = Arrays.asList("create", "list", "join", "add", "remove",
                "start", "stop", "admin", "open", "score", "result", "stats", "history", "help");
        if (args.length == 1) {
            return subs.stream().filter(x -> x.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("score")) {
            Models.Event ev = db().getActiveEvent();
            if (ev == null) return new ArrayList<>();
            return db().getBuilds(ev.id()).stream()
                    .map(b -> String.valueOf(b.id())).collect(Collectors.toList());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("score")) {
            List<String> out = new ArrayList<>();
            for (int i = plugin.minScore(); i <= plugin.maxScore(); i++) out.add(String.valueOf(i));
            return out;
        }
        return new ArrayList<>();
    }
}
