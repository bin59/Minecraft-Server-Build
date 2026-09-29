package com.mc.entitycount;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 实体 / 生物计数占位符。
 *
 * 背景：PAPI Server 扩展的 %server_total_entities% / %server_total_living_entities%
 * 在请求时同步遍历 chunk 取实体，TAB 又在异步线程（TAB Placeholder Refreshing Thread）
 * 刷新占位符，Leaf/Paper 的 AsyncCatcher 会直接拦下（"Chunk getEntities call"），
 * 导致生物数取不到、并周期性刷屏 ERROR。
 *
 * 本扩展：主线程每 5 秒统计一次全服实体数并缓存到内存，
 * 占位符请求只读缓存（纯 int，异步安全零开销）。
 *
 * 占位符：
 *   %entitycount_count%  - 全部已加载实体数（含物品掉落物，Paper getEntityCount O(1)）
 *   %entitycount_living% - 生物数（主线程遍历）
 */
public final class EntityCountPlugin extends JavaPlugin {

    private volatile int entityCount = 0;
    private volatile int livingCount = 0;

    @Override
    public void onEnable() {
        // 每 5 秒在主线程统计一次并缓存（Bukkit API 必须主线程调用）
        Bukkit.getScheduler().runTaskTimer(this, this::refresh, 20L, 100L);
        // 注册 PAPI 占位符扩展
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new EntityCountExpansion().register();
        }
        getLogger().info("实体计数占位符已启用：%entitycount_count% / %entitycount_living%");
    }

    private void refresh() {
        int entities = 0;
        int living = 0;
        for (World world : Bukkit.getWorlds()) {
            entities += world.getEntityCount();
            living += world.getLivingEntities().size();
        }
        entityCount = entities;
        livingCount = living;
    }

    int getEntityCount() {
        return entityCount;
    }

    int getLivingCount() {
        return livingCount;
    }

    /** PAPI 占位符扩展：只读内存缓存，任何线程安全 */
    private final class EntityCountExpansion extends PlaceholderExpansion {

        @Override
        public String getIdentifier() {
            return "entitycount";
        }

        @Override
        public String getAuthor() {
            return "PumpkinServer";
        }

        @Override
        public String getVersion() {
            return "1.0.0";
        }

        @Override
        public boolean persist() {
            return true;
        }

        @Override
        public String onRequest(OfflinePlayer player, String params) {
            if ("count".equalsIgnoreCase(params)) {
                return String.valueOf(getEntityCount());
            }
            if ("living".equalsIgnoreCase(params)) {
                return String.valueOf(getLivingCount());
            }
            return null;
        }
    }
}
