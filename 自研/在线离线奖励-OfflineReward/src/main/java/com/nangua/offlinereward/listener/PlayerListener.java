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
 * 玩家事件：
 *  - 退出时记 last_logout，并结算本段在线时长（写入邮箱，下次上线领取）。
 *  - 上线时按离线时长计离线奖励，并记录本次在线起点 last_login。
 *  - tick() 由定时任务调用，遍历在线玩家结算在线段（滚动 last_login）。
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

        // —— 在线奖励：记录本次在线起点 + 跨天重置每日累计 ——
        resetDailyIfNeeded(p);
        storage.setLastLogin(p.getUniqueId(), now);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        var storage = plugin.storage();
        if (storage == null) return;
        Player p = e.getPlayer();
        // 退出时结算本段在线时长（写入邮箱，玩家下次上线领取；玩家已离线不再发聊天提示）
        if (!plugin.isExempt(p)) {
            settleOnline(p, false);
        }
        storage.setLastLogout(p.getUniqueId(), System.currentTimeMillis());
        storage.setLastLogin(p.getUniqueId(), 0); // 清在线起点
    }

    /** 定时任务：遍历在线玩家结算在线段。 */
    public void tick() {
        if (plugin.storage() == null) return;
        OnlineRewardCalculator calc = new OnlineRewardCalculator(plugin.getConfig());
        if (!calc.enabled()) return;
        List<Player> online = List.copyOf(Bukkit.getOnlinePlayers());
        for (Player p : online) {
            if (plugin.isExempt(p)) continue;
            settleOnline(p, true);
        }
    }

    /**
     * 结算玩家当前在线段。announce=true（定时）时发聊天提示；false（退出）时静默写入邮箱。
     * 结算成功后把 last_login 滚动到 now，避免重复计发。
     */
    private void settleOnline(Player p, boolean announce) {
        var storage = plugin.storage();
        if (storage == null) return;
        long now = System.currentTimeMillis();
        long lastLogin = storage.getLastLogin(p.getUniqueId());
        if (lastLogin <= 0) {
            storage.setLastLogin(p.getUniqueId(), now);
            return;
        }
        long onlineMillis = now - lastLogin;
        resetDailyIfNeeded(p);
        long todayAlready = storage.getOnlineRewardToday(p.getUniqueId());
        long amount = new OnlineRewardCalculator(plugin.getConfig()).compute(onlineMillis, todayAlready);
        if (amount <= 0) return;
        if (Delivery.deliver(p, amount, onlineMillis, "online")) {
            storage.setOnlineRewardToday(p.getUniqueId(), todayAlready + amount);
            storage.setLastLogin(p.getUniqueId(), now); // 滚动段起点
            if (announce && plugin.getConfig().getBoolean("announce", true)) {
                p.sendMessage("§a[在线奖励] §f你在线了 §e" + format(onlineMillis)
                        + "§f，已发放 §e" + amount + " §f南瓜币到邮箱，输入 §e/mailbox §f领取。");
            }
        }
    }

    /** 跨天则重置在线每日累计与日期标记。 */
    private void resetDailyIfNeeded(Player p) {
        var storage = plugin.storage();
        if (storage == null) return;
        long today = todayLong();
        if (storage.getOnlineDate(p.getUniqueId()) != today) {
            storage.setOnlineRewardToday(p.getUniqueId(), 0L);
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
