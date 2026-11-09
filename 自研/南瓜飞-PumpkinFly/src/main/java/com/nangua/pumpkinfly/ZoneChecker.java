package com.nangua.pumpkinfly;

import com.bekvon.bukkit.residence.api.ResidenceApi;
import com.bekvon.bukkit.residence.containers.ResidencePlayer;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** 飞行区域判定：世界过滤 + Residence 领地归属 + 主城白名单。 */
public final class ZoneChecker {

    /** 判定结果，label 用于发给玩家的提示。 */
    public static final class Result {
        public final boolean allowed;
        public final String label;
        Result(boolean allowed, String label) { this.allowed = allowed; this.label = label; }
    }

    private final PumpkinFlyPlugin plugin;
    private boolean residenceAvailable;

    public ZoneChecker(PumpkinFlyPlugin plugin) {
        this.plugin = plugin;
    }

    public void refreshAvailability() {
        residenceAvailable = plugin.getServer().getPluginManager().isPluginEnabled("Residence");
        if (!residenceAvailable) {
            plugin.getLogger().warning("未检测到 Residence，区域判定按 zones.when-residence-missing 处理。");
        }
    }

    public boolean isResidenceAvailable() {
        return residenceAvailable;
    }

    public Result check(Player p) {
        FlyConfig c = plugin.config();
        Location loc = p.getLocation();
        String world = loc.getWorld() == null ? "" : loc.getWorld().getName();
        String lw = world.toLowerCase(Locale.ROOT);

        if (c.worldBlacklist.contains(lw)) return deny("世界 " + world + "（黑名单）");
        if (!c.worldWhitelist.isEmpty() && !c.worldWhitelist.contains(lw)) return deny("世界 " + world + "（不在白名单）");

        if (!residenceAvailable) {
            if ("ALLOW".equals(c.whenResidenceMissing)) return allow("无 Residence，全部放行");
            if ("DENY".equals(c.whenResidenceMissing)) return deny("无 Residence，禁止飞行");
            return wilderness(c, world, lw);
        }

        ClaimedResidence res = residenceAt(loc);
        if (res == null) return wilderness(c, world, lw);

        String name = res.getName() == null ? "" : res.getName();
        String ln = name.toLowerCase(Locale.ROOT);
        if (c.residenceBlacklist.contains(ln)) return deny("领地 " + name + "（黑名单）");

        switch (c.zoneMode) {
            case "EVERYWHERE":
                return allow("领地 " + name);
            case "ALL_RESIDENCES":
                return allow("领地 " + name);
            case "WHITELIST":
                return c.residenceWhitelist.contains(ln)
                        ? allow("指定领地 " + name)
                        : deny("领地 " + name + "（不在白名单）");
            case "OWN_AND_MAIN":
            default:
                if (c.mainResidences.contains(ln)) return allow("主城 " + name);
                UUID owner = ownerUuid(res);
                if (owner != null && owner.equals(p.getUniqueId()) && c.allowOwn) {
                    return allow("自己的领地 " + name);
                }
                if (c.allowTrusted && isTrusted(res, p.getUniqueId())) {
                    return allow("被信任的领地 " + name);
                }
                if (c.allowOther) return allow("领地 " + name);
                return deny("领地 " + name + "（非本人领地）");
        }
    }

    private Result wilderness(FlyConfig c, String world, String lw) {
        if ("EVERYWHERE".equals(c.zoneMode)) return allow("野外 " + world);
        if (c.allowWilderness || c.wildernessWorlds.contains(lw)) return allow("野外 " + world);
        return deny("野外 " + world);
    }

    private static Result allow(String label) { return new Result(true, label); }
    private static Result deny(String label) { return new Result(false, label); }

    private ClaimedResidence residenceAt(Location loc) {
        try {
            return ResidenceApi.getResidenceManager().getByLoc(loc);
        } catch (Throwable t) {
            plugin.getLogger().warning("Residence 区域查询失败：" + t.getClass().getSimpleName() + " " + t.getMessage());
            return null;
        }
    }

    private static UUID ownerUuid(ClaimedResidence res) {
        try {
            return res.getOwnerUUID();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean isTrusted(ClaimedResidence res, UUID uuid) {
        try {
            Set<ResidencePlayer> trusted = res.getTrustedPlayers();
            if (trusted == null) return false;
            for (ResidencePlayer rp : trusted) {
                if (rp == null) continue;
                UUID u = rp.getUniqueId();
                if (uuid.equals(u)) return true;
            }
        } catch (Throwable ignored) { }
        return false;
    }
}
