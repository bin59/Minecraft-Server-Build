package com.nangua.pumpkinfly;

import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** /pfly 主命令：购买、开关、状态、记录与管理操作。 */
public final class FlyCommand implements TabExecutor {

    private final PumpkinFlyPlugin plugin;

    public FlyCommand(PumpkinFlyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "status" : args[0].toLowerCase();

        if (sub.equals("help")) { help(sender); return true; }
        if (sub.equals("reload")) {
            if (!requireAdmin(sender)) return true;
            plugin.reloadAll();
            sender.sendMessage(plugin.config().msg("reloaded"));
            return true;
        }
        if (sub.equals("buy")) return buy(sender, args);
        if (sub.equals("give") || sub.equals("take") || sub.equals("setperm")) return adminOp(sender, sub, args);
        if (sub.equals("status")) return status(sender, args);
        if (sub.equals("records")) return records(sender, args);
        if (sub.equals("on") || sub.equals("off") || sub.equals("toggle")) return toggle(sender, sub);

        help(sender);
        return true;
    }

    // ------------------------------------------------------------------ 购买

    private boolean buy(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.config().msg("player-only"));
            return true;
        }
        Player p = (Player) sender;
        if (!p.hasPermission("pumpkinfly.buy")) {
            p.sendMessage(plugin.config().msg("no-permission"));
            return true;
        }
        if (args.length < 2) {
            p.sendMessage(plugin.config().msg("buy-usage", "cost", money(plugin.config().perMinute)));
            return true;
        }
        if (!plugin.economy().isReady()) {
            p.sendMessage(plugin.config().msg("economy-missing"));
            return true;
        }
        boolean perm = args[1].equalsIgnoreCase("perm") || args[1].equalsIgnoreCase("permanent");
        int minutes = 0;
        if (!perm) {
            try {
                minutes = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                p.sendMessage(plugin.config().msg("invalid-number"));
                return true;
            }
            if (minutes < 1 || minutes > plugin.config().maxMinutesPerBuy) {
                p.sendMessage(plugin.config().msg("invalid-number"));
                return true;
            }
        }

        PlayerData d = plugin.data().get(p.getUniqueId(), p.getName());
        if (perm && d.permanent && !plugin.config().allowDuplicatePermanent) {
            p.sendMessage(plugin.config().msg("already-permanent"));
            return true;
        }
        if (!perm && d.remainingSeconds + minutes * 60L > (long) plugin.config().maxBalanceMinutes * 60L) {
            p.sendMessage(plugin.config().msg("balance-limit"));
            return true;
        }

        double cost = perm ? plugin.config().permanentPrice : plugin.config().perMinute * minutes;
        double before = plugin.economy().balance(p);
        if (!plugin.economy().has(p, cost)) {
            p.sendMessage(plugin.config().msg("not-enough",
                    "cost", money(cost), "balance", money(before)));
            return true;
        }
        EconomyResponse r = plugin.economy().withdraw(p, cost);
        if (r == null || r.type != EconomyResponse.ResponseType.SUCCESS) {
            p.sendMessage(plugin.config().msg("withdraw-failed"));
            return true;
        }

        applyGrant(p, d, perm, minutes, true);
        writePurchase(p.getUniqueId(), d.name, perm ? "PERMANENT" : "TIME", minutes,
                cost, before, plugin.economy().balance(p), perm ? 0L : d.remainingSeconds,
                p.getName(), perm ? "永久飞行" : minutes + " 分钟");

        if (perm) {
            p.sendMessage(plugin.config().msg("buy-perm"));
        } else {
            p.sendMessage(plugin.config().msg("buy-time",
                    "minutes", String.valueOf(minutes), "cost", money(cost),
                    "remaining", duration(d.remainingSeconds)));
        }
        plugin.data().save();
        plugin.fly().checkNow(p, false);
        return true;
    }

    // ------------------------------------------------------------------ 管理操作

    private boolean adminOp(CommandSender sender, String sub, String[] args) {
        if (!requireAdmin(sender)) return true;
        int need = sub.equals("setperm") ? 3 : 3;
        if (args.length < need) {
            sender.sendMessage(plugin.config().msg("admin-usage"));
            return true;
        }
        OfflinePlayer target = resolve(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.config().msg("player-not-found"));
            return true;
        }
        UUID uuid = target.getUniqueId();
        String name = target.getName() == null ? args[1] : target.getName();
        PlayerData d = plugin.data().get(uuid, name);

        if (sub.equals("setperm")) {
            boolean value = Boolean.parseBoolean(args[2]);
            d.permanent = value;
            plugin.data().markDirty();
            writePurchase(uuid, d.name, "SETPERM", 0, 0D, 0D, 0D, d.remainingSeconds,
                    sender.getName(), "永久标记 → " + value);
            sender.sendMessage(plugin.config().msg("setperm", "player", name, "value", String.valueOf(value)));
        } else if (sub.equals("give")) {
            boolean perm = args[2].equalsIgnoreCase("perm") || args[2].equalsIgnoreCase("permanent");
            int minutes = 0;
            if (!perm) {
                try { minutes = Integer.parseInt(args[2]); } catch (NumberFormatException e) { minutes = -1; }
                if (minutes < 1) {
                    sender.sendMessage(plugin.config().msg("invalid-number"));
                    return true;
                }
            }
            applyGrant(null, d, perm, minutes, false);
            writePurchase(uuid, d.name, perm ? "GIVE_PERM" : "GIVE", minutes,
                    0D, 0D, 0D, perm ? 0L : d.remainingSeconds, sender.getName(),
                    perm ? "管理员发放永久" : "管理员发放 " + minutes + " 分钟");
            if (perm) sender.sendMessage(plugin.config().msg("give-perm", "player", name));
            else sender.sendMessage(plugin.config().msg("give-time", "player", name,
                    "minutes", String.valueOf(minutes), "remaining", duration(d.remainingSeconds)));
        } else {
            int minutes;
            try { minutes = Integer.parseInt(args[2]); } catch (NumberFormatException e) {
                sender.sendMessage(plugin.config().msg("invalid-number"));
                return true;
            }
            if (minutes < 1) { sender.sendMessage(plugin.config().msg("invalid-number")); return true; }
            d.remainingSeconds = Math.max(0L, d.remainingSeconds - minutes * 60L);
            plugin.data().markDirty();
            writePurchase(uuid, d.name, "TAKE", minutes, 0D, 0D, 0D, d.remainingSeconds,
                    sender.getName(), "管理员扣除 " + minutes + " 分钟");
            sender.sendMessage(plugin.config().msg("take", "player", name,
                    "minutes", String.valueOf(minutes), "remaining", duration(d.remainingSeconds)));
            Player online = target.getPlayer();
            if (online != null && online.isOnline()) plugin.fly().checkNow(online, false);
        }
        plugin.data().save();
        return true;
    }

    /** 统一修改账户（购买/发放共用）；charge 为 true 表示是玩家付费，由调用方先扣款。 */
    private void applyGrant(Player p, PlayerData d, boolean perm, int minutes, boolean charge) {
        String now = PlayerData.now();
        if (d.firstPurchase.isEmpty()) d.firstPurchase = now;
        d.lastActive = now;
        if (perm) {
            d.permanent = true;
        } else {
            long add = minutes * 60L;
            d.remainingSeconds += add;
            d.totalPurchasedSeconds += add;
            if (plugin.config().isRealTime()) {
                long base = Math.max(System.currentTimeMillis(), d.expireAt);
                d.expireAt = base + add * 1000L;
            }
        }
        plugin.data().markDirty();
    }

    private void writePurchase(UUID uuid, String name, String type, int minutes,
                               double cost, double before, double after, long remaining,
                               String operator, String detail) {
        Map<String, Object> rec = new LinkedHashMap<>();
        rec.put("time", PlayerData.now());
        rec.put("uuid", uuid.toString());
        rec.put("name", name);
        rec.put("type", type);
        rec.put("minutes", minutes);
        rec.put("cost", round2(cost));
        rec.put("balance-before", round2(before));
        rec.put("balance-after", round2(after));
        rec.put("remaining-after", remaining);
        rec.put("operator", operator);
        rec.put("detail", detail);
        plugin.data().addPurchase(rec);
        plugin.data().markDirty();
    }

    // ------------------------------------------------------------------ 开关 / 状态 / 记录

    private boolean toggle(CommandSender sender, String sub) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.config().msg("player-only"));
            return true;
        }
        Player p = (Player) sender;
        if (!p.hasPermission("pumpkinfly.use")) {
            p.sendMessage(plugin.config().msg("no-permission"));
            return true;
        }
        PlayerData d = plugin.data().get(p.getUniqueId(), p.getName());
        if (sub.equals("off")) {
            if (d.manualOff) { p.sendMessage(plugin.config().msg("already-off")); return true; }
            plugin.fly().setManualOff(p, true);
        } else if (sub.equals("on")) {
            if (!d.manualOff && p.getAllowFlight()) { p.sendMessage(plugin.config().msg("already-on")); return true; }
            plugin.fly().setManualOff(p, false);
        } else {
            plugin.fly().setManualOff(p, !d.manualOff);
        }
        plugin.data().save();
        return true;
    }

    private boolean status(CommandSender sender, String[] args) {
        Player target;
        if (args.length >= 2) {
            if (!requireAdmin(sender)) return true;
            OfflinePlayer off = resolve(args[1]);
            if (off == null || !off.isOnline() || off.getPlayer() == null) {
                sender.sendMessage(plugin.config().msg("player-not-found"));
                return true;
            }
            target = off.getPlayer();
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage(plugin.config().msg("admin-usage"));
            return true;
        }
        if (!sender.equals(target) && !requireAdmin(sender)) return true;

        PlayerData d = plugin.data().get(target.getUniqueId(), target.getName());
        ZoneChecker.Result zone = plugin.zones().check(target);
        FlyConfig c = plugin.config();
        String line = "status-line";
        sender.sendMessage(c.msg("status-title", "player", target.getName()));
        sender.sendMessage(c.msg(line, "label", "凭证", "value",
                d.permanent ? "&6永久飞行" : (d.hasPass() ? "&a限时飞行" : "&c无")));
        sender.sendMessage(c.msg(line, "label", "剩余时长", "value",
                d.permanent ? "不消耗" : duration(d.remainingSeconds)));
        sender.sendMessage(c.msg(line, "label", "累计购买 / 已飞", "value",
                duration(d.totalPurchasedSeconds) + " / " + duration(d.totalUsedSeconds)));
        sender.sendMessage(c.msg(line, "label", "当前区域", "value", zone.label));
        sender.sendMessage(c.msg(zone.allowed ? "zone-ok" : "zone-deny", "zone", zone.label));
        String state;
        if (FlyManager.isExempt(target)) state = "&7创造/旁观模式，插件不干预";
        else if (target.isFlying()) state = "&a飞行中";
        else if (target.getAllowFlight()) state = "&e已开飞，等待双击空格";
        else state = "&c未开启" + (d.manualOff ? "（手动关闭）" : "");
        sender.sendMessage(c.msg(line, "label", "飞行状态", "value", state));
        sender.sendMessage(c.msg(line, "label", "计时口径", "value",
                c.isRealTime() ? "现实时间倒计时" : "仅飞行中扣秒"));
        sender.sendMessage(c.msg(line, "label", "允许范围", "value", c.zoneSummary()));
        if (!d.firstPurchase.isEmpty()) {
            sender.sendMessage(c.msg(line, "label", "首次购买", "value", d.firstPurchase));
            sender.sendMessage(c.msg(line, "label", "最近活跃", "value", d.lastActive));
        }
        return true;
    }

    private boolean records(CommandSender sender, String[] args) {
        if (!sender.hasPermission("pumpkinfly.records") && !sender.hasPermission("pumpkinfly.admin")) {
            sender.sendMessage(plugin.config().msg("no-permission"));
            return true;
        }
        UUID uuid;
        String name;
        if (args.length >= 2) {
            if (!requireAdmin(sender)) return true;
            OfflinePlayer off = resolve(args[1]);
            if (off == null) { sender.sendMessage(plugin.config().msg("player-not-found")); return true; }
            uuid = off.getUniqueId();
            name = off.getName() == null ? args[1] : off.getName();
        } else if (sender instanceof Player) {
            uuid = ((Player) sender).getUniqueId();
            name = sender.getName();
        } else {
            sender.sendMessage(plugin.config().msg("admin-usage"));
            return true;
        }
        String kind = args.length >= 3 ? args[2].toLowerCase() : "buy";
        boolean session = kind.startsWith("s") || kind.equals("session") || kind.equals("飞行");
        List<Map<String, Object>> list = session
                ? plugin.data().sessionsOf(uuid, 10) : plugin.data().purchasesOf(uuid, 10);
        sender.sendMessage(plugin.config().msg("records-title",
                "type", session ? "飞行会话" : "购买", "player", name, "count", String.valueOf(list.size())));
        if (list.isEmpty()) {
            sender.sendMessage(plugin.config().msg("records-empty"));
            return true;
        }
        for (Map<String, Object> m : list) {
            if (session) {
                sender.sendMessage(plugin.config().msg("records-session",
                        "time", str(m.get("start")), "player", str(m.get("name")),
                        "seconds", str(m.get("used-seconds")), "reason", str(m.get("end-reason"))));
            } else {
                sender.sendMessage(plugin.config().msg("records-buy",
                        "time", str(m.get("time")), "type", str(m.get("type")),
                        "player", str(m.get("name")), "detail", str(m.get("detail")),
                        "cost", str(m.get("cost")), "balance", str(m.get("balance-after"))));
            }
        }
        return true;
    }

    // ------------------------------------------------------------------ 工具

    private boolean requireAdmin(CommandSender sender) {
        if (!sender.hasPermission("pumpkinfly.admin")) {
            sender.sendMessage(plugin.config().msg("no-permission"));
            return false;
        }
        return true;
    }

    private void help(CommandSender sender) {
        sender.sendMessage(plugin.config().msg("usage"));
        if (sender.hasPermission("pumpkinfly.admin")) sender.sendMessage(plugin.config().msg("admin-usage"));
    }

    private static OfflinePlayer resolve(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online;
        OfflinePlayer off = Bukkit.getOfflinePlayer(name);
        return off.hasPlayedBefore() || off.isOnline() ? off : null;
    }

    private String money(double v) {
        return plugin.economy().format(v);
    }

    private static double round2(double v) {
        return Math.round(v * 100D) / 100D;
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    public static String duration(long seconds) {
        if (seconds <= 0L) return "0 秒";
        long h = seconds / 3600L;
        long m = (seconds % 3600L) / 60L;
        long s = seconds % 60L;
        StringBuilder sb = new StringBuilder();
        if (h > 0) sb.append(h).append(" 小时 ");
        if (m > 0) sb.append(m).append(" 分 ");
        if (s > 0 || sb.length() == 0) sb.append(s).append(" 秒");
        return sb.toString().trim();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> base = Arrays.asList("buy", "on", "off", "toggle", "status", "records", "help");
        if (sender.hasPermission("pumpkinfly.admin")) {
            List<String> all = new ArrayList<>(base);
            all.addAll(Arrays.asList("give", "take", "setperm", "reload"));
            base = all;
        }
        if (args.length == 1) return filtered(base, args[0]);
        if (args[0].equalsIgnoreCase("buy") && args.length == 2) {
            return filtered(Arrays.asList("1", "5", "10", "30", "60", "perm"), args[1]);
        }
        if ((args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("take")
                || args[0].equalsIgnoreCase("setperm") || args[0].equalsIgnoreCase("status")
                || args[0].equalsIgnoreCase("records")) && args.length == 2) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) names.add(p.getName());
            return filtered(names, args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("setperm")) {
            return filtered(Arrays.asList("true", "false"), args[2]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("records")) {
            return filtered(Arrays.asList("buy", "session"), args[2]);
        }
        return Collections.emptyList();
    }

    private static List<String> filtered(List<String> src, String prefix) {
        List<String> out = new ArrayList<>();
        String p = prefix.toLowerCase();
        for (String s : src) if (s.toLowerCase().startsWith(p)) out.add(s);
        return out;
    }
}
