package com.nangua.quickmenu.hook;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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

    /**
     * 列出玩家自己名下（owner）的全部领地名称。
     * 未装 Residence、玩家无任何领地、或反射调用失败时返回空列表。
     *
     * <p>用于领地选择器的动态列表：玩家点击菜单项后，插件直接把其名下所有
     * 领地以可点击的形式展示，无需手敲领地名。
     *
     * <p>API 调用链与 {@link #isInsideOwnResidence} 一致：
     * {@code ResidenceApi.getResidence().getResidenceManager().getOwnedByPlayer(player)}。
     * 不同 Residence 版本中 {@code getOwnedByPlayer} 可能是
     * {@code (Player)} 或 {@code (Player, boolean)} 两种签名，此处逐一尝试。
     */
    public static List<String> getOwnResidenceNames(Player player) {
        if (!isAvailable() || player == null) {
            return Collections.emptyList();
        }
        try {
            Class<?> apiCls = Class.forName("com.bekvon.bukkit.residence.api.ResidenceApi");
            Method getApi = apiCls.getMethod("getResidence");
            Object api = getApi.invoke(null);
            if (api == null) {
                return Collections.emptyList();
            }
            Method getRm = api.getClass().getMethod("getResidenceManager");
            Object rm = getRm.invoke(api);
            if (rm == null) {
                return Collections.emptyList();
            }

            // 两种可能的签名：(Player) 与 (Player, boolean)，逐一尝试
            Map<?, ?> owned = null;
            try {
                Method byPlayer = rm.getClass().getMethod("getOwnedByPlayer", Player.class);
                Object result = byPlayer.invoke(rm, player);
                if (result instanceof Map) {
                    owned = (Map<?, ?>) result;
                }
            } catch (NoSuchMethodException ignored) {
                // 该版本没有 (Player) 签名，继续尝试带 boolean 的重载
            }
            if (owned == null) {
                try {
                    Method byPlayer = rm.getClass().getMethod("getOwnedByPlayer", Player.class, boolean.class);
                    Object result = byPlayer.invoke(rm, player, false);
                    if (result instanceof Map) {
                        owned = (Map<?, ?>) result;
                    }
                } catch (NoSuchMethodException ignored) {
                    // 两个签名都不存在
                }
            }
            if (owned == null) {
                return Collections.emptyList();
            }

            List<String> names = new ArrayList<>();
            for (Object key : owned.keySet()) {
                names.add(String.valueOf(key));
            }
            return names;
        } catch (Throwable t) {
            return Collections.emptyList();
        }
    }
}
