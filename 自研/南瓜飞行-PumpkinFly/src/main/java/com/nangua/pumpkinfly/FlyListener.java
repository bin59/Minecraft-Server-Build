package com.nangua.pumpkinfly;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** 通用 Bukkit 事件：进出服、移动、切世界/模式、摔落保护。 */
public final class FlyListener implements Listener {

    private final PumpkinFlyPlugin plugin;

    public FlyListener(PumpkinFlyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (plugin.config().updateNameOnJoin) {
            PlayerData d = plugin.data().get(p.getUniqueId(), p.getName());
            if (!p.getName().equals(d.name)) {
                d.name = p.getName();
                plugin.data().markDirty();
            }
        }
        plugin.fly().checkNow(p, false);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        plugin.fly().onQuit(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        if (e.getTo() == null) return;
        if (e.getFrom().getBlockX() == e.getTo().getBlockX()
                && e.getFrom().getBlockY() == e.getTo().getBlockY()
                && e.getFrom().getBlockZ() == e.getTo().getBlockZ()) return;
        plugin.fly().checkNow(e.getPlayer(), true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent e) {
        if (plugin.config().disableOnWorldChange) {
            plugin.fly().closeSession(e.getPlayer(), "WORLD_CHANGE", true);
            plugin.fly().checkNow(e.getPlayer(), false);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGameMode(PlayerGameModeChangeEvent e) {
        if (plugin.config().disableOnGamemodeChange) {
            plugin.fly().closeSession(e.getPlayer(), "GAMEMODE", true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(EntityDamageEvent e) {
        if (e.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        Entity entity = e.getEntity();
        if (!(entity instanceof Player)) return;
        if (plugin.fly().isNoFall(entity.getUniqueId())) {
            e.setCancelled(true);
            plugin.fly().clearNoFall(entity.getUniqueId());
        }
    }
}
