package com.nangua.pumpkinfly;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** 飞行主循环：凭证 → 区域 → 开关 allowFlight → 扣秒 → 会话记录 → 安全落地。 */
public final class FlyManager {

    /** 一次飞行会话（从真正起飞到落地/关闭）。 */
    private static final class Session {
        final String start;
        final String zone;
        final String world;
        final String xyz;
        long usedSeconds;
        Session(String start, String zone, String world, String xyz) {
            this.start = start; this.zone = zone; this.world = world; this.xyz = xyz;
        }
    }

    private static final Map<String, String> REASON_TEXT = new HashMap<>();
    static {
        REASON_TEXT.put("LEFT_ZONE", "离开允许区域");
        REASON_TEXT.put("EXPIRED", "时长用完");
        REASON_TEXT.put("MANUAL_OFF", "手动关闭");
        REASON_TEXT.put("LOGOUT", "退出游戏");
        REASON_TEXT.put("LANDED", "主动落地");
        REASON_TEXT.put("WORLD_CHANGE", "切换世界");
        REASON_TEXT.put("GAMEMODE", "切换游戏模式");
        REASON_TEXT.put("NO_PASS", "凭证失效");
        REASON_TEXT.put("GLOBAL_OFF", "全服飞行关闭");
        REASON_TEXT.put("BANNED", "管理员禁用");
        REASON_TEXT.put("TRIAL_EXPIRED", "体验到期");
        REASON_TEXT.put("RELOAD", "插件重载");
        REASON_TEXT.put("IDLE", "未起飞");
    }

    private final PumpkinFlyPlugin plugin;
    private final FlyDisplay display;
    private final Map<UUID, Session> active = new HashMap<>();
    private final Map<UUID, Long> noFallUntil = new HashMap<>();
    private final Map<UUID, Long> lastZoneNotify = new HashMap<>();
    private final Map<UUID, Long> lastMoveCheck = new HashMap<>();
    private final Map<UUID, Set<Integer>> warned = new HashMap<>();

    public FlyManager(PumpkinFlyPlugin plugin) {
        this.plugin = plugin;
        this.display = new FlyDisplay(plugin);
    }

    /** 主循环，每秒由 BukkitRunnable 在主线程调用。 */
    public void tick() {
        long now = System.currentTimeMillis();
        if (plugin.global().checkExpiry(now)) plugin.global().save();
        long step = plugin.config().tickSeconds;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p == null || !p.isOnline()) continue;
            UUID uuid = p.getUniqueId();

            if (isExempt(p)) {
                closeSession(p, "GAMEMODE", false);
                display.clear(p);
                continue;
            }
            if (p.hasPermission("pumpkinfly.bypass")) {
                display.clear(p);
                continue;
            }

            PlayerData d = plugin.data().get(uuid, p.getName());

            boolean consume = consumesBalance(d, now);

            // REAL_TIME：按现实时间同步剩余秒数，离线也走；体验/免费期间冻结到期时间
            if (plugin.config().isRealTime() && d.expireAt > 0L) {
                if (consume) {
                    long left = Math.max(0L, (d.expireAt - now + 999L) / 1000L);
                    if (left != d.remainingSeconds) {
                        d.remainingSeconds = left;
                        plugin.data().markDirty();
                    }
                } else if (d.expireAt <= now && d.remainingSeconds > 0L) {
                    // 体验结束恢复扣费时，按剩余余额重算到期时间，避免余额被一次性清零
                    d.expireAt = now + d.remainingSeconds * 1000L;
                    plugin.data().markDirty();
                }
            }

            ZoneChecker.Result zone = plugin.zones().check(p);
            boolean pass = hasFlightPass(p, d, now);
            boolean allowed = pass && zone.allowed && !d.manualOff;
            boolean flying = p.isFlying();

            if (allowed) {
                if (!p.getAllowFlight()) p.setAllowFlight(true);

                if (flying) {
                    Session s = openSession(p, zone.label);
                    s.usedSeconds += step;
                    // 已飞时长与「是否扣余额」无关：永久 / 体验 / REAL_TIME 也要累计
                    d.totalUsedSeconds += step;
                    d.lastActive = PlayerData.now();
                    plugin.data().markDirty();

                    // 优先消耗「体验秒数池」（飞才扣）；体验用完且仍有余额则接余额，否则停飞
                    if (d.trialActive(now)) {
                        d.trialRemaining -= step;
                        if (d.trialRemaining <= 0L) {
                            d.trialRemaining = 0L;
                            d.trialTotalSeconds = 0L;
                            d.trialExpireAt = 0L;
                            d.trialGrantedAt = "";
                            d.trialGrantedBy = "";
                            plugin.data().markDirty();
                            p.sendMessage(plugin.config().msg("trial-expired"));
                            warned.remove(uuid);
                            if (!d.permanent && d.remainingSeconds <= 0L) {
                                disableFlight(p, "expired", true);
                                continue;
                            }
                        }
                    } else if (consume && !plugin.config().isRealTime()) {
                        d.remainingSeconds -= step;
                    }

                    long left = effectiveRemainingSeconds(d, now);
                    if (left <= 0L) {
                        d.remainingSeconds = 0L;
                        disableFlight(p, "expired", true);
                        continue;
                    }
                    warnIfNeeded(p, left);
                } else {
                    // 落地即写盘，sessions.yml 才会留下本次飞行的开始/结束/秒数
                    closeSession(p, "LANDED", true);
                }
            } else {
                String reason = blockReason(d, pass, zone.allowed);
                boolean notify = reason.equals("left-zone") || reason.equals("global-off")
                        || reason.equals("banned");
                if (p.getAllowFlight() || flying) {
                    disableFlight(p, reason, notify);
                } else {
                    closeSession(p, "IDLE", false);
                }
            }
            // 界面显示只看「是否有有效飞行凭证」，不卡区域、也不卡是否在飞：
            // 这样玩家在地面 / 暂时离开飞行区时也能看到体验剩余，方便了解还有多少可飞时间
            // （体验为「飞行秒数池」，不飞不流逝、离线也不消耗，只有真正飞行时才扣减）。
            boolean showHud = d.hudEnabled && !d.manualOff && hasFlightPass(p, d, now);
            display.update(p, d, showHud, flying, zone.label, currentSessionSeconds(uuid), now);
        }
    }

    /** 移动时的即时判定（带 250ms 节流），避免飞出领地后还要等 1 秒。 */
    public void checkNow(Player p, boolean fromMove) {
        if (p == null || !p.isOnline() || isExempt(p) || p.hasPermission("pumpkinfly.bypass")) return;
        long now = System.currentTimeMillis();
        if (fromMove) {
            Long last = lastMoveCheck.get(p.getUniqueId());
            if (last != null && now - last < 250L) return;
            lastMoveCheck.put(p.getUniqueId(), now);
        }
        PlayerData d = plugin.data().get(p.getUniqueId(), p.getName());
        ZoneChecker.Result zone = plugin.zones().check(p);
        boolean pass = hasFlightPass(p, d, now);
        boolean allowed = pass && zone.allowed && !d.manualOff;
        if (!allowed && (p.getAllowFlight() || p.isFlying())) {
            String reason = blockReason(d, pass, zone.allowed);
            boolean notify = reason.equals("left-zone") ? notifyThrottled(p)
                    : reason.equals("global-off") || reason.equals("banned");
            disableFlight(p, reason, notify);
        } else if (allowed && !p.getAllowFlight()) {
            p.setAllowFlight(true);
        }
    }

    // ------------------------------------------------------------------ 凭证判定

    /**
     * 统一凭证判定，tick / checkNow / Essentials 门禁共用，避免三处口径不一致。
     * 优先级：全服总开关 → 个人封禁 → 全服免费 / 全服体验 → 个人（永久 > 体验 > 余额）。
     */
    public boolean hasFlightPass(Player p, PlayerData d, long now) {
        GlobalState g = plugin.global();
        if (!g.enabled) return false;
        if (d == null || d.banned) return false;
        if (g.freeFly || g.trialActive(now)) return true;
        return d.hasPass(now);
    }

    /** 是否消耗余额：永久 / 个人体验 / 全服体验 / 全服免费都不扣。 */
    public boolean consumesBalance(PlayerData d, long now) {
        GlobalState g = plugin.global();
        if (d.permanent || d.trialActive(now) || g.freeFly || g.trialActive(now)) return false;
        return true;
    }

    /** 界面与提醒用的「有效剩余秒数」；-1 表示不消耗（永久 / 全服免费）。 */
    public long effectiveRemainingSeconds(PlayerData d, long now) {
        GlobalState g = plugin.global();
        if (d.permanent || g.freeFly) return -1L;
        if (g.trialActive(now)) return g.trialRemainingSeconds(now);
        if (d.trialActive(now)) return d.trialRemaining;
        return d.remainingSeconds;
    }

    /** status 用的凭证来源文字。 */
    public String passLabel(PlayerData d, long now) {
        GlobalState g = plugin.global();
        if (!g.enabled) return "&c全服飞行已关闭";
        if (d.banned) return "&c已被管理员禁用";
        if (d.permanent) return "&6永久飞行";
        if (g.freeFly) return "&a全服免费飞行";
        if (g.trialActive(now))
            return "&a全服体验 &7(" + TimeUtil.format(g.trialRemainingSeconds(now)) + ")";
        if (d.trialActive(now))
            return "&a限时体验 &7(" + TimeUtil.format(d.trialRemainingSeconds(now)) + ")";
        return d.remainingSeconds > 0L ? "&a时长余额" : "&c无";
    }

    private String blockReason(PlayerData d, boolean pass, boolean zoneAllowed) {
        if (!plugin.global().enabled) return "global-off";
        if (d.banned) return "banned";
        if (!pass) return "no-pass";
        if (!zoneAllowed) return "left-zone";
        return "off";
    }

    private boolean notifyThrottled(Player p) {
        if (!plugin.config().notifyOnLeaveZone) return false;
        long now = System.currentTimeMillis();
        Long last = lastZoneNotify.get(p.getUniqueId());
        if (last != null && now - last < 5000L) return false;
        lastZoneNotify.put(p.getUniqueId(), now);
        return true;
    }

    private void warnIfNeeded(Player p, long remaining) {
        Set<Integer> sent = warned.computeIfAbsent(p.getUniqueId(), k -> new HashSet<>());
        if (remaining < 0L) { sent.clear(); return; }
        long maxWarn = 0L;
        for (int w : plugin.config().warnSeconds) maxWarn = Math.max(maxWarn, w);
        if (remaining > maxWarn) { sent.clear(); return; }
        for (int w : plugin.config().warnSeconds) {
            if (remaining == w && sent.add(w)) {
                p.sendMessage(plugin.config().msg("warn", "seconds", String.valueOf(remaining)));
            }
        }
    }

    // ------------------------------------------------------------------ 限时体验发放

    /** 给单个玩家发放限时体验（叠加到期时间，按现实时间走，离线也消耗）。 */
    public void grantTrial(UUID uuid, String name, long seconds,
                           String operator, String type, String detail) {
        grantTrial(uuid, name, seconds, operator, type, detail, true);
    }

    /** saveNow=false 用于批量发放：由调用方在循环结束后统一落盘，避免 N 次全量写文件。 */
    public void grantTrial(UUID uuid, String name, long seconds,
                           String operator, String type, String detail, boolean saveNow) {
        if (seconds <= 0L) return;
        long now = System.currentTimeMillis();
        PlayerData d = plugin.data().get(uuid, name);
        // 体验改为「飞行秒数池」：飞才扣，不飞不流逝（叠加发放）
        d.trialRemaining += seconds;
        d.trialTotalSeconds += seconds;
        d.trialExpireAt = 0L;
        d.trialGrantedAt = PlayerData.now();
        d.trialGrantedBy = operator;
        d.lastActive = PlayerData.now();
        plugin.data().markDirty();
        recordTrial(uuid, d.name, type, seconds, d.trialExpireAt, operator, detail);
        Player online = Bukkit.getPlayer(uuid);
        if (online != null && online.isOnline()) {
            online.sendMessage(plugin.config().msg("trial-grant",
                    "duration", TimeUtil.format(seconds),
                    "remaining", TimeUtil.format(d.trialRemainingSeconds(now))));
            checkNow(online, false);
        }
        if (saveNow) plugin.data().save();
    }

    /** 给全服发放限时体验（含之后上线的玩家，无需逐个发）。 */
    public void grantGlobalTrial(long seconds, String operator) {
        GlobalState g = plugin.global();
        long now = System.currentTimeMillis();
        g.trialExpireAt = Math.max(now, g.trialExpireAt) + seconds * 1000L;
        g.trialTotalSeconds = g.trialRemainingSeconds(now);
        g.trialGrantedBy = operator;
        g.save();
        recordTrial(null, "ALL", "GLOBAL_TRIAL", seconds, g.trialExpireAt, operator,
                "全服限时体验 " + TimeUtil.format(seconds));
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(plugin.config().msg("global-trial",
                    "duration", TimeUtil.format(seconds),
                    "remaining", TimeUtil.format(g.trialRemainingSeconds(now)),
                    "expire", TimeUtil.clock(g.trialExpireAt)));
            checkNow(p, false);
        }
        plugin.data().save();
    }

    /** 清除某玩家的限时体验。 */
    public void clearTrial(UUID uuid, String name, String operator) {
        clearTrial(uuid, name, operator, true);
    }

    public void clearTrial(UUID uuid, String name, String operator, boolean saveNow) {
        PlayerData d = plugin.data().get(uuid, name);
        d.trialExpireAt = 0L;
        d.trialTotalSeconds = 0L;
        d.trialRemaining = 0L;
        d.trialGrantedAt = "";
        d.trialGrantedBy = "";
        plugin.data().markDirty();
        recordTrial(uuid, d.name, "TRIAL_CLEAR", 0L, 0L, operator, "清除限时体验");
        Player online = Bukkit.getPlayer(uuid);
        if (online != null && online.isOnline()) {
            online.sendMessage(plugin.config().msg("trial-clear"));
            checkNow(online, false);
        }
        if (saveNow) plugin.data().save();
    }

    /** 清除全服限时体验。 */
    public void clearGlobalTrial(String operator) {
        GlobalState g = plugin.global();
        g.trialExpireAt = 0L;
        g.trialTotalSeconds = 0L;
        g.trialGrantedBy = "";
        g.save();
        recordTrial(null, "ALL", "GLOBAL_TRIAL_CLEAR", 0L, 0L, operator, "清除全服限时体验");
        for (Player p : Bukkit.getOnlinePlayers()) checkNow(p, false);
        plugin.data().save();
    }

    private void recordTrial(UUID uuid, String name, String type, long seconds,
                             long expireAt, String operator, String detail) {
        Map<String, Object> rec = new LinkedHashMap<>();
        rec.put("time", PlayerData.now());
        rec.put("uuid", uuid == null ? "ALL" : uuid.toString());
        rec.put("name", name);
        rec.put("type", type);
        rec.put("minutes", seconds / 60L);
        rec.put("seconds", seconds);
        rec.put("expire-at", TimeUtil.clock(expireAt));
        rec.put("cost", 0D);
        rec.put("balance-before", 0D);
        rec.put("balance-after", 0D);
        rec.put("remaining-after", 0L);
        rec.put("operator", operator);
        rec.put("detail", detail);
        plugin.data().addPurchase(rec);
        plugin.data().markDirty();
    }

    // ------------------------------------------------------------------ 开关 / 落地

    /** 供 Essentials FlyStatusChangeEvent 门禁调用：当前是否具备飞行资格。 */
    public boolean canFly(Player p) {
        if (p == null || isExempt(p) || p.hasPermission("pumpkinfly.bypass")) return true;
        if (!plugin.global().enabled) return false;
        PlayerData d = plugin.data().peek(p.getUniqueId());
        if (d == null || d.manualOff) return false;
        if (!hasFlightPass(p, d, System.currentTimeMillis())) return false;
        return plugin.zones().check(p).allowed;
    }

    public void setManualOff(Player p, boolean off) {
        PlayerData d = plugin.data().get(p.getUniqueId(), p.getName());
        d.manualOff = off;
        plugin.data().markDirty();
        if (off) {
            disableFlight(p, "off", false);
            p.sendMessage(plugin.config().msg("off"));
        } else {
            ZoneChecker.Result zone = plugin.zones().check(p);
            if (!hasFlightPass(p, d, System.currentTimeMillis())) {
                p.sendMessage(plugin.config().msg("no-pass"));
            } else if (!zone.allowed) {
                p.sendMessage(plugin.config().msg("fly-blocked", "zone", zone.label));
            } else {
                p.setAllowFlight(true);
                p.sendMessage(plugin.config().msg("on"));
            }
        }
        warned.remove(p.getUniqueId());
    }

    public void disableFlight(Player p, String messageKey, boolean notify) {
        closeSession(p, messageKeyToReason(messageKey), true);
        // 先停飞再找落脚点，避免 teleport 时仍处于飞行状态
        p.setFlying(false);
        safeLand(p);
        p.setAllowFlight(false);
        warned.remove(p.getUniqueId());
        display.clear(p);
        if (notify && messageKey != null && !messageKey.isEmpty()) {
            p.sendMessage(plugin.config().msg(messageKey));
        }
    }

    private static String messageKeyToReason(String key) {
        if ("left-zone".equals(key)) return "LEFT_ZONE";
        if ("expired".equals(key)) return "EXPIRED";
        if ("off".equals(key)) return "MANUAL_OFF";
        if ("no-pass".equals(key)) return "NO_PASS";
        if ("global-off".equals(key)) return "GLOBAL_OFF";
        if ("banned".equals(key)) return "BANNED";
        return "LANDED";
    }

    /** 向下找落脚点，失败则只清摔落累计；配合 noFall 免疫避免半空摔死。 */
    private void safeLand(Player p) {
        p.setFallDistance(0F);
        if (plugin.config().noFallSeconds > 0) {
            noFallUntil.put(p.getUniqueId(), System.currentTimeMillis() + plugin.config().noFallSeconds * 1000L);
        }
        if (!plugin.config().safeLand || p.isOnGround()) return;
        Location loc = p.getLocation();
        World w = loc.getWorld();
        if (w == null) return;
        for (int i = 1; i <= Math.max(1, plugin.config().searchDepth); i++) {
            Block b = w.getBlockAt(loc.getBlockX(), loc.getBlockY() - i, loc.getBlockZ());
            if (!b.getType().isSolid() || b.isLiquid()) continue;
            Block above = b.getRelative(0, 1, 0);
            Block head = b.getRelative(0, 2, 0);
            if (!above.isPassable() || !head.isPassable()) continue;
            Location target = new Location(w, loc.getX(), b.getY() + 1, loc.getZ(), loc.getYaw(), loc.getPitch());
            try {
                p.teleport(target, PlayerTeleportEvent.TeleportCause.PLUGIN);
            } catch (Throwable ignored) { }
            p.setFallDistance(0F);
            return;
        }
    }

    public boolean isNoFall(UUID uuid) {
        Long until = noFallUntil.get(uuid);
        if (until == null) return false;
        if (System.currentTimeMillis() > until) { noFallUntil.remove(uuid); return false; }
        return true;
    }

    public void clearNoFall(UUID uuid) { noFallUntil.remove(uuid); }

    private Session openSession(Player p, String zone) {
        UUID uuid = p.getUniqueId();
        Session s = active.get(uuid);
        if (s != null) return s;
        Location l = p.getLocation();
        s = new Session(PlayerData.now(), zone,
                l.getWorld() == null ? "" : l.getWorld().getName(),
                l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ());
        active.put(uuid, s);
        return s;
    }

    public void closeSession(Player p, String reason, boolean write) {
        if (p == null) return;
        Session s = active.remove(p.getUniqueId());
        if (s == null || !write || s.usedSeconds <= 0L) return;
        PlayerData d = plugin.data().peek(p.getUniqueId());
        // track-permanent-sessions: false 时只做账户累计，不产生会话记录
        if (d != null && d.permanent && !plugin.config().trackPermanentSessions) return;
        Map<String, Object> rec = new LinkedHashMap<>();
        rec.put("uuid", p.getUniqueId().toString());
        rec.put("name", d == null ? p.getName() : d.name);
        rec.put("start", s.start);
        rec.put("end", PlayerData.now());
        rec.put("used-seconds", s.usedSeconds);
        rec.put("end-reason", REASON_TEXT.getOrDefault(reason, reason));
        rec.put("start-zone", s.zone);
        rec.put("start-world", s.world);
        rec.put("start-xyz", s.xyz);
        plugin.data().addSession(rec);
        plugin.data().markDirty();
    }

    /** 本次飞行会话已累计秒数，供界面显示与 /pfly status 使用。 */
    public long currentSessionSeconds(UUID uuid) {
        Session s = active.get(uuid);
        return s == null ? 0L : s.usedSeconds;
    }

    public FlyDisplay display() {
        return display;
    }

    public void onQuit(Player p) {
        closeSession(p, "LOGOUT", true);
        display.clear(p);
        warned.remove(p.getUniqueId());
        lastMoveCheck.remove(p.getUniqueId());
        plugin.data().save();
    }

    public void onDisable() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            closeSession(p, "RELOAD", true);
            if (!isExempt(p)) {
                p.setFlying(false);
                p.setAllowFlight(false);
            }
        }
        display.clearAll();
        plugin.data().save();
    }

    public static boolean isExempt(Player p) {
        GameMode gm = p.getGameMode();
        return gm == GameMode.CREATIVE || gm == GameMode.SPECTATOR;
    }
}
