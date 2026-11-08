package com.nangua.orbit;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class OrbitCommand implements CommandExecutor, TabCompleter {

    private final OrbitPlugin plugin;

    public OrbitCommand(OrbitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        OrbitConfig config = plugin.getOrbitConfig();
        if (args.length == 0) {
            sender.sendMessage("§e用法: /orbit <玩家> | /orbit stop | /orbit reload | /orbit tour <start|stop|status>");
            return true;
        }

        String sub = args[0];

        if (sub.equalsIgnoreCase("stop")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§c仅玩家可用");
                return true;
            }
            plugin.stopSession(player);
            player.sendMessage(color(config.getMsgStopped()));
            return true;
        }

        if (sub.equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("orbit.admin")) {
                sender.sendMessage(color(config.getMsgNoPermission()));
                return true;
            }
            plugin.reloadPlugin();
            sender.sendMessage("§aOrbit 配置已重载");
            return true;
        }

        if (sub.equalsIgnoreCase("tour")) {
            if (!sender.hasPermission("orbit.admin")) {
                sender.sendMessage(color(config.getMsgNoPermission()));
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage("§e用法: /orbit tour <start|stop|status>");
                return true;
            }
            TourManager tour = plugin.getTourManager();
            String action = args[1];
            if (action.equalsIgnoreCase("start")) {
                if (tour.isRunning()) {
                    sender.sendMessage("§7自动巡视已在运行；用 /orbit tour status 查看进度");
                } else {
                    tour.startManual(sender instanceof Player p ? p : null);
                    sender.sendMessage("§a自动巡视已开始（依次环绕全部在线玩家）");
                }
            } else if (action.equalsIgnoreCase("stop")) {
                tour.stopManual();
                sender.sendMessage("§a自动巡视已停止");
            } else if (action.equalsIgnoreCase("status")) {
                sender.sendMessage(tour.status());
            } else {
                sender.sendMessage("§e用法: /orbit tour <start|stop|status>");
            }
            return true;
        }

        if (!(sender instanceof Player viewer)) {
            sender.sendMessage("§c仅玩家可用");
            return true;
        }
        if (!viewer.hasPermission("orbit.use")) {
            viewer.sendMessage(color(config.getMsgNoPermission()));
            return true;
        }

        Player target = Bukkit.getPlayerExact(sub);
        if (target == null) {
            viewer.sendMessage(color(config.getMsgTargetOffline()));
            return true;
        }
        if (target.getUniqueId().equals(viewer.getUniqueId())) {
            viewer.sendMessage(color(config.getMsgSelf()));
            return true;
        }

        OrbitSession existing = plugin.getSession(viewer);
        if (existing != null && existing.getTarget() != null
                && existing.getTarget().getUniqueId().equals(target.getUniqueId())) {
            plugin.stopSession(viewer);
            viewer.sendMessage(color(config.getMsgStopped()));
        } else {
            plugin.startSession(viewer, target);
            viewer.sendMessage(color(config.getMsgStarted().replace("%player%", target.getName())));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            out.add("stop");
            out.add("reload");
            out.add("tour");
            for (Player player : Bukkit.getOnlinePlayers()) {
                out.add(player.getName());
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("tour")) {
            out.add("start");
            out.add("stop");
            out.add("status");
        }
        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        out.removeIf(value -> !value.toLowerCase(Locale.ROOT).startsWith(prefix));
        return out;
    }

    private String color(String text) {
        return text == null ? "" : ChatColor.translateAlternateColorCodes('&', text);
    }
}
