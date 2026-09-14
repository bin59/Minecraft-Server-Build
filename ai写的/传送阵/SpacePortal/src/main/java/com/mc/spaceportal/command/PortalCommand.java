package com.mc.spaceportal.command;

import com.mc.spaceportal.SpacePortalPlugin;
import com.mc.spaceportal.gui.PortalGui;
import com.mc.spaceportal.portal.Portal;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * /spaceportal（别名 /csz、/portal）指令。
 */
public class PortalCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBS = Arrays.asList(
            "help", "gui", "create", "remove", "list", "link", "link2way",
            "sethue", "setcost", "setlimit", "tp", "reload");
    private static final List<String> HUES = Arrays.asList(
            "cyan", "blue", "purple", "pink", "red", "orange", "yellow", "green", "white");

    private final SpacePortalPlugin plugin;

    public PortalCommand(SpacePortalPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "help":
                sendHelp(sender);
                break;
            case "gui":
                openGui(sender);
                break;
            case "list":
                listPortals(sender);
                break;
            case "create":
                create(sender, args);
                break;
            case "remove":
                remove(sender, args);
                break;
            case "link":
                link(sender, args, false);
                break;
            case "link2way":
                link(sender, args, true);
                break;
            case "sethue":
                setHue(sender, args);
                break;
            case "setcost":
                setCost(sender, args);
                break;
            case "setlimit":
                setLimit(sender, args);
                break;
            case "tp":
                teleport(sender, args);
                break;
            case "reload":
                reload(sender);
                break;
            default:
                sender.sendMessage(msg("&c未知子指令，输入 &f/csz help &c查看帮助"));
        }
        return true;
    }

    private void openGui(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(msg("&c该指令只能由玩家执行"));
            return;
        }
        new PortalGui(plugin).openMain(player);
    }

    private void setLimit(CommandSender sender, String[] args) {
        if (!sender.hasPermission("spaceportal.admin")) {
            sender.sendMessage(msg("&c你没有权限执行该操作"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(msg("&c用法：&f/csz setlimit <数量>"));
            sender.sendMessage(msg("&7当前上限：&b" + plugin.getPlayerPortalLimit()
                    + "&7（0 = 禁止自建，-1 = 不限制）"));
            return;
        }
        int value;
        try {
            value = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(msg("&c数量必须是整数"));
            return;
        }
        if (value < -1 || value > 64) {
            sender.sendMessage(msg("&c数量必须在 -1~64 之间（-1 = 不限制）"));
            return;
        }
        plugin.setPlayerPortalLimit(value);
        sender.sendMessage(msg("&a✔ 玩家建阵上限已设为 &b"
                + (value == -1 ? "不限制" : value) + "&a，立即生效"));
    }

    private void sendHelp(CommandSender sender) {
        int cost = plugin.getCost();
        boolean admin = sender.hasPermission("spaceportal.admin");
        sender.sendMessage(msg("&b&l━━━━━━ 空间传送阵 ━━━━━━"));
        sender.sendMessage(msg("&7走进地面阵法自动充能，完成后传送到相连阵法。"));
        sender.sendMessage(msg("&7每次传送消耗 &b" + cost + " 钻石&7（权限免费者除外）。"));
        sender.sendMessage(msg("&f/csz gui &7- 打开传送阵界面：点阵法可传送/删除/连接（所有人）"));
        sender.sendMessage(msg("&f/csz create <名字> &7- 在脚下创建阵法"
                + (admin ? "（管理员不占额度）" : "（每人限 " + limitText(plugin.getPlayerPortalLimit()) + " 个）")));
        sender.sendMessage(msg("&f/csz list &7- 查看所有阵法"));
        if (admin) {
            sender.sendMessage(msg("&f/csz remove <名字> &7- 删除阵法（管理员）"));
            sender.sendMessage(msg("&f/csz link <A> <B> &7- 单向连接 A→B（管理员）"));
            sender.sendMessage(msg("&f/csz link2way <A> <B> &7- 双向连接（管理员）"));
            sender.sendMessage(msg("&f/csz sethue <名字> <颜色> &7- 修改阵法颜色（管理员）"));
            sender.sendMessage(msg("&f/csz setcost <钻石数> &7- 修改传送费用，立即生效（管理员）"));
            sender.sendMessage(msg("&f/csz setlimit <数量> &7- 修改玩家建阵上限，立即生效（管理员）"));
            sender.sendMessage(msg("&f/csz tp <名字> &7- 直接传送过去（管理员）"));
            sender.sendMessage(msg("&f/csz reload &7- 重载配置（管理员）"));
        }
    }

    private String limitText(int limit) {
        return limit < 0 ? "∞" : String.valueOf(limit);
    }

    private void listPortals(CommandSender sender) {
        var all = plugin.getPortals().all();
        if (all.isEmpty()) {
            sender.sendMessage(msg("&7尚未创建任何传送阵，用 &f/csz create <名字> &7开始"));
            return;
        }
        sender.sendMessage(msg("&b&l━━ 传送阵列表（共 " + all.size() + " 个）━━"));
        for (Portal portal : all) {
            String dest = portal.getDestName() == null
                    ? "&8未连接"
                    : "&a→ " + portal.getDestName();
            String owner = portal.getOwner() == null ? "&7公共" : "&e私人";
            sender.sendMessage(msg("&f✦ " + portal.getName() + " &7[" + portal.getWorldName()
                    + " " + (int) portal.getX() + "," + (int) portal.getY() + "," + (int) portal.getZ()
                    + "] " + dest + " " + owner));
        }
    }

    private void create(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(msg("&c该指令只能由玩家执行"));
            return;
        }
        boolean admin = player.hasPermission("spaceportal.admin");
        if (!admin && !player.hasPermission("spaceportal.create")) {
            player.sendMessage(msg("&c你没有权限执行该操作"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(msg("&c用法：&f/csz create <名字>"));
            return;
        }
        String name = args[1];
        if (!name.matches("[A-Za-z0-9_\\u4e00-\\u9fa5]{1,16}")) {
            player.sendMessage(msg("&c名字只能包含中英文、数字、下划线，最多 16 个字符"));
            return;
        }
        // 普通玩家受数量上限约束；管理员建阵不占额度、记为公共阵法
        java.util.UUID owner = null;
        if (!admin) {
            int limit = plugin.getPlayerPortalLimit();
            if (limit == 0) {
                player.sendMessage(msg("&c当前服务器不允许玩家自建传送阵"));
                return;
            }
            int owned = plugin.getPortals().countByOwner(player.getUniqueId());
            if (limit > 0 && owned >= limit) {
                player.sendMessage(msg("&c你名下已有 &f" + owned + " &c个传送阵，达到上限 &f" + limit + " &c个"));
                return;
            }
            owner = player.getUniqueId();
        }
        float hue = hueOf(args.length >= 3 ? args[2] : "cyan");
        Location loc = player.getLocation();
        if (!plugin.getPortals().create(name, loc, hue, owner)) {
            player.sendMessage(msg("&c已存在名为 &f" + name + " &c的传送阵"));
            return;
        }
        player.sendMessage(msg("&a✔ 传送阵 &f" + name + " &a已创建在脚下（颜色：" + args2Color(args) + "）"));
        player.sendMessage(msg("&7下一步：&f/csz link2way " + name + " <另一个阵法名>"));
    }

    private String args2Color(String[] args) {
        return args.length >= 3 ? args[2] : "cyan";
    }

    private void remove(CommandSender sender, String[] args) {
        if (!sender.hasPermission("spaceportal.admin")) {
            sender.sendMessage(msg("&c你没有权限执行该操作"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(msg("&c用法：&f/csz remove <名字>"));
            return;
        }
        if (plugin.getPortals().remove(args[1])) {
            sender.sendMessage(msg("&a✔ 传送阵 &f" + args[1] + " &a已删除（指向它的连接已断开）"));
        } else {
            sender.sendMessage(msg("&c未找到传送阵 &f" + args[1]));
        }
    }

    private void link(CommandSender sender, String[] args, boolean twoWay) {
        if (!sender.hasPermission("spaceportal.admin")) {
            sender.sendMessage(msg("&c你没有权限执行该操作"));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(msg("&c用法：&f/csz " + (twoWay ? "link2way" : "link") + " <A> <B>"));
            return;
        }
        String a = args[1];
        String b = args[2];
        if (a.equalsIgnoreCase(b)) {
            sender.sendMessage(msg("&c两个阵法不能相同"));
            return;
        }
        boolean ok = twoWay ? plugin.getPortals().link2Way(a, b) : plugin.getPortals().link(a, b);
        if (!ok) {
            sender.sendMessage(msg("&c连接失败：阵法不存在"));
            return;
        }
        if (twoWay) {
            sender.sendMessage(msg("&a✔ 已双向连接 &f" + a + " &a⇄ &f" + b));
        } else {
            sender.sendMessage(msg("&a✔ 已单向连接 &f" + a + " &a→ &f" + b));
        }
    }

    private void setHue(CommandSender sender, String[] args) {
        if (!sender.hasPermission("spaceportal.admin")) {
            sender.sendMessage(msg("&c你没有权限执行该操作"));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(msg("&c用法：&f/csz sethue <名字> <颜色>"));
            sender.sendMessage(msg("&7可选颜色：" + String.join("、", HUES)));
            return;
        }
        Portal portal = plugin.getPortals().get(args[1]);
        if (portal == null) {
            sender.sendMessage(msg("&c未找到传送阵 &f" + args[1]));
            return;
        }
        portal.setHue(hueOf(args[2]));
        plugin.getPortals().save();
        sender.sendMessage(msg("&a✔ 传送阵 &f" + portal.getName() + " &a颜色已改为 &f" + args[2]));
    }

    private void setCost(CommandSender sender, String[] args) {
        if (!sender.hasPermission("spaceportal.admin")) {
            sender.sendMessage(msg("&c你没有权限执行该操作"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(msg("&c用法：&f/csz setcost <钻石数>"));
            sender.sendMessage(msg("&7当前费用：&b" + plugin.getCost() + " 钻石"));
            return;
        }
        int value;
        try {
            value = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(msg("&c钻石数必须是数字"));
            return;
        }
        if (value < 0 || value > 10000) {
            sender.sendMessage(msg("&c钻石数必须在 0~10000 之间"));
            return;
        }
        plugin.setCost(value);
        sender.sendMessage(msg("&a✔ 传送费用已改为 &b" + value + " 钻石&a，立即生效"));
    }

    private void teleport(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(msg("&c该指令只能由玩家执行"));
            return;
        }
        if (!player.hasPermission("spaceportal.admin")) {
            player.sendMessage(msg("&c你没有权限执行该操作"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(msg("&c用法：&f/csz tp <名字>"));
            return;
        }
        Portal portal = plugin.getPortals().get(args[1]);
        if (portal == null) {
            player.sendMessage(msg("&c未找到传送阵 &f" + args[1]));
            return;
        }
        Location loc = portal.getCenter();
        if (loc.getWorld() == null) {
            player.sendMessage(msg("&c该阵法所在世界未加载"));
            return;
        }
        loc.add(0.5, 1.0, 0.5);
        loc.setYaw(player.getLocation().getYaw());
        loc.setPitch(player.getLocation().getPitch());
        player.teleport(loc);
        player.sendMessage(msg("&a✔ 已传送到阵法 &f" + portal.getName()));
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission("spaceportal.admin")) {
            sender.sendMessage(msg("&c你没有权限执行该操作"));
            return;
        }
        plugin.reloadConfig();
        plugin.applyConfig();
        sender.sendMessage(msg("&a✔ 配置已重载"));
    }

    private float hueOf(String name) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "blue" -> 225f;
            case "purple" -> 275f;
            case "pink" -> 315f;
            case "red" -> 0f;
            case "orange" -> 30f;
            case "yellow" -> 55f;
            case "green" -> 130f;
            case "white" -> com.mc.spaceportal.effect.EffectController.WHITE_HUE;
            default -> 185f; // cyan
        };
    }

    private String msg(String text) {
        return ChatColor.translateAlternateColorCodes('&', "&8[&b空间传送阵&8] " + text);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : SUBS) {
                if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    out.add(s);
                }
            }
        } else if (args.length >= 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            switch (sub) {
                case "create":
                    if (args.length == 3) {
                        out.addAll(HUES);
                    }
                    break;
                case "remove":
                case "sethue":
                case "tp":
                    if (args.length == 2) {
                        addNames(out, args[1]);
                    } else if (sub.equals("sethue") && args.length == 3) {
                        out.addAll(HUES);
                    }
                    break;
                case "link":
                case "link2way":
                    if (args.length == 2 || args.length == 3) {
                        addNames(out, args[args.length - 1]);
                    }
                    break;
                default:
                    break;
            }
        }
        return out;
    }

    private void addNames(List<String> out, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        for (Portal portal : plugin.getPortals().all()) {
            if (portal.getName().toLowerCase(Locale.ROOT).startsWith(p)) {
                out.add(portal.getName());
            }
        }
    }
}
