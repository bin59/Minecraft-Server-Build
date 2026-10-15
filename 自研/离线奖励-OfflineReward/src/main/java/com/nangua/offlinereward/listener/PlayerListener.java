package com.nangua.offlinereward.listener;

import com.nangua.offlinereward.OfflineReward;
import com.nangua.offlinereward.reward.Delivery;
import com.nangua.offlinereward.reward.RewardCalculator;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * 退出时记录 last_logout；上线时按离线时长计奖并写入邮箱。
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
        if (plugin.isExempt(p)) return;

        long now = System.currentTimeMillis();
        long lastLogout = storage.getLastLogout(p.getUniqueId());
        if (lastLogout <= 0) return; // 首次上线，无离线记录

        long offlineMillis = now - lastLogout;
        long lastReward = storage.getLastReward(p.getUniqueId());

        RewardCalculator calc = new RewardCalculator(plugin.getConfig());
        if (calc.inCooldown(now, lastReward)) return;

        long amount = calc.compute(offlineMillis);
        if (amount <= 0) return;

        if (Delivery.deliver(p, amount, offlineMillis)) {
            storage.setLastReward(p.getUniqueId(), now);
            if (plugin.getConfig().getBoolean("announce", true)) {
                p.sendMessage("§6[离线奖励] §f你离线了 §e" + format(offlineMillis)
                        + "§f，已发放 §e" + amount + " §f南瓜币到邮箱，输入 §e/mailbox §f领取。");
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        var storage = plugin.storage();
        if (storage != null) {
            storage.setLastLogout(e.getPlayer().getUniqueId(), System.currentTimeMillis());
        }
    }

    private static String format(long millis) {
        long h = millis / 3600_000L;
        long m = (millis % 3600_000L) / 60000L;
        if (h > 0) return h + "小时" + (m > 0 ? m + "分" : "");
        return m + "分钟";
    }
}
