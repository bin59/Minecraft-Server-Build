package com.nangua.offlinereward;

import com.nangua.offlinereward.cmd.OfflineRewardCommand;
import com.nangua.offlinereward.listener.PlayerListener;
import com.nangua.offlinereward.reward.OnlineRewardCalculator;
import com.nangua.offlinereward.storage.Storage;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Level;

/**
 * 离线奖励 + 在线奖励 OfflineReward
 * 离线奖励：按玩家离线时长，下次上线把南瓜币写入南瓜邮箱（PumpkinMail），玩家 /mailbox 领取。
 * 在线奖励：玩家在线期间定期结算，把南瓜币写入南瓜邮箱。
 * 南瓜邮箱缺失时回退为直接 eco give。
 */
public final class OfflineReward extends JavaPlugin {

    private static OfflineReward instance;
    private Storage storage;

    public static OfflineReward get() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        try {
            storage = new Storage(new File(getDataFolder(), getConfig().getString("db", "offline.db")));
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "无法初始化数据库，插件停用。", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        PlayerListener listener = new PlayerListener(this);
        getServer().getPluginManager().registerEvents(listener, this);

        // 在线奖励：定时结算（每 interval-minutes 一次）
        long interval = new OnlineRewardCalculator(getConfig()).intervalMinutes();
        if (interval > 0) {
            long ticks = Math.max(1L, interval * 60L * 20L);
            getServer().getScheduler().runTaskTimer(this, listener::tick, ticks, ticks);
            getLogger().info("离线奖励已启用；在线奖励定时结算已启用，每 " + interval + " 分钟一次。");
        } else {
            getLogger().info("离线奖励已启用（在线奖励未启用）。");
        }

        var cmd = getCommand("offlinereward");
        if (cmd != null) {
            cmd.setExecutor(new OfflineRewardCommand(this));
        }
    }

    @Override
    public void onDisable() {
        if (storage != null) {
            try { storage.close(); } catch (Exception ignored) { }
        }
        instance = null;
    }

    public Storage storage() {
        return storage;
    }

    /** 拥有豁免权限的玩家不发放（管理员/测试号）。 */
    public boolean isExempt(Player player) {
        return player.hasPermission(getConfig().getString("exempt-permission", "offlinereward.exempt"));
    }
}
