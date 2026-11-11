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

        // 1.1.3：在线奖励每分钟检查一次；阶段模式按累计在线时长阶梯发放，循环模式按序列到点结算
        OnlineRewardCalculator calc = new OnlineRewardCalculator(getConfig());
        if (calc.enabled()) {
            long checkTicks = 60L * 20L; // 1 分钟
            getServer().getScheduler().runTaskTimer(this, listener::tick, checkTicks, checkTicks);
            if (calc.isStages()) {
                getLogger().info("离线奖励已启用；在线奖励【阶段发放】" + calc.stagesText()
                        + "（累计在线 " + calc.graduateMinutes() + " 分钟毕业，每日合计 "
                        + calc.totalStageCoins() + " 币，发完当天不再发）。");
            } else {
                getLogger().info("离线奖励已启用；在线奖励【循环发放】按 " + calc.cycleText()
                        + " 结算（单价 " + getConfig().getDouble("online-reward.coins-per-hour", 25.0)
                        + " 币/小时）。");
            }
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

    /**
     * 豁免判定（1.1.1 修复）：只认「显式赋权」为 true 的精确节点。
     * admin 组的 '*' 通配会让 hasPermission("offlinereward.exempt") 返回 true，
     * 但那是通配/继承推断，不是显式节点；改版后管理员不再被误判为豁免，照常领取。
     * 若要豁免某玩家，显式执行 lp ... permission set offlinereward.exempt true。
     */
    public boolean isExempt(Player player) {
        String node = getConfig().getString("exempt-permission", "offlinereward.exempt");
        if (node == null || node.isEmpty()) return false;
        for (org.bukkit.permissions.PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            if (!node.equalsIgnoreCase(info.getPermission())) continue;
            if (info.getAttachment() == null) continue; // 插件默认权限，不算显式赋权
            return Boolean.TRUE.equals(info.getValue());
        }
        return false; // 无精确节点（含 '*' 通配）→ 不豁免
    }
}
