package com.nangua.pumpkinmail;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 聊天文本输入（管理端在 GUI 里选"手动输入名单 / 名单文件"时用于接收后续输入）。
 */
public class ChatInput implements Listener {

    private static final Map<UUID, Consumer<String>> pending = new ConcurrentHashMap<>();
    private final PumpkinMail plugin;

    public ChatInput(PumpkinMail plugin) {
        this.plugin = plugin;
    }

    public static void await(Player p, Consumer<String> consumer) {
        pending.put(p.getUniqueId(), consumer);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        Consumer<String> consumer = pending.remove(p.getUniqueId());
        if (consumer == null) return;
        e.setCancelled(true);
        String input = e.getMessage();
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            if (input.equalsIgnoreCase("cancel")) {
                p.sendMessage("已取消。");
            } else {
                consumer.accept(input);
            }
        });
    }
}
