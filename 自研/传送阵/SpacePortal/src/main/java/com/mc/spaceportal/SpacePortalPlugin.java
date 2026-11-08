package com.mc.spaceportal;

import com.mc.spaceportal.command.PortalCommand;
import com.mc.spaceportal.display.PortalDisplayManager;
import com.mc.spaceportal.effect.EffectController;
import com.mc.spaceportal.effect.PortalEffectTask;
import com.mc.spaceportal.gui.PortalGuiListener;
import com.mc.spaceportal.listener.PortalListener;
import com.mc.spaceportal.portal.PortalManager;
import com.mc.spaceportal.teleport.TeleportService;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * 空间传送阵 SpacePortal 主类。
 * 适用于 Java / 基岩互通服（Geyser）：默认以「特殊物品」静态标记阵法，粒子/音效为可选功能。
 */
public final class SpacePortalPlugin extends JavaPlugin {

    private PortalManager portalManager;
    private TeleportService teleportService;
    private PortalDisplayManager displayManager;

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
    private boolean entityFollowEnabled;
    private double petFollowRadius;
    private int costPerEntity;
    private boolean showCoordinates;

    /** 特效总开关：false = 关闭全部粒子、音效、时光隧道 */
    private boolean effectsEnabled;
    /** 特殊物品展示开关 */
    private boolean displayEnabled;
    /** 是否要求玩家手持特殊物品才能启动传送阵 */
    private boolean itemTriggerEnabled;
    private double displayHeight;
    private String displayMaterial;
    private String displayItemName;
    private String displayMode;

    /** 游戏内指令修改的配置（存于 override.yml，优先级高于 config.yml） */
    private YamlConfiguration override;
    private File overrideFile;

    /** 地面粒子特效任务句柄，便于修改间隔后重启 */
    private BukkitTask effectTaskHandle;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadOverride();

        portalManager = new PortalManager(this);
        portalManager.load();

        teleportService = new TeleportService(this);
        displayManager = new PortalDisplayManager(this);

        // 读到内存后再应用：applyConfig 会启动/停止特效任务与物品展示
        applyConfig();

        getServer().getPluginManager().registerEvents(new PortalListener(this), this);
        getServer().getPluginManager().registerEvents(new PortalGuiListener(this), this);

        PluginCommand command = getCommand("spaceportal");
        if (command != null) {
            PortalCommand executor = new PortalCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        getLogger().info("空间传送阵已启用！已加载 " + portalManager.size()
                + " 个传送阵；特效=" + (effectsEnabled ? "开" : "关")
                + "，物品标记=" + (displayEnabled ? "开" : "关") + "。");
    }

    /** 按当前配置启动（或重启）地面粒子特效任务；特效关闭时不启动任何定时任务 */
    private void startEffectTask() {
        if (effectTaskHandle != null) {
            effectTaskHandle.cancel();
            effectTaskHandle = null;
        }
        if (!effectsEnabled) {
            return;
        }
        effectTaskHandle = new PortalEffectTask(this)
                .runTaskTimer(this, 5L, Math.max(2, particleInterval));
    }

    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);
        if (displayManager != null) {
            displayManager.removeAll();
        }
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
        entityFollowEnabled = getConfig().getBoolean("entity-follow-enabled", true);
        petFollowRadius = Math.max(0.5, getConfig().getDouble("pet-follow-radius", 3.0));
        costPerEntity = Math.max(0, getConfig().getInt("cost-per-entity", 5));
        showCoordinates = getConfig().getBoolean("show-coordinates", false);

        effectsEnabled = getConfig().getBoolean("effects-enabled", false);
        displayEnabled = getConfig().getBoolean("display-item.enabled", true);
        itemTriggerEnabled = getConfig().getBoolean("display-item.use-as-trigger", false);
        displayHeight = getConfig().getDouble("display-item.height", 1.2);
        displayMaterial = getConfig().getString("display-item.material", "ECHO_SHARD");
        displayItemName = getConfig().getString("display-item.name", "&d✦ 空间传送阵");
        displayMode = getConfig().getString("display-item.mode", "ITEM_DISPLAY");

        // 静态开关放在特效入口，所有调用点统一生效（含 TeleportService 里的音效）
        EffectController.setEnabled(effectsEnabled);
        startEffectTask();
        if (displayManager != null) {
            if (displayEnabled) {
                displayManager.refresh();
            } else {
                displayManager.stop();
            }
        }
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

    /** 管理员用 /csz item set 保存的自定义展示物品（可为带数据组件的物品） */
    public ItemStack getCustomDisplayItem() {
        return override.getItemStack("display-item", null);
    }

    public void setCustomDisplayItem(ItemStack item) {
        if (item == null) {
            override.set("display-item", null);
        } else {
            override.set("display-item", item.clone());
        }
        saveOverride();
    }

    public void clearCustomDisplayItem() {
        override.set("display-item", null);
        saveOverride();
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

    public PortalDisplayManager getDisplayManager() {
        return displayManager;
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

    /** 是否允许坐骑/宠物随行传送 */
    public boolean isEntityFollowEnabled() {
        return entityFollowEnabled;
    }

    /** 宠物随行检测半径（格） */
    public double getPetFollowRadius() {
        return petFollowRadius;
    }

    /** 每个随行实体额外消耗的钻石数 */
    public int getCostPerEntity() {
        return costPerEntity;
    }

    /** 是否在 GUI / 列表命令中显示传送阵坐标 */
    public boolean isShowCoordinates() {
        return showCoordinates;
    }

    /** 特效总开关：false 时所有粒子、音效、时光隧道均不执行 */
    public boolean isEffectsEnabled() {
        return effectsEnabled;
    }

    public boolean isDisplayEnabled() {
        return displayEnabled;
    }

    /** 是否必须手持特殊物品才能启动传送阵 */
    public boolean isItemTriggerEnabled() {
        return itemTriggerEnabled;
    }

    public double getDisplayHeight() {
        return displayHeight;
    }

    public String getDisplayMaterial() {
        return displayMaterial;
    }

    public String getDisplayItemName() {
        return displayItemName;
    }

    /** 展示实体类型：ITEM_DISPLAY（默认）或 ARMOR_STAND（基岩兼容） */
    public String getDisplayMode() {
        return displayMode == null ? "ITEM_DISPLAY" : displayMode.trim().toUpperCase(Locale.ROOT);
    }

    /** 展示实体的朝向模式，配置值非法时回退 CENTER */
    public Display.Billboard getDisplayBillboard() {
        String raw = getConfig().getString("display-item.billboard", "CENTER");
        try {
            return Display.Billboard.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException ex) {
            return Display.Billboard.CENTER;
        }
    }
}
