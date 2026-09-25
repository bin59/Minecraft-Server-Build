package com.hh.dragon;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * DragonManager - 末影龙重生管理
 * 1. /dragon respawn [世界]：在指定世界生成末影龙（不重置黑曜石柱/祭坛，绕过原版重生仪式）
 * 2. 禁止在末地返回传送门四角放置末影水晶触发原版重生（会重置黑曜石柱）
 */
public class DragonManager extends JavaPlugin implements Listener, CommandExecutor {

    // 末地返回传送门四角的重生水晶触发位（x, z），原版在这 4 格放水晶会触发柱子重置+重生
    private static final int[][] CRYSTAL_SLOTS = {{4, 0}, {-4, 0}, {0, 4}, {0, -4}};
    private static final int SLOT_Y = 75;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("dragon").setExecutor(this);
        getLogger().info("DragonManager v" + getDescription().getVersion() + " 已启用（重生不刷新黑曜石柱）");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0 || !args[0].equalsIgnoreCase("respawn")) {
            sender.sendMessage("§6用法: §e/dragon respawn [世界名]");
            sender.sendMessage("§7生成末影龙（不刷新黑曜石柱，不重置祭坛）。缺省世界为 world_the_end");
            return true;
        }
        String worldName = args.length > 1 ? args[1] : "world_the_end";
        World w = Bukkit.getWorld(worldName);
        if (w == null) {
            sender.sendMessage("§c世界 " + worldName + " 不存在");
            return true;
        }
        if (w.getEnvironment() != World.Environment.THE_END) {
            sender.sendMessage("§c" + worldName + " 不是末地维度（只建议在末地生成）");
            return true;
        }
        boolean hasDragon = w.getEntities().stream()
                .anyMatch(e -> e.getType() == EntityType.ENDER_DRAGON);
        if (hasDragon) {
            sender.sendMessage("§c该世界已存在末影龙，无需重复生成");
            return true;
        }
        Location loc = new Location(w, 0.5, 128, 0.5);
        w.spawnEntity(loc, EntityType.ENDER_DRAGON);
        Bukkit.broadcastMessage("§5✦ 末影龙已在 " + worldName + " 重生！");
        Bukkit.broadcastMessage("§7（本次重生未刷新黑曜石柱；战斗中龙不会破坏方块）");
        return true;
    }

    @EventHandler
    public void onEntityPlace(EntityPlaceEvent e) {
        if (e.getEntity().getType() != EntityType.ENDER_CRYSTAL) return;
        World w = e.getBlock().getWorld();
        if (w.getEnvironment() != World.Environment.THE_END) return;
        int x = e.getBlock().getX();
        int y = e.getBlock().getY();
        int z = e.getBlock().getZ();
        for (int[] slot : CRYSTAL_SLOTS) {
            boolean onSlot;
            if (slot[0] != 0) {
                onSlot = Math.abs(x - slot[0]) <= 1 && Math.abs(z) <= 1;
            } else {
                onSlot = Math.abs(x) <= 1 && Math.abs(z - slot[1]) <= 1;
            }
            if (onSlot && Math.abs(y - SLOT_Y) <= 4) {
                e.setCancelled(true);
                Player p = e.getPlayer();
                if (p != null) {
                    p.sendMessage("§c不能在传送门四角放置末影水晶（会重置黑曜石柱）。");
                    p.sendMessage("§e请使用 §b/dragon respawn §e重生末影龙（不刷新黑曜石柱）。");
                }
                return;
            }
        }
    }
}
