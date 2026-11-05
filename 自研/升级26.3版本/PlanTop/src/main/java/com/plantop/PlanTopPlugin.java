package com.plantop;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * PlanTop - 从 Plan 数据库读取在线时间排行，注册 %ptop_N_*% 占位符。
 * 数据源与 Plan 一致（plan_sessions 聚合），保留全部历史在线时间。
 */
public class PlanTopPlugin extends JavaPlugin {

    private PlaytimeCache cache;
    private PlanTopExpansion expansion;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        cache = new PlaytimeCache(this);
        cache.start();

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            expansion = new PlanTopExpansion(this);
            expansion.register();
            getLogger().info("PAPI 占位符已注册: %ptop_<N>_name% / %ptop_<N>_time% / %ptop_<N>_displayname%");
        } else {
            getLogger().warning("未检测到 PlaceholderAPI，占位符不可用（数据仍会定时缓存）");
        }
    }

    @Override
    public void onDisable() {
        if (cache != null) cache.stop();
    }

    public PlaytimeCache getCache() {
        return cache;
    }
}
