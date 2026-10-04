package com.nangua.bedrockskinrecorder;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * 监听基岩玩家上线，延迟异步抓取皮肤。
 * Floodgate 的皮肤数据在玩家登录后可能需要短暂时间才就绪，
 * 因此交给 SkinFetcher 带重试的异步任务处理。
 */
public class SkinCaptureListener implements Listener {

    private final BedrockSkinRecorder plugin;
    private final SkinFetcher fetcher;

    public SkinCaptureListener(BedrockSkinRecorder plugin, SkinFetcher fetcher) {
        this.plugin = plugin;
        this.fetcher = fetcher;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!fetcher.isBedrock(player.getUniqueId())) {
            return; // Java 玩家不处理
        }
        plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin,
                () -> fetcher.capture(player, 0), 60L); // 3 秒后开始抓取
    }
}
