package com.nangua.offlinereward;

import com.nangua.offlinereward.cmd.OfflineRewardCommand;
import com.nangua.offlinereward.listener.PlayerListener;
import com.nangua.offlinereward.storage.Storage;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Level;

/**
 * 离线奖励 OfflineReward
 * 按玩家离线时长，下次上线把南瓜币写入南瓜邮箱（PumpkinMail），玩家 /mailbox 领取。
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

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        var cmd = getCommand("offlinereward");
        if (cmd != null) {
            cmd.setExecutor(new OfflineRewardCommand(this));
        }

        getLogger().info("离线奖励已启用。按离线时长，上线后进入 /mailbox 领取南瓜币。");
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
