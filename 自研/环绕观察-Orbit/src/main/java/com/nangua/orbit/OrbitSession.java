package com.nangua.orbit;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 * 单个观察者对一个目标玩家的环绕会话。
 *
 * 实现：观察者切到旁观者模式，本体直接按圆周坐标传送并朝向目标。
 *
 * 旧版用「隐形 ArmorStand + setSpectatorTarget」当相机，但 Camera 包要求客户端
 * 已经追踪到该实体；spawn 后立刻绑定常被客户端丢弃，表现为「盔甲架在转、
 * 观察者画面不动」，且盔甲架可能意外可见。改为直接移动观察者本体后，
 * 不依赖任何实体或相机包，行为确定、跨核心一致。
 */
public final class OrbitSession {

    private static final double FULL_CIRCLE = Math.PI * 2.0;

    private final OrbitPlugin plugin;
    private final Player viewer;
    private Player target;
    private OrbitConfig config;
    private BukkitTask task;
    private double angle;
    private long ticks;
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
        this.ticks = 0L;

        // 跨世界先挪到目标世界（旁观者不受地形限制）
        if (!viewer.getWorld().equals(target.getWorld())) {
            viewer.teleport(target.getLocation());
        }
        viewer.setGameMode(GameMode.SPECTATOR);
        clearSpectatorTarget();

        // 立即到位，不必等第一个 tick
        move();

        // 固定 1 tick 调度，内部按 update-interval-ticks 抽帧，reload 后立即生效
        this.task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    private void tick() {
        if (!viewer.isOnline() || !target.isOnline()) {
            // 从插件会话表中一并移除，避免留下僵尸 session
            plugin.stopSession(viewer);
            return;
        }
        if (viewer.getGameMode() != GameMode.SPECTATOR) {
            viewer.setGameMode(GameMode.SPECTATOR);
            clearSpectatorTarget();
        }
        int interval = Math.max(1, config.getUpdateIntervalTicks());
        ticks++;
        if (ticks % interval != 0) {
            return;
        }
        // 按实际抽帧间隔换算角速度，改 update-interval-ticks 不影响转速
        angle += Math.toRadians(config.getSpeedDegreesPerSecond()) * interval / 20.0;
        if (angle > FULL_CIRCLE) {
            angle -= FULL_CIRCLE;
        }
        move();
    }

    private void move() {
        viewer.teleport(computeLocation());
    }

    private void clearSpectatorTarget() {
        try {
            viewer.setSpectatorTarget(null);
        } catch (IllegalArgumentException ignored) {
            // 不在旁观者模式时该调用会抛异常，忽略
        }
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

    /** 自动巡视切换目标：不重建会话，避免游戏模式与位置来回闪烁。 */
    public void retarget(Player newTarget, OrbitConfig newConfig) {
        this.target = newTarget;
        this.config = newConfig;
        this.angle = 0.0;
        this.ticks = 0L;
        move();
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        if (viewer.isOnline()) {
            clearSpectatorTarget();
            if (previousLocation != null && previousLocation.getWorld() != null) {
                viewer.teleport(previousLocation);
            }
            if (previousGameMode != null) {
                viewer.setGameMode(previousGameMode);
            }
        }
    }
}
