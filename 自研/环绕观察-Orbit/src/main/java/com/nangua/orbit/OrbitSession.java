package com.nangua.orbit;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 * 单个观察者对一个目标玩家的环绕会话。
 * 每 tick 计算圆周坐标并移动隐形相机，观察者以旁观者模式跟随相机。
 */
public final class OrbitSession {

    private final OrbitPlugin plugin;
    private final Player viewer;
    private Player target;
    private OrbitConfig config;
    private ArmorStand camera;
    private BukkitTask task;
    private double angle;
    private GameMode previousGameMode;
    private Location previousLocation;

    public OrbitSession(OrbitPlugin plugin, Player viewer, Player target, OrbitConfig config) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.target = target;
        this.config = config;
    }

    public Player getViewer() {
        return viewer;
    }

    public Player getTarget() {
        return target;
    }

    public boolean isActive() {
        return task != null && !task.isCancelled();
    }

    public void updateConfig(OrbitConfig newConfig) {
        this.config = newConfig;
    }

    public void start() {
        this.previousGameMode = viewer.getGameMode();
        this.previousLocation = viewer.getLocation().clone();
        this.angle = 0.0;

        // 跨世界无法旁观，先把观察者挪到目标世界
        if (!viewer.getWorld().equals(target.getWorld())) {
            viewer.teleport(target.getLocation());
        }

        this.camera = spawnCamera();
        viewer.setGameMode(GameMode.SPECTATOR);

        // 延后 1 tick 绑定旁观目标，确保模式已切换生效
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (viewer.isOnline() && camera != null && camera.isValid()) {
                viewer.setSpectatorTarget(camera);
            }
        });

        this.task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 0L, 1L);
    }

    private ArmorStand spawnCamera() {
        Location location = computeLocation();
        return target.getWorld().spawn(location, ArmorStand.class, stand -> {
            stand.setInvisible(true);
            stand.setMarker(true);
            stand.setGravity(false);
            stand.setSilent(true);
            stand.setInvulnerable(true);
            stand.setSmall(true);
            stand.setBasePlate(false);
            stand.setArms(false);
            stand.setPersistent(false);
        });
    }

    private void tick() {
        if (!viewer.isOnline() || !target.isOnline() || camera == null || !camera.isValid()) {
            stop();
            return;
        }
        angle += Math.toRadians(config.getSpeedDegreesPerSecond()) / 20.0;
        camera.teleport(computeLocation());
    }

    private Location computeLocation() {
        Location base = target.getLocation();
        double x = base.getX() + config.getRadius() * Math.cos(angle);
        double z = base.getZ() + config.getRadius() * Math.sin(angle);
        double y = base.getY() + config.getHeight();
        Location location = new Location(base.getWorld(), x, y, z);
        location.setDirection(base.clone()
                .add(0.0, config.getLookHeight(), 0.0)
                .toVector()
                .subtract(location.toVector()));
        return location;
    }

    /** 自动巡视切换目标：不重建会话，避免游戏模式闪烁。 */
    public void retarget(Player newTarget, OrbitConfig newConfig) {
        this.target = newTarget;
        this.config = newConfig;
        this.angle = 0.0;
        if (camera != null && camera.isValid()) {
            camera.teleport(computeLocation());
        }
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        if (camera != null) {
            if (camera.isValid()) {
                camera.remove();
            }
            camera = null;
        }
        if (viewer.isOnline()) {
            try {
                viewer.setSpectatorTarget(null);
            } catch (IllegalArgumentException ignored) {
                // 已不在旁观状态，忽略
            }
            if (previousLocation != null && previousLocation.getWorld() != null) {
                viewer.teleport(previousLocation);
            }
            if (previousGameMode != null) {
                viewer.setGameMode(previousGameMode);
            }
        }
    }
}
