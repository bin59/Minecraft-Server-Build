package com.nangua.orbit;

import org.bukkit.configuration.file.FileConfiguration;

public final class OrbitConfig {

    private final double radius;
    private final double height;
    private final double speedDegreesPerSecond;
    private final double lookHeight;
    private final String msgStarted;
    private final String msgStopped;
    private final String msgTargetOffline;
    private final String msgSelf;
    private final String msgNoPermission;
    private final boolean autoTourEnabled;
    private final String autoTourViewer;
    private final int intervalSeconds;
    private final int perPlayerSeconds;

    public OrbitConfig(FileConfiguration config) {
        this.radius = Math.max(1.0, config.getDouble("radius", 6.0));
        this.height = Math.max(1.0, config.getDouble("height", 5.0));
        this.speedDegreesPerSecond = Math.max(1.0, config.getDouble("speed-degrees-per-second", 16.0));
        this.lookHeight = config.getDouble("look-height", 1.0);
        this.msgStarted = config.getString("messages.started", "&a开始环绕观察 &e%player%");
        this.msgStopped = config.getString("messages.stopped", "&a已结束环绕观察");
        this.msgTargetOffline = config.getString("messages.target-offline", "&c目标玩家不在线");
        this.msgSelf = config.getString("messages.self", "&c不能环绕观察自己");
        this.msgNoPermission = config.getString("messages.no-permission", "&c你没有权限");
        this.autoTourEnabled = config.getBoolean("auto-tour.enabled", false);
        this.autoTourViewer = config.getString("auto-tour.viewer", "");
        this.intervalSeconds = Math.max(1, config.getInt("auto-tour.interval-seconds", 3600));
        this.perPlayerSeconds = Math.max(1, config.getInt("auto-tour.per-player-seconds", 30));
    }

    public double getRadius() {
        return radius;
    }

    public double getHeight() {
        return height;
    }

    public double getSpeedDegreesPerSecond() {
        return speedDegreesPerSecond;
    }

    public double getLookHeight() {
        return lookHeight;
    }

    public String getMsgStarted() {
        return msgStarted;
    }

    public String getMsgStopped() {
        return msgStopped;
    }

    public String getMsgTargetOffline() {
        return msgTargetOffline;
    }

    public String getMsgSelf() {
        return msgSelf;
    }

    public String getMsgNoPermission() {
        return msgNoPermission;
    }

    public boolean isAutoTourEnabled() {
        return autoTourEnabled;
    }

    public String getAutoTourViewer() {
        return autoTourViewer;
    }

    public int getIntervalSeconds() {
        return intervalSeconds;
    }

    public int getPerPlayerSeconds() {
        return perPlayerSeconds;
    }
}
