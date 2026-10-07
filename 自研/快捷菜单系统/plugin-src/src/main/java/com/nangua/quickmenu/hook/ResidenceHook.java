package com.nangua.quickmenu.hook;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

/**
 * Residence 领地感知（软依赖，用反射调用，未装 Residence 时所有方法返回 false）。
 *
 * 判断玩家是否站在自己（或自己有权限的）领地里：
 *   ResidenceApi.getResidenceManager().getByPlayer(player) → Residence
 *   residence.isOwner(player) / residence.getOwner()
 */
public final class ResidenceHook {

    private static volatile Boolean available = null;

    private ResidenceHook() {}

    /** Residence 插件是否存在。 */
    public static boolean isAvailable() {
        if (available == null) {
            available = Bukkit.getPluginManager().getPlugin("Residence") != null;
        }
        return available;
    }

    /**
     * 玩家是否站在自己的 Residence 领地里（owner 或 member 都算）。
     * 未装 Residence、玩家不在任何领地、或领地不属于玩家时返回 false。
     */
    public static boolean isInsideOwnResidence(Player player) {
        if (!isAvailable() || player == null) return false;
        try {
            Class<?> apiCls = Class.forName("com.bekvon.bukkit.residence.api.ResidenceApi");
            Method getApi = apiCls.getMethod("getResidence");
            Object api = getApi.invoke(null);
            if (api == null) return false;
            Method getRm = api.getClass().getMethod("getResidenceManager");
            Object rm = getRm.invoke(api);
            if (rm == null) return false;
            Method getByPlayer = rm.getClass().getMethod("getByPlayer", Player.class);
            Object res = getByPlayer.invoke(rm, player);
            if (res == null) return false;

            // isOwner(Player)
            try {
                Method isOwner = res.getClass().getMethod("isOwner", Player.class);
                Object r = isOwner.invoke(res, player);
                if (r instanceof Boolean && (Boolean) r) return true;
            } catch (NoSuchMethodException ignored) {}

            // 兜底：getOwner().equals(player.getName())
            try {
                Method getOwner = res.getClass().getMethod("getOwner");
                Object owner = getOwner.invoke(res);
                if (owner != null && player.getName().equalsIgnoreCase(String.valueOf(owner))) return true;
            } catch (NoSuchMethodException ignored) {}

            // member 判断：isOwner(player) 可能只认主人，member 也允许
            try {
                Method isMember = res.getClass().getMethod("isMember", Player.class);
                Object r = isMember.invoke(res, player);
                if (r instanceof Boolean && (Boolean) r) return true;
            } catch (NoSuchMethodException ignored) {}

            return false;
        } catch (Throwable t) {
            return false;
        }
    }
}
