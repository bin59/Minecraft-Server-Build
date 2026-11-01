package com.nangua.pumpkinmail;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitTask;

/**
 * 待领取奖励提示：默认只在玩家<b>进入服务器时提醒一次</b>，不再定时循环刷屏。
 *
 * <p>配置项（config.yml → hint）：
 * <ul>
 *   <li>{@code enabled} 总开关；关闭后上线也不再提示</li>
 *   <li>{@code on-join-only} true（默认）= 只在进入时提醒一次</li>
 *   <li>{@code interval-seconds} 仅当 on-join-only 为 false 时生效；设为 0 同样表示只提醒一次</li>
 * </ul>
 */
public class HintTask {

    /** 上线后延迟多少 tick 再提示：避开进服瞬间其他插件的 ActionBar 覆盖。 */
    private static final long JOIN_DELAY_TICKS = 40L; // 2 秒

    private final PumpkinMail plugin;
    private BukkitTask task;

    public HintTask(PumpkinMail plugin) {
        this.plugin = plugin;
    }

    public void start() {
        boolean enabled = plugin.getConfig().getBoolean("hint.enabled", true);
        if (!enabled) return; // 总开关关闭：既不定轮询，也不在上线时提示

        boolean joinOnly = plugin.getConfig().getBoolean("hint.on-join-only", true);
        long intervalSec = plugin.getConfig().getLong("hint.interval-seconds", 30);

        // 定时轮询只在「明确关闭 join-only 且间隔 > 0」时才启用
        if (!joinOnly && intervalSec > 0) {
            long intervalTicks = Math.max(20, intervalSec * 20L);
            task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 60L, intervalTicks);
        }

        plugin.getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onJoin(PlayerJoinEvent e) {
                Player p = e.getPlayer();
                // 稍延迟再发，避免被进服瞬间的一堆 ActionBar 冲掉
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (p.isOnline()) sendHint(p);
                }, JOIN_DELAY_TICKS);
            }
        }, plugin);
    }

    private void tick() {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            sendHint(p);
        }
    }

    /** 有待领取才提示；数量为 0 时静默。 */
    private void sendHint(Player p) {
        int n = plugin.storage().countPending(p.getName(), p.getUniqueId());
        if (n > 0) {
            p.sendActionBar(color("&e你有 &a" + n + " &e条待领取奖励，输入 &f/mailbox &e领取"));
        }
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
