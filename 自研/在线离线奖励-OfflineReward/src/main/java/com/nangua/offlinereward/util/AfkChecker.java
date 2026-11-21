package com.nangua.offlinereward.util;

import com.nangua.offlinereward.OfflineReward;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

/**
 * AFK 判定（1.1.4）：通过反射接入 EssentialsX 的 {@code User.isAfk()}，避免编译期硬依赖。
 *
 * <p>设计原则：
 * <ul>
 *   <li>开关实时读 {@code online-reward.ignore-afk}，{@code /offlinereward reload} 后立即生效；</li>
 *   <li>反射 Method 缓存到 static（类结构不变），每次调用只做 method.invoke，开销极小；</li>
 *   <li>EssentialsX 缺失 / 版本不匹配 / 反射异常时优雅降级为「非 AFK」，不影响原有在线奖励行为。</li>
 * </ul>
 */
public final class AfkChecker {

    private final OfflineReward plugin;
    private static volatile boolean probed = false;
    private static volatile Method getUserMethod = null;   // Essentials.getUser(Player)
    private static volatile Method isAfkMethod = null;     // UserData.isAfk()

    public AfkChecker(OfflineReward plugin) {
        this.plugin = plugin;
    }

    /** 本次是否启用 AFK 门控（配置开关实时读取）。 */
    public boolean enabled() {
        return plugin.getConfig().getBoolean("online-reward.ignore-afk", true);
    }

    /** EssentialsX 的 AFK API 是否已成功探测到（用于 check 命令显示状态）。 */
    public boolean available() {
        probe();
        return getUserMethod != null && isAfkMethod != null;
    }

    /**
     * 判定玩家当前是否 AFK。未启用 / EssentialsX 缺失 / 反射失败时一律返回 false（视为活跃）。
     */
    public boolean isAfk(Player p) {
        if (!enabled()) return false;
        if (!probe()) return false;
        try {
            Object user = getUserMethod.invoke(essentials(), p);
            if (user == null) return false;
            return Boolean.TRUE.equals(isAfkMethod.invoke(user));
        } catch (Throwable t) {
            return false; // 优雅降级：任何异常都当作非 AFK，绝不让在线奖励因此停摆
        }
    }

    private static Plugin essentials() {
        return Bukkit.getPluginManager().getPlugin("Essentials");
    }

    /** 懒加载反射 Method；返回是否可用。只探测一次。 */
    private static boolean probe() {
        if (probed) return getUserMethod != null && isAfkMethod != null;
        synchronized (AfkChecker.class) {
            if (probed) return getUserMethod != null && isAfkMethod != null;
            probed = true;
            try {
                Plugin ess = essentials();
                if (ess == null || !ess.isEnabled()) return false;
                Class<?> essCls = ess.getClass();
                getUserMethod = essCls.getMethod("getUser", Player.class);
                Class<?> userCls = Class.forName("com.earth2me.essentials.UserData");
                isAfkMethod = userCls.getMethod("isAfk");
            } catch (Throwable t) {
                getUserMethod = null;
                isAfkMethod = null;
            }
            return getUserMethod != null && isAfkMethod != null;
        }
    }
}
