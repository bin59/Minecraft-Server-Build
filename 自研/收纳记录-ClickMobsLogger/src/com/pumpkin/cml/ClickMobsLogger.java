package com.pumpkin.cml;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ClickMobsLogger extends JavaPlugin {

    private File logFile;
    private List<String> blacklistItems;
    private List<String> trackedMobs;
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration cfg = getConfig();
        blacklistItems = cfg.getStringList("blacklist-items-in-hand");
        if (blacklistItems.isEmpty()) blacklistItems = Arrays.asList("LEAD", "SADDLE");
        trackedMobs = cfg.getStringList("tracked-mobs");
        if (trackedMobs.isEmpty()) trackedMobs = Arrays.asList("COW", "PIG", "SHEEP");

        if (!getDataFolder().exists()) getDataFolder().mkdirs();
        logFile = new File(getDataFolder(), "pickups.jsonl");
        try {
            if (!logFile.exists()) logFile.createNewFile();
        } catch (IOException e) {
            getLogger().severe("无法创建日志文件: " + e.getMessage());
        }

        getServer().getPluginManager().registerEvents(new PickupListener(this), this);
        getLogger().info("ClickMobsLogger 已启用，记录文件: " + logFile.getAbsolutePath());
    }

    public boolean isTracked(String typeName) {
        // 配置里填 ALL 表示记录所有动物类型
        if (trackedMobs.stream().anyMatch(m -> m.equalsIgnoreCase("ALL"))) return true;
        return trackedMobs.stream().anyMatch(m -> m.equalsIgnoreCase(typeName));
    }

    public boolean isHandBlacklisted(org.bukkit.Material mat) {
        return blacklistItems.stream().anyMatch(b -> b.equalsIgnoreCase(mat.name()));
    }

    public synchronized void logPickup(Player p, String type, org.bukkit.Location loc) {
        String line = String.format(
                "{\"time\":\"%s\",\"player\":\"%s\",\"uuid\":\"%s\",\"entity\":\"%s\",\"world\":\"%s\",\"x\":%.2f,\"y\":%.2f,\"z\":%.2f}",
                LocalDateTime.now().format(dtf),
                escape(p.getName()),
                p.getUniqueId().toString(),
                type,
                loc.getWorld() == null ? "?" : loc.getWorld().getName(),
                loc.getX(), loc.getY(), loc.getZ()
        );
        try (FileWriter fw = new FileWriter(logFile, true)) {
            fw.write(line);
            fw.write("\n");
        } catch (IOException e) {
            getLogger().severe("写入日志失败: " + e.getMessage());
        }
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("cml")) return false;
        if (!sender.hasPermission("clickmobslogger.use")) {
            sender.sendMessage("§c你没有权限使用此命令。");
            return true;
        }
        if (!logFile.exists()) {
            sender.sendMessage("§c没有记录文件。");
            return true;
        }
        String filter = args.length >= 1 ? args[0] : null;
        int limit = 20;
        try {
            List<String> lines = Files.readAllLines(logFile.toPath());
            List<String> rev = new ArrayList<>(lines);
            Collections.reverse(rev);
            int shown = 0;
            sender.sendMessage("§e=== ClickMobs 拾取记录（最近 " + limit + " 条）===");
            for (String l : rev) {
                if (shown >= limit) break;
                if (filter != null && !l.contains("\"player\":\"" + filter + "\"")) continue;
                sender.sendMessage("§7" + l);
                shown++;
            }
            if (shown == 0) sender.sendMessage("§7无匹配记录。");
        } catch (IOException e) {
            sender.sendMessage("§c读取失败: " + e.getMessage());
        }
        return true;
    }
}
