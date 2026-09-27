package com.nangua.quickmenu.command;

import com.nangua.quickmenu.QuickMenuPlugin;
import com.nangua.quickmenu.menu.ActionExecutor;
import com.nangua.quickmenu.menu.Menu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * /qm 命令。
 *
 * <pre>
 *   /qm                     打开默认菜单
 *   /qm open [菜单id]        打开指定菜单
 *   /qm give [玩家]          给自己/他人发放触发物品（需 quickmenu.admin）
 *   /qm reload              重载配置（需 quickmenu.admin）
 *   /qm list                列出所有已加载菜单（需 quickmenu.admin）
 *   /qm info                显示当前玩家的客户端类型与设备信息（调试用）
 * </pre>
 *
 * <p>所有提示文本均来自 config.yml 的 messages 节点，便于本地化。
 */
public final class QuickMenuCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS =
            Arrays.asList("open", "give", "reload", "list", "info");

    private final QuickMenuPlugin plugin;

    public QuickMenuCommand(QuickMenuPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ActionExecutor.color(
                        msg("console-not-allowed", "&c该命令只能由玩家执行")));
                return true;
            }
            Player player = (Player) sender;
            if (!player.hasPermission("quickmenu.use")) {
                player.sendMessage(ActionExecutor.color(
                        msg("no-permission", "&c你没有权限使用快捷菜单")));
                return true;
            }
            plugin.getMenuManager().openDefaultMenu(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "open":
                return handleOpen(sender, args);
            case "give":
                return handleGive(sender, args);
            case "reload":
                return handleReload(sender);
            case "list":
                return handleList(sender);
            case "info":
                return handleInfo(sender);
            default:
                sender.sendMessage(ActionExecutor.color(
                        msg("unknown-subcommand", "&c未知子命令，使用 /qm 查看帮助")));
                return true;
        }
    }

    private boolean handleOpen(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ActionExecutor.color(
                    msg("console-not-allowed", "&c该命令只能由玩家执行")));
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("quickmenu.use")) {
            player.sendMessage(ActionExecutor.color(
                    msg("no-permission", "&c你没有权限使用快捷菜单")));
            return true;
        }
        if (args.length < 2) {
            plugin.getMenuManager().openDefaultMenu(player);
            return true;
        }
        plugin.getMenuManager().openMenu(player, args[1]);
        return true;
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("quickmenu.admin")) {
            sender.sendMessage(ActionExecutor.color(
                    msg("no-admin-permission", "&c你需要管理员权限")));
            return true;
        }

        Player target;
        if (args.length >= 2) {
            target = plugin.getServer().getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(ActionExecutor.color(
                        msg("player-not-found", "&c找不到该玩家")
                                .replace("{player}", args[1])));
                return true;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage(ActionExecutor.color(
                    msg("specify-player", "&c控制台执行时必须指定玩家名")));
            return true;
        }

        // 材质升级：先移除旧材质触发物品（如指南针→时钟切换），再发放新物品
        plugin.getMenuManager().getChestRenderer().upgradeLegacyTriggerItem(target);

        ItemStack triggerItem = plugin.getMenuManager().getChestRenderer().buildTriggerItem();
        PlayerInventory inventory = target.getInventory();
        java.util.HashMap<Integer, ItemStack> overflow = inventory.addItem(triggerItem);
        if (!overflow.isEmpty()) {
            sender.sendMessage(ActionExecutor.color(
                    msg("inventory-full", "&c{player} 的背包已满")
                            .replace("{player}", target.getName())));
            return true;
        }
        sender.sendMessage(ActionExecutor.color(
                msg("give-success", "&a已发放快捷菜单物品给 {player}")
                        .replace("{player}", target.getName())));
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("quickmenu.admin")) {
            sender.sendMessage(ActionExecutor.color(
                    msg("no-admin-permission", "&c你需要管理员权限")));
            return true;
        }
        plugin.loadConfiguration();
        int count = plugin.getSettings().getMenus().size();
        sender.sendMessage(ActionExecutor.color(
                msg("reload-success", "&a配置已重载，共加载 {count} 个菜单")
                        .replace("{count}", String.valueOf(count))));
        return true;
    }

    private boolean handleList(CommandSender sender) {
        if (!sender.hasPermission("quickmenu.admin")) {
            sender.sendMessage(ActionExecutor.color(
                    msg("no-admin-permission", "&c你需要管理员权限")));
            return true;
        }
        List<Menu> menus = plugin.getSettings().getMenus();
        if (menus.isEmpty()) {
            sender.sendMessage(ActionExecutor.color(
                    msg("no-menus", "&c尚未加载任何菜单")));
            return true;
        }
        sender.sendMessage(ActionExecutor.color("&6已加载的菜单："));
        for (Menu menu : menus) {
            sender.sendMessage(ActionExecutor.color(
                    " &e" + menu.getId() + " &7- " + menu.getTitle()
                            + " &8(容量 " + menu.getSize() + ", 物品 " + menu.getItems().size() + ")"));
        }
        return true;
    }

    private boolean handleInfo(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ActionExecutor.color(
                    msg("console-not-allowed", "&c该命令只能由玩家执行")));
            return true;
        }
        Player player = (Player) sender;

        boolean bedrock = plugin.getMenuManager().isBedrockPlayer(player);
        String device = plugin.getMenuManager().getDeviceInfo(player);

        StringBuilder sb = new StringBuilder();
        sb.append(ActionExecutor.color("&6客户端诊断信息\n"));
        sb.append(ActionExecutor.color("&7玩家: &f" + player.getName() + "\n"));
        sb.append(ActionExecutor.color("&7UUID: &f" + player.getUniqueId() + "\n"));
        sb.append(ActionExecutor.color("&7客户端类型: &f" + (bedrock ? "基岩版" : "Java 版") + "\n"));
        if (bedrock && device != null) {
            sb.append(ActionExecutor.color("&7设备/版本: &f" + device + "\n"));
        }
        sb.append(ActionExecutor.color("&7Floodgate: &f"
                + (plugin.isFloodgateAvailable() ? "可用" : "不可用") + "\n"));
        sb.append(ActionExecutor.color("&7将使用的界面: &f"
                + (bedrock && plugin.getSettings().isBedrockUseNativeForm()
                        && plugin.isFloodgateAvailable() ? "原生 Form 表单" : "箱子 GUI")));
        player.sendMessage(sb.toString());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            if (!sender.hasPermission("quickmenu.admin")) {
                return Collections.singletonList("open");
            }
            List<String> result = new ArrayList<>();
            for (String sub : SUBCOMMANDS) {
                if (sub.startsWith(prefix)) {
                    result.add(sub);
                }
            }
            return result;
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("open")) {
                List<String> ids = new ArrayList<>();
                String prefix = args[1].toLowerCase();
                for (Menu menu : plugin.getSettings().getMenus()) {
                    if (menu.getId().toLowerCase().startsWith(prefix)) {
                        ids.add(menu.getId());
                    }
                }
                return ids;
            }
            if (sub.equals("give") && sender.hasPermission("quickmenu.admin")) {
                List<String> names = new ArrayList<>();
                String prefix = args[1].toLowerCase();
                for (Player online : plugin.getServer().getOnlinePlayers()) {
                    if (online.getName().toLowerCase().startsWith(prefix)) {
                        names.add(online.getName());
                    }
                }
                return names;
            }
        }
        return Collections.emptyList();
    }

    /** 读取消息配置，提供默认值 */
    private String msg(String key, String fallback) {
        return plugin.getConfig().getString("messages." + key, fallback);
    }
}
