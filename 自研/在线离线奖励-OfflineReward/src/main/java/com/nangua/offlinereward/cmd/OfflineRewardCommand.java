package com.nangua.offlinereward.cmd;

import com.nangua.offlinereward.OfflineReward;
import com.nangua.offlinereward.reward.OnlineRewardCalculator;
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
            var st = plugin.storage();
            long last = st == null ? 0 : st.getLastLogout(target.getUniqueId());
            long reward = st == null ? 0 : st.getLastReward(target.getUniqueId());
            long today = st == null ? 0 : st.getOnlineRewardToday(target.getUniqueId());
            long login = st == null ? 0 : st.getLastLogin(target.getUniqueId());
            long due = st == null ? 0 : st.getNextOnlineSettle(target.getUniqueId());
            int idx = st == null ? 0 : st.getOnlineCycleIndex(target.getUniqueId());
            OnlineRewardCalculator calc = new OnlineRewardCalculator(plugin.getConfig());
            sender.sendMessage("§7[离线奖励] " + args[1] + " 最后退出:" + (last > 0 ? last : "无记录")
                    + "  上次发放:" + (reward > 0 ? reward : "无"));
            sender.sendMessage("§7[在线奖励] 豁免:" + (plugin.isExempt(target) ? "§c是（不发）" : "§a否（正常发放）")
                    + "  今日已发:" + today + " 币  在线起点:" + (login > 0 ? login : "无"));
            if (calc.isStages()) {
                long total = (st == null ? 0 : st.getOnlineMinutesToday(target.getUniqueId()))
                        + (login > 0 ? Math.max(0, System.currentTimeMillis() - login) : 0);
                String next = idx < calc.stageCount()
                        ? calc.stageAt(idx).minutes + " 分钟 / " + calc.stageAt(idx).coins + " 币"
                        : "今日已毕业（不再发放）";
                sender.sendMessage("§7[在线奖励] 模式:阶段发放  " + calc.stagesText()
                        + "  今日累计在线:" + (total / 60000L) + " 分钟  已领 " + idx + "/"
                        + calc.stageCount() + " 阶段  下一阶段:" + next);
            } else {
                long waitMin = due > 0 ? Math.max(0, (due - System.currentTimeMillis() + 59999L) / 60000L) : 0;
                sender.sendMessage("§7[在线奖励] 模式:循环发放  间隔:" + calc.cycleText()
                        + "  当前第 " + (idx + 1) + " 段  距下次结算约:" + waitMin + " 分钟");
            }
            return true;
        }
        sender.sendMessage("§e/offlinereward reload §7| §e/offlinereward check <玩家>");
        return true;
    }
}
