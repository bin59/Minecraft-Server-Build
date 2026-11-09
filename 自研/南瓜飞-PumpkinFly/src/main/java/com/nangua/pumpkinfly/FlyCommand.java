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
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * /pfly 主命令：购买、奖励领取、限时体验、全局/单人飞行管理、开关、状态与记录。
 *
 * <p>时长统一由 {@link TimeUtil} 解析：{@code 30m / 2h / 3d / 1d2h30m}，
 * 纯数字沿用旧的「分钟」口径，保证老命令与老 QuickMenu 按钮继续可用。
 */
public final class FlyCommand implements TabExecutor {

    private final PumpkinFlyPlugin plugin;

    public FlyCommand(PumpkinFlyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "status" : args[0].toLowerCase(Locale.ROOT);

        if (sub.equals("help")) { help(sender); return true; }
        if (sub.equals("reload")) {
            if (!requireAdmin(sender)) return true;
            plugin.reloadAll();
            sender.sendMessage(plugin.config().msg("reloaded"));
            return true;
        }
        if (sub.equals("buy")) return buy(sender, args);
        if (sub.equals("claim")) return claim(sender);
        if (sub.equals("trial")) return trial(sender, args);
        if (sub.equals("global")) return global(sender, args);
        if (sub.equals("player")) return player(sender, args);
        if (sub.equals("give") || sub.equals("take") || sub.equals("setperm")) return adminOp(sender, sub, args);
        if (sub.equals("status")) return status(sender, args);
        if (sub.equals("records")) return records(sender, args);
        if (sub.equals("on") || sub.equals("off") || sub.equals("toggle")) return toggle(sender, sub);
        if (sub.equals("hud")) return hud(sender, args);

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
        long minutes = 0L;
        if (!perm) {
            long seconds = TimeUtil.parseSeconds(args[1]);
            if (seconds < 0L) {
                p.sendMessage(plugin.config().msg("invalid-duration"));
                return true;
            }
            minutes = (seconds + 59L) / 60L;
            if (minutes < 1L || minutes > plugin.config().maxMinutesPerBuy) {
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

    // ------------------------------------------------------------------ 领取限时体验奖励

    /** /pfly claim：领取 rewards.claim-trial 配置的体验，ONCE / DAILY / ALWAYS 三种模式。 */
    private boolean claim(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.config().msg("player-only"));
            return true;
        }
        Player p = (Player) sender;
        if (!p.hasPermission("pumpkinfly.use")) {
            p.sendMessage(plugin.config().msg("no-permission"));
            return true;
        }
        long seconds = TimeUtil.parseSeconds(plugin.config().claimTrial);
        if (seconds <= 0L) {
            p.sendMessage(plugin.config().msg("claim-disabled"));
            return true;
        }
        long capped = capTrial(seconds);
        PlayerData d = plugin.data().get(p.getUniqueId(), p.getName());
        String today = TimeUtil.today();
        String mode = plugin.config().claimMode;
        if (("ONCE".equals(mode) && !d.claimDate.isEmpty())
                || ("DAILY".equals(mode) && today.equals(d.claimDate))) {
            p.sendMessage(plugin.config().msg("claim-used", "mode", mode));
            return true;
        }
        d.claimDate = today;
        plugin.data().markDirty();
        plugin.fly().grantTrial(p.getUniqueId(), d.name, capped, p.getName(), "CLAIM",
                "领取体验奖励 " + TimeUtil.format(capped));
        p.sendMessage(plugin.config().msg("claim-ok",
                "duration", TimeUtil.format(capped), "mode", mode));
        return true;
    }

    // ------------------------------------------------------------------ 限时体验发放（管理员）

    /** /pfly trial <玩家|online|all> <时长|clear|status> */
    private boolean trial(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) return true;
        if (args.length < 3) {
            sender.sendMessage(plugin.config().msg("trial-usage"));
            return true;
        }
        boolean all = args[1].equalsIgnoreCase("all") || args[1].equalsIgnoreCase("allplayers");
        boolean onlineOnly = args[1].equalsIgnoreCase("online");
        String action = args[2].toLowerCase(Locale.ROOT);

        OfflinePlayer off = null;
        UUID uuid = null;
        String name = args[1];
        if (!all && !onlineOnly) {
            off = resolve(args[1]);
            if (off == null) {
                sender.sendMessage(plugin.config().msg("player-not-found"));
                return true;
            }
            uuid = off.getUniqueId();
            name = off.getName() == null ? args[1] : off.getName();
        }

        if (action.equals("status")) {
            if (all || onlineOnly) {
                sender.sendMessage(plugin.config().msg("trial-batch-info",
                        "count", String.valueOf(onlineOnly
                                ? Bukkit.getOnlinePlayers().size() : plugin.data().all().size())));
                return true;
            }
            PlayerData d = plugin.data().get(uuid, name);
            long now = System.currentTimeMillis();
            if (d.trialActive(now)) {
                sender.sendMessage(plugin.config().msg("trial-info", "player", name,
                        "remaining", TimeUtil.format(d.trialRemainingSeconds(now)),
                        "expire", TimeUtil.clock(d.trialExpireAt),
                        "by", d.trialGrantedBy));
            } else {
                sender.sendMessage(plugin.config().msg("trial-none", "player", name));
            }
            return true;
        }

        if (action.equals("clear")) {
            int count = 0;
            if (all) {
                for (Map.Entry<UUID, PlayerData> e : plugin.data().all().entrySet()) {
                    plugin.fly().clearTrial(e.getKey(), e.getValue().name, sender.getName(), false);
                    count++;
                }
            } else if (onlineOnly) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    plugin.fly().clearTrial(p.getUniqueId(), p.getName(), sender.getName(), false);
                    count++;
                }
            } else {
                plugin.fly().clearTrial(uuid, name, sender.getName(), false);
                count = 1;
            }
            plugin.data().save();
            sender.sendMessage(plugin.config().msg("trial-cleared", "count", String.valueOf(count)));
            return true;
        }

        long seconds = TimeUtil.parseSeconds(action);
        if (seconds < 0L) {
            sender.sendMessage(plugin.config().msg("invalid-duration"));
            return true;
        }
        long capped = capTrial(seconds);
        String detail = "管理员发放限时体验 " + TimeUtil.format(capped);
        int count = 0;
        if (all) {
            for (Map.Entry<UUID, PlayerData> e : plugin.data().all().entrySet()) {
                plugin.fly().grantTrial(e.getKey(), e.getValue().name, capped,
                        sender.getName(), "TRIAL", detail, false);
                count++;
            }
        } else if (onlineOnly) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                plugin.fly().grantTrial(p.getUniqueId(), p.getName(), capped,
                        sender.getName(), "TRIAL_ONLINE", detail, false);
                count++;
            }
        } else {
            plugin.fly().grantTrial(uuid, name, capped, sender.getName(), "TRIAL", detail, false);
            count = 1;
        }
        plugin.data().save();
        sender.sendMessage(plugin.config().msg("trial-grant-admin", "player", name,
                "duration", TimeUtil.format(capped), "count", String.valueOf(count)));
        return true;
    }

    // ------------------------------------------------------------------ 全局飞行管理

    /** /pfly global <on|off|toggle|status|free <on|off|toggle>|trial <时长|clear>> */
    private boolean global(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) return true;
        if (args.length < 2) {
            sender.sendMessage(plugin.config().msg("global-usage"));
            return true;
        }
        GlobalState g = plugin.global();
        String op = args[1].toLowerCase(Locale.ROOT);
        long now = System.currentTimeMillis();
        switch (op) {
            case "status":
                sender.sendMessage(plugin.config().msg("global-status-title"));
                line(sender, "全服总开关", g.enabled ? "&a开启" : "&c关闭（所有人已落地）");
                line(sender, "全服免费飞行", g.freeFly ? "&a开启（无需凭证）" : "&7关闭");
                line(sender, "全服限时体验", g.trialActive(now)
                        ? "&a" + TimeUtil.format(g.trialRemainingSeconds(now))
                        + " &8(到期 " + TimeUtil.clock(g.trialExpireAt) + ")"
                        : "&7未开启");
                line(sender, "体验发放者", g.trialGrantedBy.isEmpty() ? "无" : g.trialGrantedBy);
                line(sender, "区域规则", plugin.config().zoneSummary());
                return true;

            case "on":
            case "off":
            case "toggle": {
                boolean value = op.equals("on") ? true : (op.equals("off") ? false : !g.enabled);
                g.enabled = value;
                g.save();
                if (!value) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (!FlyManager.isExempt(p) && !p.hasPermission("pumpkinfly.bypass")) {
                            plugin.fly().disableFlight(p, "global-off", true);
                        }
                    }
                    sender.sendMessage(plugin.config().msg("global-off"));
                } else {
                    for (Player p : Bukkit.getOnlinePlayers()) plugin.fly().checkNow(p, false);
                    sender.sendMessage(plugin.config().msg("global-on"));
                }
                return true;
            }

            case "free": {
                String v = args.length >= 3 ? args[2].toLowerCase(Locale.ROOT) : "toggle";
                boolean value = v.equals("on") ? true : (v.equals("off") ? false : !g.freeFly);
                g.freeFly = value;
                g.save();
                String text = plugin.config().msg(value ? "global-free-on" : "global-free-off");
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendMessage(text);
                    plugin.fly().checkNow(p, false);
                }
                // 在线管理员已在广播里收到，控制台才需要单独回执
                if (!(sender instanceof Player)) sender.sendMessage(text);
                return true;
            }

            case "trial": {
                if (args.length < 3) {
                    sender.sendMessage(plugin.config().msg("global-usage"));
                    return true;
                }
                if (args[2].equalsIgnoreCase("clear")) {
                    plugin.fly().clearGlobalTrial(sender.getName());
                    sender.sendMessage(plugin.config().msg("global-trial-clear"));
                    return true;
                }
                long seconds = TimeUtil.parseSeconds(args[2]);
                if (seconds < 0L) {
                    sender.sendMessage(plugin.config().msg("invalid-duration"));
                    return true;
                }
                plugin.fly().grantGlobalTrial(capTrial(seconds), sender.getName());
                return true;
            }

            default:
                sender.sendMessage(plugin.config().msg("global-usage"));
                return true;
        }
    }

    // ------------------------------------------------------------------ 单人飞行管理

    /** /pfly player <玩家> <status|on|off|ban|unban|trial <时长>|cleartrial|give|take|setperm> */
    private boolean player(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) return true;
        if (args.length < 3) {
            sender.sendMessage(plugin.config().msg("player-usage"));
            return true;
        }
        OfflinePlayer off = resolve(args[1]);
        if (off == null) {
            sender.sendMessage(plugin.config().msg("player-not-found"));
            return true;
        }
        UUID uuid = off.getUniqueId();
        String name = off.getName() == null ? args[1] : off.getName();
        String op = args[2].toLowerCase(Locale.ROOT);
        Player online = off.getPlayer();
        PlayerData d = plugin.data().get(uuid, name);

        switch (op) {
            case "status":
                return status(sender, new String[]{"status", name});

            case "on":
            case "off": {
                boolean offState = op.equals("off");
                if (online != null && online.isOnline()) {
                    plugin.fly().setManualOff(online, offState);
                } else {
                    d.manualOff = offState;
                    plugin.data().markDirty();
                    plugin.data().save();
                }
                sender.sendMessage(plugin.config().msg("player-set", "player", name,
                        "value", offState ? "关闭" : "开启"));
                return true;
            }

            case "ban": {
                d.banned = true;
                plugin.data().markDirty();
                if (online != null && online.isOnline()) {
                    plugin.fly().disableFlight(online, "banned", true);
                } else {
                    plugin.data().save();
                }
                sender.sendMessage(plugin.config().msg("player-banned", "player", name));
                return true;
            }

            case "unban": {
                d.banned = false;
                plugin.data().markDirty();
                if (online != null && online.isOnline()) plugin.fly().checkNow(online, false);
                plugin.data().save();
                sender.sendMessage(plugin.config().msg("player-unbanned", "player", name));
                return true;
            }

            case "trial": {
                if (args.length < 4) {
                    sender.sendMessage(plugin.config().msg("player-usage"));
                    return true;
                }
                long seconds = TimeUtil.parseSeconds(args[3]);
                if (seconds < 0L) {
                    sender.sendMessage(plugin.config().msg("invalid-duration"));
                    return true;
                }
                long capped = capTrial(seconds);
                plugin.fly().grantTrial(uuid, name, capped, sender.getName(), "TRIAL",
                        "单人管理发放 " + TimeUtil.format(capped));
                sender.sendMessage(plugin.config().msg("trial-grant-admin", "player", name,
                        "duration", TimeUtil.format(capped), "count", "1"));
                return true;
            }

            case "cleartrial":
                plugin.fly().clearTrial(uuid, name, sender.getName());
                sender.sendMessage(plugin.config().msg("trial-cleared", "count", "1"));
                return true;

            case "give":
            case "take":
            case "setperm": {
                if (args.length < 4) {
                    sender.sendMessage(plugin.config().msg("player-usage"));
                    return true;
                }
                return adminOp(sender, op, new String[]{op, name, args[3]});
            }

            default:
                sender.sendMessage(plugin.config().msg("player-usage"));
                return true;
        }
    }

    // ------------------------------------------------------------------ 旧管理员命令（兼容）

    private boolean adminOp(CommandSender sender, String sub, String[] args) {
        if (!requireAdmin(sender)) return true;
        if (args.length < 3) {
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
            long minutes = 0L;
            if (!perm) {
                long seconds = TimeUtil.parseSeconds(args[2]);
                if (seconds < 0L) {
                    sender.sendMessage(plugin.config().msg("invalid-duration"));
                    return true;
                }
                minutes = (seconds + 59L) / 60L;
                if (minutes < 1L) minutes = 1L;
            }
            applyGrant(null, d, perm, minutes, false);
            writePurchase(uuid, d.name, perm ? "GIVE_PERM" : "GIVE", minutes,
                    0D, 0D, 0D, perm ? 0L : d.remainingSeconds, sender.getName(),
                    perm ? "管理员发放永久" : "管理员发放 " + minutes + " 分钟");
            if (perm) sender.sendMessage(plugin.config().msg("give-perm", "player", name));
            else sender.sendMessage(plugin.config().msg("give-time", "player", name,
                    "minutes", String.valueOf(minutes), "remaining", duration(d.remainingSeconds)));
        } else {
            long seconds = TimeUtil.parseSeconds(args[2]);
            if (seconds < 0L) {
                sender.sendMessage(plugin.config().msg("invalid-duration"));
                return true;
            }
            long minutes = (seconds + 59L) / 60L;
            if (minutes < 1L) minutes = 1L;
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
    private void applyGrant(Player p, PlayerData d, boolean perm, long minutes, boolean charge) {
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

    private void writePurchase(UUID uuid, String name, String type, long minutes,
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

    private long capTrial(long seconds) {
        return Math.min(seconds, (long) plugin.config().maxTrialDays * 86400L);
    }

    private void line(CommandSender sender, String label, String value) {
        sender.sendMessage(plugin.config().msg("status-line", "label", label, "value", value));
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

    /** /pfly hud [on|off|toggle]：单独开关自己的飞行界面显示（不影响飞行能力）。 */
    private boolean hud(CommandSender sender, String[] args) {
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
        String opt = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "toggle";
        if (opt.equals("on")) d.hudEnabled = true;
        else if (opt.equals("off")) d.hudEnabled = false;
        else d.hudEnabled = !d.hudEnabled;
        plugin.data().markDirty();
        plugin.data().save();
        if (!d.hudEnabled) plugin.fly().display().clear(p);
        p.sendMessage(plugin.config().msg(d.hudEnabled ? "hud-on" : "hud-off"));
        return true;
    }

    /** /pfly status [玩家]：管理员可查离线玩家账户；普通玩家只能查自己。 */
    private boolean status(CommandSender sender, String[] args) {
        OfflinePlayer off;
        if (args.length >= 2) {
            if (!requireAdmin(sender)) return true;
            off = resolve(args[1]);
            if (off == null) {
                sender.sendMessage(plugin.config().msg("player-not-found"));
                return true;
            }
        } else if (sender instanceof Player) {
            off = (Player) sender;
        } else {
            sender.sendMessage(plugin.config().msg("admin-usage"));
            return true;
        }
        if (!sender.equals(off) && !requireAdmin(sender)) return true;

        UUID uuid = off.getUniqueId();
        String name = off.getName() == null ? (args.length >= 2 ? args[1] : sender.getName()) : off.getName();
        Player target = off.isOnline() ? off.getPlayer() : null;
        PlayerData d = plugin.data().get(uuid, name);
        FlyConfig c = plugin.config();
        long now = System.currentTimeMillis();
        GlobalState g = plugin.global();

        sender.sendMessage(c.msg("status-title", "player", name));
        line(sender, "凭证来源", plugin.fly().passLabel(d, now));
        long effective = plugin.fly().effectiveRemainingSeconds(d, now);
        line(sender, "剩余时长", effective < 0L ? "不消耗" : duration(Math.max(0L, effective)));
        line(sender, "限时体验", d.trialActive(now)
                ? "&a" + duration(d.trialRemainingSeconds(now)) + " &8(到期 "
                + TimeUtil.clock(d.trialExpireAt) + "，发放人 " + d.trialGrantedBy + ")"
                : "&7无");
        line(sender, "全服状态", (g.enabled ? "&a开启" : "&c已关闭")
                + (g.freeFly ? " &8| &a免费飞行" : "")
                + (g.trialActive(now) ? " &8| &a全服体验 " + duration(g.trialRemainingSeconds(now)) : ""));
        line(sender, "时长余额", duration(d.remainingSeconds));
        line(sender, "累计购买 / 已飞",
                duration(d.totalPurchasedSeconds) + " / " + duration(d.totalUsedSeconds));
        line(sender, "本次已飞", target == null ? "离线"
                : duration(plugin.fly().currentSessionSeconds(uuid)));
        if (target != null) {
            ZoneChecker.Result zone = plugin.zones().check(target);
            line(sender, "当前区域", zone.label);
            sender.sendMessage(c.msg(zone.allowed ? "zone-ok" : "zone-deny", "zone", zone.label));
            String state;
            if (FlyManager.isExempt(target)) state = "&7创造/旁观模式，插件不干预";
            else if (target.isFlying()) state = "&a飞行中";
            else if (target.getAllowFlight()) state = "&e已开飞，等待双击空格";
            else state = "&c未开启" + (d.manualOff ? "（手动关闭）" : "");
            line(sender, "飞行状态", state);
        } else {
            line(sender, "当前区域", "离线");
            line(sender, "飞行状态", "&7离线");
        }
        line(sender, "手动开关 / 禁用", (d.manualOff ? "&e手动关闭" : "&a正常")
                + (d.banned ? " &8| &c管理员已禁用" : ""));
        line(sender, "计时口径", c.isRealTime() ? "现实时间倒计时" : "仅飞行中扣秒");
        line(sender, "界面显示", d.hudEnabled ? "&a开启 &8(/pfly hud 可关闭)" : "&c已关闭");
        line(sender, "允许范围", c.zoneSummary());
        if (!d.firstPurchase.isEmpty()) {
            line(sender, "首次购买", d.firstPurchase);
            line(sender, "最近活跃", d.lastActive);
        }
        return true;
    }

    /**
     * /pfly records [玩家|类型] [类型]：
     *  - 第二参是记录类型关键字（buy/购买/b/session/飞行/s）时，视为「查看自己的记录」，仅需 pumpkinfly.records 权限；
     *  - 第二参是玩家名时，视为「查看他人记录」，需要 pumpkinfly.admin；
     *  - 第三参为类型（仅查他人时有效），默认 buy。
     * 这样 QuickMenu 的 [player] pfly records buy / session 按钮才能正确自查，
     * 不会把 buy / session 当成玩家名去解析（旧逻辑会刷 Couldn't find profile 报错）。
     */
    private boolean records(CommandSender sender, String[] args) {
        if (!sender.hasPermission("pumpkinfly.records") && !sender.hasPermission("pumpkinfly.admin")) {
            sender.sendMessage(plugin.config().msg("no-permission"));
            return true;
        }
        final String[] KINDS = {"buy", "购买", "b", "session", "飞行", "s"};
        String kindKeyword = null;
        if (args.length >= 2) {
            String a = args[1].toLowerCase(Locale.ROOT);
            for (String k : KINDS) if (k.equals(a)) { kindKeyword = k; break; }
        }
        UUID uuid;
        String name;
        String kind;
        if (kindKeyword != null) {
            // 自查记录：第二参是类型关键字，仅需 records 权限，不要求 admin
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.config().msg("admin-usage"));
                return true;
            }
            uuid = ((Player) sender).getUniqueId();
            name = sender.getName();
            kind = kindKeyword;
        } else if (args.length >= 2) {
            // 查他人：第二参是玩家名，需要 admin
            if (!requireAdmin(sender)) return true;
            OfflinePlayer off = resolve(args[1]);
            if (off == null) {
                sender.sendMessage(plugin.config().msg("player-not-found"));
                return true;
            }
            uuid = off.getUniqueId();
            name = off.getName() == null ? args[1] : off.getName();
            kind = args.length >= 3 ? args[2].toLowerCase(Locale.ROOT) : "buy";
        } else if (sender instanceof Player) {
            // 无参：查自己的购买记录
            uuid = ((Player) sender).getUniqueId();
            name = sender.getName();
            kind = "buy";
        } else {
            sender.sendMessage(plugin.config().msg("admin-usage"));
            return true;
        }
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
        sender.sendMessage(plugin.config().msg("usage-reward"));
        if (sender.hasPermission("pumpkinfly.admin")) {
            sender.sendMessage(plugin.config().msg("admin-usage"));
            sender.sendMessage(plugin.config().msg("global-usage"));
            sender.sendMessage(plugin.config().msg("trial-usage"));
            sender.sendMessage(plugin.config().msg("player-usage"));
        }
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

    /** 兼容旧调用：统一交给 TimeUtil，输出「x 天 x 小时 x 分 x 秒」。 */
    public static String duration(long seconds) {
        return TimeUtil.format(seconds);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> base = new ArrayList<>(Arrays.asList(
                "buy", "claim", "on", "off", "toggle", "hud", "status", "records", "help"));
        if (sender.hasPermission("pumpkinfly.admin")) {
            base.addAll(Arrays.asList("give", "take", "setperm", "trial", "global", "player", "reload"));
        }
        if (args.length == 1) return filtered(base, args[0]);

        if (args[0].equalsIgnoreCase("buy") && args.length == 2) {
            return filtered(Arrays.asList("1", "5", "10", "30", "60", "1h", "1d", "perm"), args[1]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("take")
                || args[0].equalsIgnoreCase("setperm") || args[0].equalsIgnoreCase("status")
                || args[0].equalsIgnoreCase("records") || args[0].equalsIgnoreCase("player"))) {
            return filtered(onlineNames(), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("trial")) {
            List<String> out = new ArrayList<>(onlineNames());
            out.addAll(Arrays.asList("online", "all"));
            return filtered(out, args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("global")) {
            return filtered(Arrays.asList("on", "off", "toggle", "status", "free", "trial"), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("hud")) {
            return filtered(Arrays.asList("on", "off", "toggle"), args[1]);
        }

        if (args.length == 3) {
            String a0 = args[0].toLowerCase(Locale.ROOT);
            if (a0.equals("setperm")) return filtered(Arrays.asList("true", "false"), args[2]);
            if (a0.equals("records")) return filtered(Arrays.asList("buy", "session"), args[2]);
            if (a0.equals("give") || a0.equals("take")) {
                return filtered(Arrays.asList("30m", "1h", "2h", "1d", "perm"), args[2]);
            }
            if (a0.equals("trial")) {
                return filtered(Arrays.asList("30m", "1h", "2h", "1d", "7d", "clear", "status"), args[2]);
            }
            if (a0.equals("global")) {
                if (args[1].equalsIgnoreCase("free")) return filtered(Arrays.asList("on", "off", "toggle"), args[2]);
                if (args[1].equalsIgnoreCase("trial")) {
                    return filtered(Arrays.asList("30m", "1h", "2h", "1d", "clear"), args[2]);
                }
            }
            if (a0.equals("player")) {
                return filtered(Arrays.asList("status", "on", "off", "ban", "unban",
                        "trial", "cleartrial", "give", "take", "setperm"), args[2]);
            }
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("player")) {
            String a2 = args[2].toLowerCase(Locale.ROOT);
            if (a2.equals("trial") || a2.equals("give") || a2.equals("take")) {
                return filtered(Arrays.asList("30m", "1h", "2h", "1d", "perm"), args[3]);
            }
            if (a2.equals("setperm")) return filtered(Arrays.asList("true", "false"), args[3]);
        }
        return Collections.emptyList();
    }

    private static List<String> onlineNames() {
        List<String> names = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) names.add(p.getName());
        return names;
    }

    private static List<String> filtered(List<String> src, String prefix) {
        List<String> out = new ArrayList<>();
        String p = prefix.toLowerCase(Locale.ROOT);
        for (String s : src) if (s.toLowerCase(Locale.ROOT).startsWith(p)) out.add(s);
        return out;
    }
}
