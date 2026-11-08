package com.mc.spaceportal.effect;

import com.mc.spaceportal.SpacePortalPlugin;
import com.mc.spaceportal.portal.Portal;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * 阵法特效定时任务：每 particle-interval-ticks tick 渲染一帧地面粒子法阵。
 * 只渲染附近有玩家的世界（世界未加载则跳过）。
 */
public class PortalEffectTask extends BukkitRunnable {

    private final SpacePortalPlugin plugin;
    private double phase;

    public PortalEffectTask(SpacePortalPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        // 相位步长与刷新间隔联动，保证不同间隔下动画转速一致
        phase += 0.09 * plugin.getParticleInterval();
        if (phase > Math.PI * 200) {
            phase = 0;
        }
        double rd = plugin.getRenderDistance();
        double rdSq = rd * rd;
        for (Portal portal : plugin.getPortals().all()) {
            Location center = portal.getCenter();
            World world = center.getWorld();
            if (world == null) {
                continue; // 世界未加载
            }
            // 附近没有玩家时不渲染，节省性能
            // 直接遍历世界玩家列表，避免 getNearbyEntities 扫描全部实体（怪物/掉落物等）
            if (!hasViewer(world, center, rdSq)) {
                continue;
            }
            double charge = plugin.getTeleportService().getChargeProgress(portal.getName());
            EffectController.drawPortalFrame(portal, center, plugin.getPortalRadius(), phase, charge);
        }
    }

    private boolean hasViewer(World world, Location center, double radiusSq) {
        for (Player player : world.getPlayers()) {
            Location loc = player.getLocation();
            double dx = loc.getX() - center.getX();
            double dz = loc.getZ() - center.getZ();
            double dy = loc.getY() - center.getY();
            if (dx * dx + dy * dy + dz * dz <= radiusSq) {
                return true;
            }
        }
        return false;
    }
}
