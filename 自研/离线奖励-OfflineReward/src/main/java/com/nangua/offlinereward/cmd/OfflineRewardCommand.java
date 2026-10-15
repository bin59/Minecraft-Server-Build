package com.nangua.offlinereward.cmd;

import com.nangua.offlinereward.OfflineReward;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class OfflineRewardCommand implements CommandExecutor {

    private final OfflineReward plugin;

    public OfflineRewardCommand(OfflineReward plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            sender.sendMessage("§a[离线奖励] 配置已重载。");
            return true;
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("check")) {
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage("§c玩家不在线，无法查看（check 仅限在线玩家）。");
                return true;
            }
            long last = plugin.storage() == null ? 0 : plugin.storage().getLastLogout(target.getUniqueId());
            long reward = plugin.storage() == null ? 0 : plugin.storage().getLastReward(target.getUniqueId());
            sender.sendMessage("§7[离线奖励] " + args[1] + " 最后退出:" + (last > 0 ? last : "无记录")
                    + "  上次发放:" + (reward > 0 ? reward : "无"));
            return true;
        }
        sender.sendMessage("§e/offlinereward reload §7| §e/offlinereward check <玩家>");
        return true;
    }
}
