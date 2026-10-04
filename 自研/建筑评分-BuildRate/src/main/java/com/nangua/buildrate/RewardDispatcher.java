package com.nangua.buildrate;

import com.nangua.buildrate.storage.Models;
import com.nangua.buildrate.storage.Stats;
import com.nangua.pumpkinmail.PumpkinMail;
import com.nangua.pumpkinmail.storage.Reward;
import com.earth2me.essentials.CommandSource;
import net.ess3.api.events.UserBalanceUpdateEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 活动结算发奖：按名次给前 N 名发奖。
 *  - money   -> 在线玩家经 Essentials API 发币（账本记为「活动参与」）；离线玩家回退 eco give
 *  - item    -> 入队到南瓜邮箱（玩家 /mailbox 领取）
 *  - command -> 控制台执行自定义命令
 */
public final class RewardDispatcher {

    private final BuildRate plugin;

    public RewardDispatcher(BuildRate plugin) {
        this.plugin = plugin;
    }

    public boolean enabled() {
        return plugin.getConfig().getBoolean("rewards.enabled", true);
    }

    /** 结算某活动：计算排名并按名次发奖，可广播前 3 名。 */
    public void settleEvent(Models.Event ev) {
        if (!enabled()) return;
        List<Models.Build> builds = plugin.db().getBuilds(ev.id());
        Map<Integer, List<Models.Score>> scores = plugin.db().getScoresByBuild(ev.id());
        List<Models.BuildResult> results = Stats.compute(builds, scores, plugin.dropHiLo());
        if (results.isEmpty()) return;

        int rank = 1;
        for (Models.BuildResult r : results) {
            giveRank(r.build().player(), rank);
            rank++;
        }

        if (plugin.getConfig().getBoolean("rewards.announce", true)) {
            announceTop(results);
        }
    }

    private void giveRank(String playerName, int rank) {
        List<Map<?, ?>> list = plugin.getConfig().getMapList("rewards.ranks." + rank);
        if (list == null || list.isEmpty()) return;
        for (Map<?, ?> m : list) {
            String type = str(m.get("type"));
            String data = str(m.get("data"));
            String desc = str(m.get("desc"));
            if (type == null || data == null) continue;
            switch (type) {
                case "money" -> giveMoney(playerName, data);
                case "item" -> mailItem(playerName, data, desc, rank);
                case "command" -> dispatch(data, playerName);
                default -> { }
            }
        }
        Player p = Bukkit.getPlayerExact(playerName);
        if (p != null && p.isOnline()) {
            p.sendMessage(ChatColor.LIGHT_PURPLE + "[建筑评分] 你获得第 " + rank + " 名奖励，已发放（物品在 /mailbox 领取）。");
        }
    }

    /** 发南瓜币：在线玩家走 Essentials API（账本记「活动参与」），离线回退 eco give。 */
    private void giveMoney(String playerName, String data) {
        double amt;
        try {
            amt = Double.parseDouble(data.trim());
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("奖励 money 金额非法：" + data);
            return;
        }
        Player p = Bukkit.getPlayerExact(playerName);
        com.earth2me.essentials.Essentials ess =
                (com.earth2me.essentials.Essentials) Bukkit.getPluginManager().getPlugin("Essentials");
        if (ess != null && p != null && p.isOnline()) {
            try {
                com.earth2me.essentials.User u = (com.earth2me.essentials.User) ess.getUser(p);
                CommandSource cs = new CommandSource(ess, Bukkit.getConsoleSender());
                if (u != null) {
                    // 带 Cause.API 发币 → 南瓜账本按调用栈识别为 com.nangua.buildrate → 记「活动参与」
                    u.giveMoney(BigDecimal.valueOf(amt), cs, UserBalanceUpdateEvent.Cause.API);
                    return;
                }
            } catch (Throwable t) {
                plugin.getLogger().warning("Essentials API 发币失败，回退 eco give：" + t.getMessage());
            }
        }
        dispatch("eco give {player} " + amt, playerName);
    }

    /** 物品奖励入队到南瓜邮箱，玩家用 /mailbox 领取。 */
    private void mailItem(String playerName, String data, String desc, int rank) {
        PumpkinMail pm = PumpkinMail.get();
        if (pm == null || pm.storage() == null) return;
        try {
            String d = (desc == null || desc.isEmpty()) ? "建筑大赛第 " + rank + " 名奖励" : desc;
            pm.storage().addReward(new Reward(
                    "br-" + UUID.randomUUID(), null, playerName, null,
                    "item", data, d, "BuildRate", System.currentTimeMillis(), false));
        } catch (Throwable t) {
            plugin.getLogger().warning("入队南瓜邮箱失败：" + t.getMessage());
        }
    }

    private void dispatch(String cmd, String playerName) {
        if (cmd == null || cmd.isEmpty()) return;
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("{player}", playerName));
    }

    private void announceTop(List<Models.BuildResult> results) {
        Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "===== 建筑大赛结算，获奖名单 =====");
        int shown = 0;
        for (Models.BuildResult r : results) {
            if (shown >= 3) break;
            shown++;
            Bukkit.broadcastMessage(ChatColor.GOLD + "#" + shown + "  " + ChatColor.WHITE + r.build().name()
                    + ChatColor.GRAY + "（" + r.build().player() + "）"
                    + ChatColor.GREEN + " " + String.format("%.1f", r.avg()) + " 分"
                    + ChatColor.GRAY + " · " + r.votes() + "票");
        }
        Bukkit.broadcastMessage(ChatColor.GRAY + "奖励已发放（物品在 /mailbox 领取）。");
    }

    private String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
