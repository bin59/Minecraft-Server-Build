package com.mc.spaceportal;

import com.mc.spaceportal.command.PortalCommand;
import com.mc.spaceportal.effect.PortalEffectTask;
import com.mc.spaceportal.gui.PortalGuiListener;
import com.mc.spaceportal.listener.PortalListener;
import com.mc.spaceportal.portal.PortalManager;
import com.mc.spaceportal.teleport.TeleportService;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;

/**
 * 空间传送阵 SpacePortal 主类。
 * 适用于 Java / 基岩互通服（Geyser）：粒子与音效均走 Bukkit 标准通道，双端可见。
 */
public final class SpacePortalPlugin extends JavaPlugin {

    private PortalManager portalManager;
    private TeleportService teleportService;

    private int cost;
    private int playerPortalLimit;
    private int chargeTicks;
    private int tunnelTicks;
    private int particleInterval;
    private int cooldownSeconds;
    private double portalRadius;
    private double activateRadius;
    private double renderDistance;
    private boolean freeInCreative;

    /** 游戏内指令修改的配置（存于 override.yml，优先级高于 config.yml） */
    private YamlConfiguration override;
    private File overrideFile;

    /** 地面粒子特效任务句柄，便于修改间隔后重启 */
    private BukkitTask effectTaskHandle;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadOverride();
        applyConfig();

        portalManager = new PortalManager(this);
        portalManager.load();

        teleportService = new TeleportService(this);

        getServer().getPluginManager().registerEvents(new PortalListener(this), this);
        getServer().getPluginManager().registerEvents(new PortalGuiListener(this), this);

        PluginCommand command = getCommand("spaceportal");
        if (command != null) {
            PortalCommand executor = new PortalCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

    // 地面阵法粒子特效：间隔由 particle-interval-ticks 控制（applyConfig 内部启动）

        getLogger().info("空间传送阵已启用！已加载 " + portalManager.size() + " 个传送阵。");
    }

    /** 按当前配置启动（或重启）地面粒子特效任务 */
    private void startEffectTask() {
        if (effectTaskHandle != null) {
            effectTaskHandle.cancel();
            effectTaskHandle = null;
        }
        effectTaskHandle = new PortalEffectTask(this)
                .runTaskTimer(this, 5L, Math.max(2, particleInterval));
    }

    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);
        if (portalManager != null) {
            portalManager.save();
        }
        saveOverride();
        getLogger().info("空间传送阵已卸载。");
    }

    /** 加载游戏内覆盖配置 */
    private void loadOverride() {
        overrideFile = new File(getDataFolder(), "override.yml");
        override = YamlConfiguration.loadConfiguration(overrideFile);
    }

    /** 保存游戏内覆盖配置 */
    public void saveOverride() {
        if (override == null || overrideFile == null) {
            return;
        }
        try {
            override.save(overrideFile);
        } catch (IOException ex) {
            getLogger().severe("保存 override.yml 失败: " + ex.getMessage());
        }
    }

    /** 读取配置到内存（供 /csz reload 使用），游戏内覆盖优先 */
    public void applyConfig() {
        cost = override.getInt("cost-diamonds", getConfig().getInt("cost-diamonds", 5));
        playerPortalLimit = override.getInt("player-portal-limit",
                getConfig().getInt("player-portal-limit", 1));
        chargeTicks = Math.max(10, getConfig().getInt("charge-ticks", 60));
        tunnelTicks = Math.max(10, getConfig().getInt("tunnel-ticks", 50));
        particleInterval = Math.max(2, getConfig().getInt("particle-interval-ticks", 4));
        cooldownSeconds = Math.max(0, getConfig().getInt("cooldown-seconds", 5));
        portalRadius = Math.max(1.0, getConfig().getDouble("portal-radius", 3.0));
        activateRadius = Math.max(1.0, getConfig().getDouble("activate-radius", 2.2));
        renderDistance = Math.max(8.0, getConfig().getDouble("render-distance", 40.0));
        freeInCreative = getConfig().getBoolean("free-in-creative", true);
        // 粒子刷新间隔可能刚被改过，据此重启特效任务
        startEffectTask();
    }

    /** 游戏内设置传送费用（/csz setcost），写入覆盖配置并立即生效 */
    public void setCost(int newCost) {
        override.set("cost-diamonds", newCost);
        saveOverride();
        applyConfig();
    }

    /** 游戏内设置玩家建阵数量上限（/csz setlimit 或 GUI），写入覆盖配置并立即生效 */
    public void setPlayerPortalLimit(int newLimit) {
        override.set("player-portal-limit", newLimit);
        saveOverride();
        applyConfig();
    }

    /** 获取玩家建阵数量上限：0 = 禁止玩家自建，-1 = 不限制 */
    public int getPlayerPortalLimit() {
        return playerPortalLimit;
    }

    public PortalManager getPortals() {
        return portalManager;
    }

    public TeleportService getTeleportService() {
        return teleportService;
    }

    public int getCost() {
        return cost;
    }

    public int getChargeTicks() {
        return chargeTicks;
    }

    public int getTunnelTicks() {
        return tunnelTicks;
    }

    /** 地面粒子特效的刷新间隔（tick）：2 = 每 0.1 秒一帧，4 = 每 0.2 秒一帧，越大越省 */
    public int getParticleInterval() {
        return particleInterval;
    }

    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    public double getPortalRadius() {
        return portalRadius;
    }

    public double getActivateRadius() {
        return activateRadius;
    }

    public double getRenderDistance() {
        return renderDistance;
    }

    public boolean isFreeInCreative() {
        return freeInCreative;
    }
}
