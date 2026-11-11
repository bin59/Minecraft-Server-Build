package com.nangua.offlinereward.listener;

import com.nangua.offlinereward.OfflineReward;
import com.nangua.offlinereward.reward.Delivery;
import com.nangua.offlinereward.reward.OnlineRewardCalculator;
import com.nangua.offlinereward.reward.RewardCalculator;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * 玩家事件（1.1.3 起双模式）：
 *  - stages（默认）：按「今日累计在线时长」分阶段发放固定币数，达成最后一阶段当天毕业；
 *    分次上线可累计时长，默认阶段表累计在线 3 小时（180 分钟）领满后毕业。
 *  - cycle（1.1.2）：按 cycle-minutes 序列循环结算，按真实在线毫秒精确计价。
 *  - 退出时记 last_logout，并把本段在线时长结算 / 累计（写入邮箱，下次上线领取）。
 *  - tick() 由定时任务（每分钟）调用，按模式分别判定。
 */
public final class PlayerListener implements Listener {

    private final OfflineReward plugin;

    public PlayerListener(OfflineReward plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        var storage = plugin.storage();
        if (storage == null) return;
        long now = System.currentTimeMillis();

        // —— 离线奖励（原有） ——
        if (!plugin.isExempt(p)) {
            long lastLogout = storage.getLastLogout(p.getUniqueId());
            if (lastLogout > 0) {
                long offlineMillis = now - lastLogout;
                long lastReward = storage.getLastReward(p.getUniqueId());
                RewardCalculator calc = new RewardCalculator(plugin.getConfig());
                if (!calc.inCooldown(now, lastReward)) {
                    long amount = calc.compute(offlineMillis);
                    if (amount > 0 && Delivery.deliver(p, amount, offlineMillis)) {
                        storage.setLastReward(p.getUniqueId(), now);
                        if (plugin.getConfig().getBoolean("announce", true)) {
                            p.sendMessage("§6[离线奖励] §f你离线了 §e" + format(offlineMillis)
                                    + "§f，已发放 §e" + amount + " §f南瓜币到邮箱，输入 §e/mailbox §f领取。");
                        }
                    }
                }
            }
        }

        // —— 在线奖励：跨天重置 + 记录本次在线起点 ——
        resetDailyIfNeeded(p);
        OnlineRewardCalculator calc = new OnlineRewardCalculator(plugin.getConfig());
        if (calc.isStages()) {
            storage.setLastLogin(p.getUniqueId(), now);
            storage.setNextOnlineSettle(p.getUniqueId(), 0);
        } else {
            startCycle(p, now);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        var storage = plugin.storage();
        if (storage == null) return;
        Player p = e.getPlayer();
        OnlineRewardCalculator calc = new OnlineRewardCalculator(plugin.getConfig());
        // 退出时结算本段：阶段模式累计时长并检查达标阶段；循环模式按真实时长发。均静默。
        if (!plugin.isExempt(p)) {
            if (calc.isStages()) settleStages(p, System.currentTimeMillis(), false, true);
            else settleCycle(p, false, true);
        }
        storage.setLastLogout(p.getUniqueId(), System.currentTimeMillis());
        storage.setLastLogin(p.getUniqueId(), 0);          // 清在线起点
        storage.setNextOnlineSettle(p.getUniqueId(), 0);   // 清下次结算时间
        if (!calc.isStages()) {
            storage.setOnlineCycleIndex(p.getUniqueId(), 0); // 循环模式：下次上线重新从第 1 段起算
        }
        // 阶段模式保留 online_cycle_index（= 今日已领阶段数），分次上线可继续推进
    }

    /** 定时任务（每分钟）：按模式判定是否该给该玩家发奖。 */
    public void tick() {
        var storage = plugin.storage();
        if (storage == null) return;
        OnlineRewardCalculator calc = new OnlineRewardCalculator(plugin.getConfig());
        if (!calc.enabled()) return;
        long now = System.currentTimeMillis();
        List<Player> online = List.copyOf(Bukkit.getOnlinePlayers());
        for (Player p : online) {
            if (plugin.isExempt(p)) continue;
            if (calc.isStages()) {
                settleStages(p, now, true, false);
            } else {
                long due = storage.getNextOnlineSettle(p.getUniqueId());
                if (due <= 0) { startCycle(p, now); continue; } // 重载后已在线玩家补初始化
                if (now >= due) settleCycle(p, true, false);
            }
        }
    }

    // ==================== 阶段发放（1.1.3 默认） ====================

    /**
     * 按今日累计在线时长发放阶段奖励。达成最后一阶段后当天不再发放。
     * @param announce true（定时）发聊天提示；false（退出）静默
     * @param onQuit   true 时把本段时长计入今日累计并清在线起点，但不清已领阶段数
     */
    private void settleStages(Player p, long now, boolean announce, boolean onQuit) {
        var storage = plugin.storage();
        if (storage == null) return;
        OnlineRewardCalculator calc = new OnlineRewardCalculator(plugin.getConfig());
        resetDailyIfNeeded(p);
        long lastLogin = storage.getLastLogin(p.getUniqueId());
        if (lastLogin <= 0) {
            if (!onQuit) storage.setLastLogin(p.getUniqueId(), now);
            return;
        }
        long segment = Math.max(0L, now - lastLogin);
        long total = storage.getOnlineMinutesToday(p.getUniqueId()) + segment;
        int idx = storage.getOnlineCycleIndex(p.getUniqueId());
        boolean announceOn = announce && plugin.getConfig().getBoolean("announce", true);

        if (idx >= calc.stageCount()) {
            // 今日阶段已全部领完：只维护在线起点，不再累计/发币
            storage.setLastLogin(p.getUniqueId(), onQuit ? 0L : now);
            return;
        }

        while (idx < calc.stageCount()) {
            OnlineRewardCalculator.Stage s = calc.stageAt(idx);
            if (total < s.minutes * 60000L) break;
            long coins = s.coins;
            long todayAlready = storage.getOnlineRewardToday(p.getUniqueId());
            if (calc.hasDailyCap()) {
                long room = calc.maxPerDay() - todayAlready;
                if (room <= 0) break;
                coins = Math.min(coins, room);
            }
            if (!Delivery.deliver(p, coins, s.minutes * 60000L, "online")) break;
            storage.setOnlineRewardToday(p.getUniqueId(), todayAlready + coins);
            idx++;
            storage.setOnlineCycleIndex(p.getUniqueId(), idx);
            if (announceOn) {
                p.sendMessage("§a[在线奖励] §f今日第 §e" + idx + "/" + calc.stageCount()
                        + " §f阶段达成（累计在线 " + format(s.minutes * 60000L) + "），已发放 §e" + coins
                        + " §f南瓜币到邮箱，输入 §e/mailbox §f领取。");
            }
        }
        // 本段计入今日累计；重置在线起点避免重复计时
        storage.setOnlineMinutesToday(p.getUniqueId(), total);
        storage.setLastLogin(p.getUniqueId(), onQuit ? 0L : now);
    }

    // ==================== 循环发放（1.1.2，mode: cycle 可回退） ====================

    /** 起算一轮循环：写在线起点、循环下标 0、按第 0 段间隔设置下次结算时间。 */
    private void startCycle(Player p, long now) {
        var storage = plugin.storage();
        if (storage == null) return;
        OnlineRewardCalculator calc = new OnlineRewardCalculator(plugin.getConfig());
        storage.setLastLogin(p.getUniqueId(), now);
        storage.setOnlineCycleIndex(p.getUniqueId(), 0);
        storage.setNextOnlineSettle(p.getUniqueId(), now + calc.cycleMinutesAt(0) * 60000L);
    }

    /**
     * 循环模式：结算当前在线段，按真实在线毫秒精确计价并结转零头。
     * @param onQuit true = 退出结算：跳过 min-minutes 门槛、不结转、不推进循环
     */
    private void settleCycle(Player p, boolean announce, boolean onQuit) {
        var storage = plugin.storage();
        if (storage == null) return;
        long now = System.currentTimeMillis();
        long lastLogin = storage.getLastLogin(p.getUniqueId());
        if (lastLogin <= 0) {
            if (!onQuit) startCycle(p, now);
            return;
        }
        long onlineMillis = now - lastLogin;
        resetDailyIfNeeded(p);
        long todayAlready = storage.getOnlineRewardToday(p.getUniqueId());
        OnlineRewardCalculator calc = new OnlineRewardCalculator(plugin.getConfig());
        long amount = calc.compute(onlineMillis, todayAlready, onQuit);
        if (amount <= 0) {
            if (!onQuit) advanceCycle(p, now, calc);
            return;
        }
        if (Delivery.deliver(p, amount, onlineMillis, "online")) {
            storage.setOnlineRewardToday(p.getUniqueId(), todayAlready + amount);
            long nextLogin = now;
            if (!calc.hasDailyCap() && !onQuit) {
                long consumed = calc.millisPerCoins(amount);
                if (consumed > 0) nextLogin = lastLogin + Math.min(consumed, onlineMillis);
            }
            storage.setLastLogin(p.getUniqueId(), nextLogin);
            if (onQuit) return;
            advanceCycle(p, now, calc);
            if (announce && plugin.getConfig().getBoolean("announce", true)) {
                p.sendMessage("§a[在线奖励] §f你在线了 §e" + format(onlineMillis)
                        + "§f，已发放 §e" + amount + " §f南瓜币到邮箱，输入 §e/mailbox §f领取。");
            }
        } else if (!onQuit) {
            advanceCycle(p, now, calc);
        }
    }

    /** 循环下标 +1，按新一段的间隔设置「下次结算时间」。 */
    private void advanceCycle(Player p, long now, OnlineRewardCalculator calc) {
        var storage = plugin.storage();
        if (storage == null) return;
        int next = (storage.getOnlineCycleIndex(p.getUniqueId()) + 1) % Math.max(1, calc.cycleSize());
        storage.setOnlineCycleIndex(p.getUniqueId(), next);
        storage.setNextOnlineSettle(p.getUniqueId(), now + calc.cycleMinutesAt(next) * 60000L);
    }

    // ==================== 公共 ====================

    /** 跨天则重置在线每日累计、阶段进度与日期标记。 */
    private void resetDailyIfNeeded(Player p) {
        var storage = plugin.storage();
        if (storage == null) return;
        long today = todayLong();
        if (storage.getOnlineDate(p.getUniqueId()) != today) {
            storage.setOnlineRewardToday(p.getUniqueId(), 0L);
            storage.setOnlineMinutesToday(p.getUniqueId(), 0L);
            storage.setOnlineCycleIndex(p.getUniqueId(), 0);
            storage.setOnlineDate(p.getUniqueId(), today);
        }
    }

    private static long todayLong() {
        LocalDate d = LocalDate.now(ZoneId.systemDefault());
        return d.getYear() * 10000L + d.getMonthValue() * 100L + d.getDayOfMonth();
    }

    private static String format(long millis) {
        long h = millis / 3600_000L;
        long m = (millis % 3600_000L) / 60000L;
        if (h > 0) return h + "小时" + (m > 0 ? m + "分" : "");
        return m + "分钟";
    }
}
