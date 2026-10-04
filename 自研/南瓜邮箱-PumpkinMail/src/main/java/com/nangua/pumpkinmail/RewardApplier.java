package com.nangua.pumpkinmail;

import com.nangua.pumpkinmail.storage.Reward;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * 奖励发放：以控制台命令方式执行，兼容 Java 与基岩玩家。
 *  - money   -> eco give <玩家> <金额>            （EssentialsX/Vault 加钱，即南瓜币）
 *  - item    -> give <玩家> <材质> <数量>
 *  - command -> 自定义命令，{player} 占位符替换为玩家名
 */
public class RewardApplier {

    private final Plugin plugin;

    public RewardApplier(Plugin plugin) {
        this.plugin = plugin;
    }

    /** 返回是否成功执行。 */
    public boolean apply(Player player, Reward reward) {
        String cmd = buildCommand(player, reward);
        if (cmd == null) return false;
        return plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), cmd);
    }

    private String buildCommand(Player player, Reward r) {
        return switch (r.rewardType()) {
            case "money" -> "eco give " + player.getName() + " " + r.data();
            case "item" -> {
                String[] p = r.data().split(":", 2);
                String mat = p[0].trim();
                int amt = (p.length > 1) ? safeInt(p[1], 1) : 1;
                yield "give " + player.getName() + " " + mat + " " + amt;
            }
            case "command" -> r.data().replace("{player}", player.getName());
            default -> null;
        };
    }

    private int safeInt(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }
}
