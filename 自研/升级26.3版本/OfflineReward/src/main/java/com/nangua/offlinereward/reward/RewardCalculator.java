package com.nangua.offlinereward.reward;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * 计奖规则：离线时长 → 南瓜币数。
 * 金额 = 离线小时 × coins-per-hour，含保底、每日封顶、总封顶、冷却。
 */
public final class RewardCalculator {

    private final double coinsPerHour;
    private final long minHours;        // 保底：离线不足该小时数不发
    private final long maxHoursPerDay;  // 单次最多计入的离线小时
    private final long maxReward;       // 单次奖励总封顶（0 = 不封顶）
    private final long cooldownHours;   // 两次发放最小间隔小时（0 = 不限）
    private final boolean floorMinutes; // true = 不足整小时的分钟舍去

    public RewardCalculator(FileConfiguration c) {
        this.coinsPerHour = c.getDouble("reward.coins-per-hour", 15.0);
        this.minHours = c.getLong("reward.min-hours", 1L);
        this.maxHoursPerDay = c.getLong("reward.max-hours-per-day", 24L);
        this.maxReward = c.getLong("reward.max-reward", 0L);
        this.cooldownHours = c.getLong("reward.cooldown-hours", 0L);
        this.floorMinutes = c.getBoolean("reward.floor-minutes", true);
    }

    /** 返回应发币数；0 = 不发放。offlineMillis 为本次离线毫秒数。 */
    public long compute(long offlineMillis) {
        if (offlineMillis <= 0) return 0;
        long minutes = offlineMillis / 60000L;
        if (minutes <= 0) return 0;
        if (floorMinutes) {
            minutes -= minutes % 60L; // 舍去不足整小时的分钟
        }
        double hours = minutes / 60.0;
        if (hours < minHours) return 0;
        if (hours > maxHoursPerDay) hours = maxHoursPerDay;

        long amount = (long) Math.floor(hours * coinsPerHour);
        if (amount <= 0) return 0;
        if (maxReward > 0 && amount > maxReward) amount = maxReward;
        return amount;
    }

    /** 距上次发放是否仍在冷却期内。 */
    public boolean inCooldown(long now, long lastRewardMillis) {
        if (cooldownHours <= 0 || lastRewardMillis <= 0) return false;
        return (now - lastRewardMillis) < cooldownHours * 3600_000L;
    }
}
