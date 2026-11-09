package com.nangua.pumpkinfly;

import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;

/** 南瓜飞主类：所有组件在此装配，任务均为主线程同步执行。 */
public final class PumpkinFlyPlugin extends JavaPlugin {

    private FlyConfig config;
    private DataStore data;
    private GlobalState global;
    private EconomyHook economy;
    private ZoneChecker zones;
    private FlyManager fly;
    private BukkitTask tickTask;
    private BukkitTask saveTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = new FlyConfig(this);
        config.reload();

        data = new DataStore(this);
        data.load();

        // 必须在 config.reload() 之后：global.yml 不存在时用 config.yml 的值作初值
        global = new GlobalState(this);
        global.load();

        economy = new EconomyHook(this);
        economy.hook();

        zones = new ZoneChecker(this);
        zones.refreshAvailability();

        fly = new FlyManager(this);

        PluginCommand cmd = getCommand("pfly");
        if (cmd != null) {
            FlyCommand executor = new FlyCommand(this);
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        } else {
            getLogger().severe("plugin.yml 中未注册 pfly 命令！");
        }
        getServer().getPluginManager().registerEvents(new FlyListener(this), this);
        if (getServer().getPluginManager().isPluginEnabled("Essentials")) {
            getServer().getPluginManager().registerEvents(new EssentialsGuard(this), this);
            getLogger().info("已挂载 Essentials 飞行门禁（拦截 /fly 绕过区域限制）。");
        }
        startTasks();
        warnEssentialsAutoFly();

        getLogger().info("南瓜飞已启用 | 计时=" + config.timingMode
                + " | 区域=" + config.zoneSummary()
                + " | 永久价=" + (long) config.permanentPrice
                + " | 每分钟=" + (long) config.perMinute);
    }

    @Override
    public void onDisable() {
        if (tickTask != null) tickTask.cancel();
        if (saveTask != null) saveTask.cancel();
        if (fly != null) fly.onDisable();
        if (global != null) global.save();
        getLogger().info("南瓜飞已关闭，数据已落盘。");
    }

    /** /pfly reload：重载配置并重建任务（tick 间隔可能变化）。 */
    public void reloadAll() {
        config.reload();
        zones.refreshAvailability();
        if (fly != null) fly.display().onReload();
        startTasks();
    }

    private void startTasks() {
        if (tickTask != null) tickTask.cancel();
        if (saveTask != null) saveTask.cancel();
        tickTask = new BukkitRunnable() {
            @Override public void run() {
                try { fly.tick(); } catch (Throwable t) {
                    getLogger().warning("飞行主循环异常：" + t.getClass().getSimpleName() + " " + t.getMessage());
                }
            }
        }.runTaskTimer(this, 20L, config.tickSeconds * 20L);
        saveTask = new BukkitRunnable() {
            @Override public void run() {
                try { data.save(); } catch (Throwable t) {
                    getLogger().warning("定时落盘异常：" + t.getMessage());
                }
            }
        }.runTaskTimer(this, config.saveIntervalSeconds * 20L, config.saveIntervalSeconds * 20L);
    }

    private void warnEssentialsAutoFly() {
        if (!config.warnEssentialsAutoFly) return;
        File f = new File(getDataFolder().getParentFile(), "Essentials" + File.separator + "config.yml");
        if (!f.exists()) return;
        try {
            YamlConfiguration yc = YamlConfiguration.loadConfiguration(f);
            for (String key : yc.getKeys(true)) {
                String k = key.toLowerCase();
                if (k.contains("fly") && (k.contains("join") || k.contains("reset")) && yc.getBoolean(key, false)) {
                    getLogger().warning("Essentials 的 " + key + " 为 true，会在进服/换世界时抢写飞行状态；"
                            + "本插件每秒会纠正，但建议关掉以避免抖动。");
                }
            }
        } catch (Throwable ignored) { }
    }

    public FlyConfig config() { return config; }
    public DataStore data() { return data; }
    public GlobalState global() { return global; }
    public EconomyHook economy() { return economy; }
    public ZoneChecker zones() { return zones; }
    public FlyManager fly() { return fly; }
}
