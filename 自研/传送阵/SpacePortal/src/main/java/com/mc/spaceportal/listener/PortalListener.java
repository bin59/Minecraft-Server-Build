package com.mc.spaceportal.listener;

import com.mc.spaceportal.SpacePortalPlugin;
import com.mc.spaceportal.portal.Portal;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 玩家移动监听：检测玩家进入阵法范围并触发充能。
 * 只在玩家跨越"进入边界"时触发一次，避免移动事件重复刷屏。
 */
public class PortalListener implements Listener {

    private final SpacePortalPlugin plugin;
    /** 记录每个玩家当前所在的阵法范围（name），null 表示不在任何阵法内 */
    private final Map<UUID, String> inside = new HashMap<>();

    public PortalListener(SpacePortalPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        // 只关心跨格移动
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        checkEntry(event.getPlayer());
    }

    private void checkEntry(Player player) {
        String now = findPortalNear(player);
        String before = inside.get(player.getUniqueId());

        if (now == null) {
            if (before != null) {
                inside.remove(player.getUniqueId());
            }
            return;
        }
        // 从阵法外进入（或从一个阵法切换到另一个）→ 触发充能
        if (!now.equals(before)) {
            inside.put(player.getUniqueId(), now);
            Portal portal = plugin.getPortals().get(now);
            if (portal != null) {
                plugin.getTeleportService().tryStartCharge(player, portal);
            }
        }
    }

    /** 查找玩家水平距离最近的激活范围内阵法，无则返回 null */
    private String findPortalNear(Player player) {
        Location loc = player.getLocation();
        String best = null;
        double bestDist = Double.MAX_VALUE;
        double radius = plugin.getActivateRadius();
        for (Portal portal : plugin.getPortals().all()) {
            if (!portal.getWorldName().equals(loc.getWorld().getName())) {
                continue;
            }
            double dx = loc.getX() - portal.getX();
            double dz = loc.getZ() - portal.getZ();
            double dy = loc.getY() - portal.getY();
            double distSq = dx * dx + dz * dz;
            if (distSq <= radius * radius && Math.abs(dy) <= 2.5 && distSq < bestDist) {
                bestDist = distSq;
                best = portal.getName();
            }
        }
        return best;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        inside.remove(event.getPlayer().getUniqueId());
        plugin.getTeleportService().handleQuit(event.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        inside.remove(event.getEntity().getUniqueId());
        plugin.getTeleportService().cancelCharge(event.getEntity(), false);
    }

    /** 玩家被其他方式传送离开时，清理进入状态（防止落地后立即重复触发） */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN
                || event.getCause() == PlayerTeleportEvent.TeleportCause.COMMAND) {
            inside.remove(event.getPlayer().getUniqueId());
        }
    }
}
