package com.nangua.offlinereward.reward;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * 在线奖励计奖：玩家在线时长 → 南瓜币。
 * 与离线奖励对称，但按「分钟段」结算（每 interval-minutes 一个整段），单价通常更高以鼓励在线；
 * 支持每日封顶（max-per-day，0 = 不封顶）。
 */
public final class OnlineRewardCalculator {

    private final boolean enabled;
    private final double coinsPerHour;
    private final long minMinutes;
    private final long intervalMinutes;
    private final long maxPerDay;
    private final boolean floorMinutes;

    public OnlineRewardCalculator(FileConfiguration c) {
        this.enabled = c.getBoolean("online-reward.enabled", true);
        this.coinsPerHour = c.getDouble("online-reward.coins-per-hour", 25.0);
        this.minMinutes = c.getLong("online-reward.min-minutes", 5L);
        this.intervalMinutes = Math.max(1L, c.getLong("online-reward.interval-minutes", 5L));
        this.maxPerDay = c.getLong("online-reward.max-per-day", 0L);
        this.floorMinutes = c.getBoolean("online-reward.floor-minutes", true);
    }

    public boolean enabled() { return enabled; }
    public long intervalMinutes() { return intervalMinutes; }

    /**
     * 返回应发币数；0 = 不发放。
     * @param onlineMillis  本次待结算在线毫秒数
     * @param todayAlready 本日已发在线奖励币数（用于每日封顶）
     */
    public long compute(long onlineMillis, long todayAlready) {
        if (!enabled) return 0;
        if (onlineMillis <= 0) return 0;
        long minutes = onlineMillis / 60000L;
        if (minutes <= 0) return 0;
        // 按结算间隔取整段（只发整 interval 段，避免碎片化小额）
        if (floorMinutes) {
            long rem = minutes % intervalMinutes;
            minutes -= rem;
        }
        if (minutes < minMinutes) return 0;

        long amount = (long) Math.floor((minutes / 60.0) * coinsPerHour);
        if (amount <= 0) return 0;
        if (maxPerDay > 0) {
            long room = maxPerDay - todayAlready;
            if (room <= 0) return 0;
            if (amount > room) amount = room;
        }
        return amount;
    }
}
