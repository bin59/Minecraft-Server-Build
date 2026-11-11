package com.nangua.offlinereward.reward;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 在线奖励计奖（1.1.3 起双模式）：
 *  - stages（默认）：按「今日累计在线时长」分阶段发放固定币数，达成最后一阶段即毕业，
 *    当天不再发放。默认阶段表累计在线 3 小时（180 分钟）领满、全天合计 300 币；分次上线可累计。
 *  - cycle（1.1.2 行为）：按 cycle-minutes 序列循环结算，按真实在线毫秒精确计价
 *    （coins-per-hour，默认 25 币/小时），持续发放直到触及 max-per-day 上限。
 * 两种模式共用 max-per-day 保险（当前 300 币/天）与 /offlinereward check 诊断。
 */
public final class OnlineRewardCalculator {

    /** 阶段：今日累计在线达到 minutes 分钟 → 发 coins 币。 */
    public static final class Stage {
        public final long minutes;
        public final long coins;

        public Stage(long minutes, long coins) {
            this.minutes = minutes;
            this.coins = coins;
        }
    }

    private final boolean enabled;
    private final boolean stagesMode;
    private final double coinsPerHour;
    private final long minMinutes;
    private final long intervalMinutes;
    private final long maxPerDay;
    private final List<Long> cycleMinutes;
    private final List<Stage> stages;

    public OnlineRewardCalculator(FileConfiguration c) {
        this.enabled = c.getBoolean("online-reward.enabled", true);
        String m = c.getString("online-reward.mode", "stages");
        this.stagesMode = !"cycle".equalsIgnoreCase(m == null ? "" : m.trim());
        this.coinsPerHour = c.getDouble("online-reward.coins-per-hour", 25.0);
        this.minMinutes = c.getLong("online-reward.min-minutes", 5L);
        this.intervalMinutes = Math.max(1L, c.getLong("online-reward.interval-minutes", 5L));
        this.maxPerDay = c.getLong("online-reward.max-per-day", 0L);

        // cycle 模式：循环间隔序列；未配置时退化为固定 interval-minutes
        List<Long> cycle = new ArrayList<>();
        for (Long v : c.getLongList("online-reward.cycle-minutes")) {
            if (v != null && v >= 1L) cycle.add(v);
        }
        if (cycle.isEmpty()) cycle.add(intervalMinutes);
        this.cycleMinutes = List.copyOf(cycle);

        // stages 模式：阶段表（累计分钟 → 币数）；缺省/非法时回落到内置 3 阶段
        List<Stage> st = new ArrayList<>();
        for (Map<?, ?> row : c.getMapList("online-reward.stages")) {
            long minutes = toLong(row.get("minutes"));
            long coins = toLong(row.get("coins"));
            if (minutes >= 1L && coins >= 1L) st.add(new Stage(minutes, coins));
        }
        if (st.isEmpty()) {
            st.add(new Stage(10L, 10L));
            st.add(new Stage(30L, 20L));
            st.add(new Stage(60L, 30L));
        }
        this.stages = List.copyOf(st);
        // 1.1.1 起 floor-minutes 弃用；保留仅为兼容旧配置。
    }

    private static long toLong(Object o) {
        if (o instanceof Number n) return n.longValue();
        if (o instanceof String s) {
            try { return Long.parseLong(s.trim()); } catch (NumberFormatException ignored) { }
        }
        return 0L;
    }

    public boolean enabled() { return enabled; }
    public boolean isStages() { return stagesMode; }
    public String modeText() { return stagesMode ? "阶段发放" : "循环发放"; }
    public long maxPerDay() { return maxPerDay; }
    public long intervalMinutes() { return intervalMinutes; }
    public boolean hasDailyCap() { return maxPerDay > 0; }

    // —— 阶段模式 ——
    public List<Stage> stages() { return stages; }
    public int stageCount() { return stages.size(); }

    public Stage stageAt(int index) {
        int i = Math.max(0, Math.min(index, stages.size() - 1));
        return stages.get(i);
    }

    /** 每日阶段奖励合计（币）。 */
    public long totalStageCoins() {
        long sum = 0L;
        for (Stage s : stages) sum += s.coins;
        return sum;
    }

    /** 最后一阶段的累计在线门槛（分钟），即「毕业时长」。 */
    public long graduateMinutes() {
        return stages.get(stages.size() - 1).minutes;
    }

    /** 阶段表可读描述，用于启动日志与 /offlinereward check。 */
    public String stagesText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < stages.size(); i++) {
            if (i > 0) sb.append(" → ");
            sb.append(stages.get(i).minutes).append("分/").append(stages.get(i).coins).append("币");
        }
        return sb.toString();
    }

    // —— 循环模式 ——
    public int cycleSize() { return cycleMinutes.size(); }

    public long cycleMinutesAt(int index) {
        int n = cycleMinutes.size();
        int i = ((index % n) + n) % n;
        return cycleMinutes.get(i);
    }

    public String cycleText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cycleMinutes.size(); i++) {
            if (i > 0) sb.append("/");
            sb.append(cycleMinutes.get(i));
        }
        return sb.append(" 分钟循环").toString();
    }

    /** 发放 coins 币对应的在线毫秒数（循环模式零头结转用）。 */
    public long millisPerCoins(long coins) {
        if (coins <= 0 || coinsPerHour <= 0) return 0L;
        return (long) Math.floor(coins * 3_600_000.0 / coinsPerHour);
    }

    /** 循环模式计奖（按真实在线毫秒精确计价）。 */
    public long compute(long onlineMillis, long todayAlready) {
        return compute(onlineMillis, todayAlready, false);
    }

    /**
     * @param skipMin true = 跳过 min-minutes 门槛（退出结算用）
     */
    public long compute(long onlineMillis, long todayAlready, boolean skipMin) {
        if (!enabled) return 0;
        if (onlineMillis <= 0) return 0;
        long minutes = onlineMillis / 60000L;
        if (!skipMin && minutes < minMinutes) return 0;

        long amount = (long) Math.floor((onlineMillis / 3_600_000.0) * coinsPerHour);
        if (amount <= 0) return 0;
        if (maxPerDay > 0) {
            long room = maxPerDay - todayAlready;
            if (room <= 0) return 0;
            if (amount > room) amount = room;
        }
        return amount;
    }
}
