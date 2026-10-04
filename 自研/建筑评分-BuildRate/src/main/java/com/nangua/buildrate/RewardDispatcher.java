package com.nangua.buildrate;

import com.nangua.buildrate.storage.Models;
import com.nangua.buildrate.storage.Stats;
import com.nangua.pumpkinmail.PumpkinMail;
import com.nangua.pumpkinmail.storage.Reward;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 活动结算发奖：按名次给前 N 名发奖。
 * 所有奖励（南瓜币 money、物品 item）一律入队到南瓜邮箱，玩家 /mailbox 领取；
 * 南瓜币在领取时由邮箱执行 eco give，并被南瓜账本按奖励描述（含活动名）记为来源。
 * command 类型仍直接控制台执行。
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

        String en = ev.name();
        int rank = 1;
        for (Models.BuildResult r : results) {
            giveRank(r.build().player(), rank, en);
            rank++;
        }

        if (plugin.getConfig().getBoolean("rewards.announce", true)) {
            announceTop(results, en);
        }
    }

    private void giveRank(String playerName, int rank, String eventName) {
        List<Map<?, ?>> list = plugin.getConfig().getMapList("rewards.ranks." + rank);
        if (list == null || list.isEmpty()) return;   // 非获奖名次：不发任何提示
        boolean queuedAny = false;
        for (Map<?, ?> m : list) {
            String type = str(m.get("type"));
            String data = str(m.get("data"));
            String cfgDesc = str(m.get("desc"));
            if (type == null || data == null) continue;
            String desc = "「" + eventName + "」第" + rank + "名"
                    + (cfgDesc != null && !cfgDesc.isEmpty() ? " · " + cfgDesc : "");
            switch (type) {
                case "money", "item" -> { mailReward(playerName, type, data, desc); queuedAny = true; }
                case "command" -> dispatch(data, playerName);
                default -> { }
            }
        }
        // 仅真正获得奖励的获奖玩家才提示领奖；非获奖玩家不打扰
        if (queuedAny) {
            Player p = Bukkit.getPlayerExact(playerName);
            if (p != null && p.isOnline()) {
                p.sendMessage(ChatColor.LIGHT_PURPLE + "[建筑评分] 你获得第 " + rank + " 名奖励，请到 §e/mailbox §f领取（含南瓜币）。");
            }
        }
    }

    /** 奖励入队南瓜邮箱（南瓜币与物品都走邮箱，玩家 /mailbox 领取）。 */
    private void mailReward(String playerName, String type, String data, String desc) {
        PumpkinMail pm = PumpkinMail.get();
        if (pm == null || pm.storage() == null) return;
        try {
            pm.storage().addReward(new Reward(
                    "br-" + UUID.randomUUID(), null, playerName, null,
                    type, data, desc, "BuildRate", System.currentTimeMillis(), false));
        } catch (Throwable t) {
            plugin.getLogger().warning("入队南瓜邮箱失败：" + t.getMessage());
        }
    }

    private void dispatch(String cmd, String playerName) {
        if (cmd == null || cmd.isEmpty()) return;
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("{player}", playerName));
    }

    private void announceTop(List<Models.BuildResult> results, String eventName) {
        Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "===== 「" + eventName + "」结算，获奖名单 =====");
        int shown = 0;
        for (Models.BuildResult r : results) {
            if (shown >= 3) break;
            shown++;
            Bukkit.broadcastMessage(ChatColor.GOLD + "#" + shown + "  " + ChatColor.WHITE + r.build().name()
                    + ChatColor.GRAY + "（" + r.build().player() + "）"
                    + ChatColor.GREEN + " " + String.format("%.1f", r.avg()) + " 分"
                    + ChatColor.GRAY + " · " + r.votes() + "票");
        }
        Bukkit.broadcastMessage(ChatColor.GRAY + "奖励已发放到获奖玩家邮箱，/mailbox 领取（含南瓜币）。");
    }

    private String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
