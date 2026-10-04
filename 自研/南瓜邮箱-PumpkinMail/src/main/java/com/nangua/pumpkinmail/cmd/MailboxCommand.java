package com.nangua.pumpkinmail.cmd;

import com.nangua.pumpkinmail.PumpkinMail;
import com.nangua.pumpkinmail.storage.Preset;
import com.nangua.pumpkinmail.storage.Reward;
import com.nangua.pumpkinmail.storage.Storage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * /mailbox 主命令。
 *   /mailbox                             玩家打开自己的邮箱
 *   /mailbox give <奖品ID> <名单>         一键批量发放
 *   /mailbox preset add <ID> <奖品> [备注]
 *   /mailbox preset list / remove <ID>
 *   /mailbox admin                       打开批量发放 GUI
 *   /mailbox list <玩家>                  查看某玩家奖励
 *   /mailbox stats                       发放统计
 *   /mailbox reload                      重载
 * 名单 target：@online / @whitelist / @file:路径 / 逗号分隔的玩家名
 */
public class MailboxCommand implements CommandExecutor, TabCompleter {

    private final PumpkinMail plugin;

    public MailboxCommand(PumpkinMail plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player p)) { sender.sendMessage(msg("&c请在游戏内执行 /mailbox")); return true; }
            plugin.guiManager().openMailbox(p);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "admin" -> {
                if (!checkAdmin(sender)) return true;
                if (!(sender instanceof Player p)) { sender.sendMessage(msg("&c请在游戏内打开发放面板")); return true; }
                plugin.guiManager().openAdmin(p);
            }
            case "give" -> {
                if (!checkAdmin(sender)) return true;
                if (args.length < 3) { sender.sendMessage(usage("give <奖品ID> <@online|@whitelist|@file:路径|玩家,玩家>")); return true; }
                int n = give(args[1], args[2], sender.getName());
                sender.sendMessage(msg("&a已向 &e" + n + " &a名玩家发放 &e" + args[1] + " &a（上线后 /mailbox 领取）"));
            }
            case "preset" -> {
                if (!checkAdmin(sender)) return true;
                if (args.length < 2) { sender.sendMessage(msg("&c用法：/mailbox preset add|list|remove")); return true; }
                preset(sender, args);
            }
            case "list" -> {
                if (!checkAdmin(sender)) return true;
                if (args.length < 2) { sender.sendMessage(usage("list <玩家>")); return true; }
                list(sender, args[1]);
            }
            case "stats" -> {
                if (!checkAdmin(sender)) return true;
                long[] s = plugin.storage().stats();
                sender.sendMessage(msg("&e南瓜邮箱统计：待领 &f" + s[0] + " &7/ 已领 &f" + s[1]));
            }
            case "reload" -> {
                if (!checkAdmin(sender)) return true;
                plugin.reloadConfig();
                sender.sendMessage(msg("&a已重载配置文件。"));
            }
            default -> sender.sendMessage(msg("&c未知子命令。&7" + label + " admin / give / preset / list / stats"));
        }
        return true;
    }

    /** 一键批量发放核心逻辑（命令与 GUI 共用）。返回成功发放人数。 */
    public int give(String presetId, String target, String senderName) {
        Storage storage = plugin.storage();
        Preset preset = storage.getPreset(presetId);
        if (preset == null) return 0;
        List<String> names = resolveTarget(target);
        if (names.isEmpty()) return 0;
        int n = 0;
        long now = System.currentTimeMillis();
        for (String raw : names) {
            String name = raw == null ? "" : raw.trim();
            if (name.isEmpty()) continue;
            UUID uuid = Bukkit.getOfflinePlayer(name).getUniqueId();
            storage.addReward(new Reward(
                    UUID.randomUUID().toString(), uuid == null ? null : uuid.toString(), name,
                    preset.id(), preset.rewardType(), preset.data(), preset.desc(),
                    senderName, now, false));
            n++;
        }
        return n;
    }

    private void preset(CommandSender sender, String[] args) {
        Storage storage = plugin.storage();
        switch (args[1].toLowerCase()) {
            case "add" -> {
                if (args.length < 5) {
                    sender.sendMessage(msg("&c用法："));
                    sender.sendMessage(msg("&7  /mailbox preset add <ID> money <金额> [备注]"));
                    sender.sendMessage(msg("&7  /mailbox preset add <ID> item <材质[:数量]> [备注]"));
                    sender.sendMessage(msg("&7  /mailbox preset add <ID> command <指令...>（{player}=玩家名）"));
                    return;
                }
                String id = args[2];
                String type = args[3].toLowerCase();
                String data, desc;
                switch (type) {
                    case "money" -> {
                        data = args[4];
                        desc = join(args, 5);
                    }
                    case "item" -> {
                        data = args[4];
                        desc = join(args, 5);
                    }
                    case "command" -> {
                        data = join(args, 4);
                        desc = id;
                    }
                    default -> { sender.sendMessage(msg("&c未知奖品类型：" + args[3])); return; }
                }
                storage.savePreset(new Preset(id, type, data, desc.isEmpty() ? null : desc));
                sender.sendMessage(msg("&a奖品已保存：&e" + id + " &7" + new Preset(id, type, data, desc.isEmpty() ? null : desc).summary()));
            }
            case "list" -> {
                List<Preset> ps = storage.listPresets();
                if (ps.isEmpty()) { sender.sendMessage(msg("&c奖品库为空。")); return; }
                sender.sendMessage(msg("&e奖品库（" + ps.size() + " 个）："));
                for (Preset p : ps) sender.sendMessage(msg("&7 - &f" + p.id() + " &8" + p.summary()));
            }
            case "remove" -> {
                if (args.length < 3) { sender.sendMessage(usage("preset remove <ID>")); return; }
                storage.removePreset(args[2]);
                sender.sendMessage(msg("&a已删除奖品 &e" + args[2]));
            }
            default -> sender.sendMessage(msg("&c用法：/mailbox preset add|list|remove"));
        }
    }

    private void list(CommandSender sender, String player) {
        List<Reward> rs = plugin.storage().listForPlayer(player);
        sender.sendMessage(msg("&e玩家 &f" + player + " &e的奖励记录（共 " + rs.size() + " 条）："));
        for (Reward r : rs) {
            String state = r.claimed() ? "&a已领" : "&e待领";
            sender.sendMessage(msg("&7 - " + state + " &f" + (r.desc() == null ? r.rewardType() : r.desc())));
        }
    }

    // ---------- 名单解析 ----------

    private List<String> resolveTarget(String target) {
        List<String> out = new ArrayList<>();
        String t = target.trim();
        if (t.equalsIgnoreCase("@online")) {
            for (Player p : Bukkit.getOnlinePlayers()) out.add(p.getName());
        } else if (t.equalsIgnoreCase("@whitelist")) {
            for (OfflinePlayer p : Bukkit.getWhitelistedPlayers()) out.add(p.getName());
        } else if (t.toLowerCase().startsWith("@file:")) {
            String path = t.substring("@file:".length()).trim();
            File f = new File(path);
            if (!f.isAbsolute()) f = new File(plugin.getDataFolder(), path);
            if (!f.isFile()) {
                plugin.getLogger().warning("名单文件不存在：" + f.getAbsolutePath());
                return out;
            }
            try {
                List<String> lines = Files.readAllLines(f.toPath(), StandardCharsets.UTF_8);
                for (String line : lines) {
                    String s = line.trim();
                    if (s.isEmpty() || s.startsWith("#")) continue;
                    out.add(s);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("读取名单文件失败：" + f.getAbsolutePath());
            }
        } else {
            for (String s : t.split(",")) out.add(s);
        }
        return out;
    }

    // ---------- 工具 ----------

    private boolean checkAdmin(CommandSender sender) {
        if (sender.hasPermission("pumpkinmail.admin")) return true;
        sender.sendMessage(msg("&c你没有权限执行此操作。"));
        return false;
    }

    private String join(String[] args, int from) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < args.length; i++) {
            if (i > from) sb.append(' ');
            sb.append(args[i]);
        }
        return sb.toString();
    }

    private String usage(String s) {
        return msg("&c用法：/mailbox " + s);
    }

    private String msg(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("pumpkinmail.admin")) return Collections.emptyList();
        if (args.length == 1) {
            return filter(args[0], List.of("give", "preset", "admin", "list", "stats", "reload"));
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return filter(args[1], plugin.storage().listPresets().stream().map(Preset::id).toList());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return filter(args[2], List.of("@online", "@whitelist", "@file:"));
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("preset")) {
            return filter(args[1], List.of("add", "list", "remove"));
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("list")) {
            return filter(args[1], plugin.storage().listPresets().stream().map(Preset::id).toList());
        }
        return Collections.emptyList();
    }

    private List<String> filter(String prefix, List<String> list) {
        List<String> out = new ArrayList<>();
        for (String s : list) if (s.toLowerCase().startsWith(prefix.toLowerCase())) out.add(s);
        return out;
    }
}
