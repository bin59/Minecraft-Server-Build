package com.nangua.pumpkinfly;

import net.ess3.api.events.FlyStatusChangeEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Essentials 门禁：/fly、/efly、/essentials:fly、控制台 /fly <玩家> 都会先抛
 * FlyStatusChangeEvent，这里统一拦掉没有凭证或不在允许区域的情况。
 * 本类独立成文件，只有检测到 Essentials 时才注册，避免类缺失导致插件启动失败。
 */
public final class EssentialsGuard implements Listener {

    private final PumpkinFlyPlugin plugin;

    public EssentialsGuard(PumpkinFlyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFlyStatus(FlyStatusChangeEvent e) {
        if (!plugin.config().blockEssentialsFly) return;
        if (!e.getValue()) return;           // 关闭飞行不拦
        Player p = e.getAffected() == null ? null : e.getAffected().getBase();
        if (p == null || !p.isOnline() || FlyManager.isExempt(p)) return;
        if (plugin.fly().canFly(p)) return;

        e.setCancelled(true);
        PlayerData d = plugin.data().peek(p.getUniqueId());
        ZoneChecker.Result zone = plugin.zones().check(p);
        if (d == null || !d.hasPass()) {
            p.sendMessage(plugin.config().msg("no-pass"));
        } else {
            p.sendMessage(plugin.config().msg("fly-blocked", "zone", zone.label));
        }
    }
}
