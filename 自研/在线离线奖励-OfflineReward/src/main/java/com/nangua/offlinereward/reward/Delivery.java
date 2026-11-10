package com.nangua.offlinereward.reward;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * 发放：优先写入南瓜邮箱（PumpkinMail），玩家 /mailbox 领取时经其 RewardApplier 执行 eco give。
 * 南瓜邮箱缺失或写入失败时，回退为控制台直接 eco give。
 * source 用于区分离线/在线（"offline" / "online"）。
 */
public final class Delivery {

    /** 兼容旧调用：默认离线奖励。 */
    public static boolean deliver(Player player, long amount, long millis) {
        return deliver(player, amount, millis, "offline");
    }

    public static boolean deliver(Player player, long amount, long millis, String source) {
        boolean online = "online".equals(source);
        String label = online ? "在线奖励" : "离线奖励";
        String desc = label + "：" + (online ? "在线 " : "离线 ") + format(millis) + " → " + amount + " 南瓜币";
        Plugin pm = Bukkit.getPluginManager().getPlugin("PumpkinMail");
        if (pm != null) {
            try {
                var mail = (com.nangua.pumpkinmail.PumpkinMail) pm;
                var storage = mail.storage();
                if (storage == null) return false;
                long now = System.currentTimeMillis();
                var reward = new com.nangua.pumpkinmail.storage.Reward(
                        source + "_" + player.getUniqueId().toString().replace("-", "") + "_" + now,
                        player.getUniqueId().toString(),
                        player.getName(),
                        source,
                        "money",
                        String.valueOf(amount),
                        desc,
                        "OfflineReward",
                        now,
                        false
                );
                storage.addReward(reward);
                return true;
            } catch (Throwable t) {
                player.getServer().getLogger().warning("[OfflineReward] 写入南瓜邮箱失败，回退直发：" + t);
            }
        }
        return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "eco give " + player.getName() + " " + amount);
    }

    private static String format(long millis) {
        long h = millis / 3600_000L;
        long m = (millis % 3600_000L) / 60000L;
        if (h > 0) return h + "小时" + (m > 0 ? m + "分" : "");
        return m + "分钟";
    }
}
