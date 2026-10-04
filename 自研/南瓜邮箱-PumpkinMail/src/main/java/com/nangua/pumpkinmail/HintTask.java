package com.nangua.pumpkinmail;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitTask;

/**
 * 上线/定时提示：有待领取奖励时，用 ActionBar 提醒玩家打开 /mailbox。
 */
public class HintTask {

    private final PumpkinMail plugin;
    private BukkitTask task;

    public HintTask(PumpkinMail plugin) {
        this.plugin = plugin;
    }

    public void start() {
        long intervalTicks = Math.max(20, plugin.getConfig().getLong("hint.interval-seconds", 30) * 20L);

        if (plugin.getConfig().getBoolean("hint.enabled", true)) {
            task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 60L, intervalTicks);
        }

        plugin.getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onJoin(PlayerJoinEvent e) {
                Player p = e.getPlayer();
                int n = plugin.storage().countPending(p.getName(), p.getUniqueId());
                if (n > 0) p.sendActionBar(color("&e你有 &a" + n + " &e条待领取奖励，输入 &f/mailbox &e领取"));
            }
        }, plugin);
    }

    private void tick() {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            int n = plugin.storage().countPending(p.getName(), p.getUniqueId());
            if (n > 0) p.sendActionBar(color("&e你有 &a" + n + " &e条待领取奖励，输入 &f/mailbox &e领取"));
        }
    }

    public void stop() {
        if (task != null) task.cancel();
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
