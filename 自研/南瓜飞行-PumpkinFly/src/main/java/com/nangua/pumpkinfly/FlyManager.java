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
        REASON_TEXT.put("RELOAD", "插件重载");
        REASON_TEXT.put("IDLE", "未起飞");
    }

    private final PumpkinFlyPlugin plugin;
    private final Map<UUID, Session> active = new HashMap<>();
    private final Map<UUID, Long> noFallUntil = new HashMap<>();
    private final Map<UUID, Long> lastZoneNotify = new HashMap<>();
    private final Map<UUID, Long> lastMoveCheck = new HashMap<>();
    private final Map<UUID, Set<Integer>> warned = new HashMap<>();

    public FlyManager(PumpkinFlyPlugin plugin) {
        this.plugin = plugin;
    }

    /** 主循环，每秒由 BukkitRunnable 在主线程调用。 */
    public void tick() {
        long now = System.currentTimeMillis();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p == null || !p.isOnline()) continue;
            UUID uuid = p.getUniqueId();

            if (isExempt(p)) {
                closeSession(p, "GAMEMODE", false);
                continue;
            }
            if (p.hasPermission("pumpkinfly.bypass")) continue;

            PlayerData d = plugin.data().get(uuid, p.getName());

            // REAL_TIME：按现实时间同步剩余秒数，离线也走
            if (plugin.config().isRealTime() && d.usesTime() && d.expireAt > 0L) {
                long left = Math.max(0L, (d.expireAt - now + 999L) / 1000L);
                if (left != d.remainingSeconds) {
                    d.remainingSeconds = left;
                    plugin.data().markDirty();
                }
            }

            ZoneChecker.Result zone = plugin.zones().check(p);
            boolean pass = d.hasPass();
            boolean allowed = pass && zone.allowed && !d.manualOff;

            if (allowed) {
                if (!p.getAllowFlight()) p.setAllowFlight(true);

                if (p.isFlying()) {
                    Session s = openSession(p, zone.label);
                    s.usedSeconds += plugin.config().tickSeconds;
                    d.lastActive = PlayerData.now();

                    if (d.usesTime()) {
                        if (!plugin.config().isRealTime()) {
                            d.remainingSeconds -= plugin.config().tickSeconds;
                            d.totalUsedSeconds += plugin.config().tickSeconds;
                            plugin.data().markDirty();
                        }
                        if (d.remainingSeconds <= 0L) {
                            d.remainingSeconds = 0L;
                            disableFlight(p, "expired", true);
                            continue;
                        }
                        warnIfNeeded(p, d.remainingSeconds);
                    } else if (!plugin.config().trackPermanentSessions) {
                        closeSession(p, "LANDED", false);
                    }
                } else {
                    closeSession(p, "LANDED", false);
                }
            } else {
                String reason = !pass ? "no-pass" : (!zone.allowed ? "left-zone" : "off");
                if (p.getAllowFlight() || p.isFlying()) {
                    disableFlight(p, reason, reason.equals("left-zone"));
                } else {
                    closeSession(p, "IDLE", false);
                }
            }
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
        boolean allowed = d.hasPass() && zone.allowed && !d.manualOff;
        if (!allowed && (p.getAllowFlight() || p.isFlying())) {
            disableFlight(p, !d.hasPass() ? "no-pass" : "left-zone", !d.hasPass() ? false : notifyThrottled(p));
        } else if (allowed && !p.getAllowFlight()) {
            p.setAllowFlight(true);
        }
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
        long maxWarn = 0L;
        for (int w : plugin.config().warnSeconds) maxWarn = Math.max(maxWarn, w);
        if (remaining > maxWarn) { sent.clear(); return; }
        for (int w : plugin.config().warnSeconds) {
            if (remaining == w && sent.add(w)) {
                p.sendMessage(plugin.config().msg("warn", "seconds", String.valueOf(remaining)));
            }
        }
    }

    /** 供 Essentials FlyStatusChangeEvent 门禁调用：当前是否具备飞行资格。 */
    public boolean canFly(Player p) {
        if (p == null || isExempt(p) || p.hasPermission("pumpkinfly.bypass")) return true;
        PlayerData d = plugin.data().peek(p.getUniqueId());
        if (d == null || !d.hasPass() || d.manualOff) return false;
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
            if (!d.hasPass()) {
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
        if (notify && messageKey != null && !messageKey.isEmpty()) {
            p.sendMessage(plugin.config().msg(messageKey));
        }
    }

    private static String messageKeyToReason(String key) {
        if ("left-zone".equals(key)) return "LEFT_ZONE";
        if ("expired".equals(key)) return "EXPIRED";
        if ("off".equals(key)) return "MANUAL_OFF";
        if ("no-pass".equals(key)) return "NO_PASS";
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
        if (s == null || !write) return;
        PlayerData d = plugin.data().peek(p.getUniqueId());
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

    public void onQuit(Player p) {
        closeSession(p, "LOGOUT", true);
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
        plugin.data().save();
    }

    public static boolean isExempt(Player p) {
        GameMode gm = p.getGameMode();
        return gm == GameMode.CREATIVE || gm == GameMode.SPECTATOR;
    }
}
