package com.nangua.moneyledger.cmd;

import com.nangua.moneyledger.MoneyLedger;
import com.nangua.moneyledger.ledger.Category;
import com.nangua.moneyledger.ledger.LedgerEntry;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.io.BufferedWriter;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * /moneyledger 查询命令。
 */
public class MoneyLedgerCommand implements CommandExecutor, TabCompleter {

    private static final String[] SUBS = {"recent", "summary", "export", "stats"};

    private final MoneyLedger plugin;

    public MoneyLedgerCommand(MoneyLedger plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§6[南瓜账本] §e/moneyledger recent <玩家> [类别] §7查看流水");
            sender.sendMessage("§e/moneyledger summary <玩家> §7按类别汇总");
            sender.sendMessage("§e/moneyledger export [玩家] §7导出 CSV");
            sender.sendMessage("§e/moneyledger stats §7统计总笔数");
            return true;
        }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "recent": return recent(sender, args);
            case "summary": return summary(sender, args);
            case "export": return export(sender, args);
            case "stats":
                sender.sendMessage("§6[南瓜账本] 已记录南瓜币变动笔数: §e" + plugin.getStore().count());
                return true;
            default:
                sender.sendMessage(usage());
                return true;
        }
    }

    private boolean recent(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage(usage()); return true; }
        String key = args[1];
        Category cat = args.length >= 3 ? Category.fromName(args[2]) : null;
        int limit = plugin.getConfig().getInt("query-limit", 30);
        List<LedgerEntry> rows;
        if (cat == null) rows = plugin.getStore().recent(key, limit);
        else rows = plugin.getStore().byCategory(key, cat.name(), limit);

        if (rows.isEmpty()) {
            sender.sendMessage("§6[南瓜账本] §c没有找到 §e" + key + (cat != null ? " §c的 §e" + cat.label + " §c记录" : " §c的记录"));
            return true;
        }
        sender.sendMessage("§6[南瓜账本] §f" + key + (cat != null ? " · " + cat.label : "") + " §7(共 " + rows.size() + " 条, 按时间)");
        SimpleDateFormat fmt = new SimpleDateFormat("MM-dd HH:mm:ss");
        for (LedgerEntry e : rows) {
            String arrow = e.delta() >= 0 ? "§a+" + String.format("%.2f", e.delta()) : "§c" + String.format("%.2f", e.delta());
            String catName = label(e.category());
            sender.sendMessage("  §7" + fmt.format(new Date(e.time())) + " §8[" + catName + "§8] " + arrow + " §7余额 " + String.format("%.2f", e.newBalance()) + sourceSuffix(e.source()));
        }
        return true;
    }

    private boolean summary(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage(usage()); return true; }
        String key = args[1];
        Map<String, Double> s = plugin.getStore().summary(key);
        if (s.isEmpty()) {
            sender.sendMessage("§6[南瓜账本] §c没有 §e" + key + " §c的记录");
            return true;
        }
        sender.sendMessage("§6[南瓜账本] §f" + key + " §7按类别净额");
        for (Map.Entry<String, Double> en : s.entrySet()) {
            String v = en.getValue() >= 0 ? "§a+" + String.format("%.2f", en.getValue()) : "§c" + String.format("%.2f", en.getValue());
            sender.sendMessage("  " + label(en.getKey()) + ": " + v);
        }
        return true;
    }

    private boolean export(CommandSender sender, String[] args) {
        String key = args.length >= 2 ? args[1] : null;
        List<LedgerEntry> rows;
        if (key == null) {
            // 全量导出：直接查所有（分批按名字不可行，改查全表）
            rows = allRows();
        } else {
            rows = plugin.getStore().all(key);
        }
        File dir = new File(plugin.getDataFolder(), "export");
        dir.mkdirs();
        String name = key == null ? "all" : sanitize(key);
        File csv = new File(dir, "ledger_" + name + "_" + System.currentTimeMillis() + ".csv");
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try (BufferedWriter w = Files.newBufferedWriter(csv.toPath(), StandardCharsets.UTF_8)) {
            w.write("time,name,category,cause,source,delta,old_balance,new_balance");
            w.newLine();
            for (LedgerEntry e : rows) {
                w.write(String.join(",",
                        fmt.format(new Date(e.time())),
                        e.name(),
                        label(e.category()),
                        e.cause(),
                        e.source(),
                        String.format("%.2f", e.delta()),
                        String.format("%.2f", e.oldBalance()),
                        String.format("%.2f", e.newBalance())));
                w.newLine();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            sender.sendMessage("§6[南瓜账本] §c导出失败: " + ex.getMessage());
            return true;
        }
        sender.sendMessage("§6[南瓜账本] §a已导出 " + rows.size() + " 条 -> §e" + csv.getAbsolutePath());
        return true;
    }

    private List<LedgerEntry> allRows() {
        // 全量导出：遍历 store 提供的方法不够，改为追加一个全表读取
        return plugin.getStore().allByNameNull();
    }

    private String label(String cat) {
        Category c = Category.fromName(cat);
        return c == null ? cat : c.label;
    }

    private String sourceSuffix(String src) {
        return src == null || src.isEmpty() ? "" : " §8(" + src + ")";
    }

    private String sanitize(String s) {
        return s.replaceAll("[^\\w\\-.]", "_");
    }

    private String usage() {
        return "§6[南瓜账本] §e/moneyledger <recent|summary|export|stats>";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : SUBS) if (s.startsWith(args[0].toLowerCase())) out.add(s);
        } else if (args.length == 3 && args[0].equalsIgnoreCase("recent")) {
            for (Category c : Category.values()) if (c.label.startsWith(args[2])) out.add(c.name());
        }
        return out;
    }
}
