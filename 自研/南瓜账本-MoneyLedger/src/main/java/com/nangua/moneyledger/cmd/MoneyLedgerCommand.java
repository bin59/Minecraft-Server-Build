package com.nangua.moneyledger.cmd;

import com.nangua.moneyledger.MoneyLedger;
import com.nangua.moneyledger.ledger.Category;
import com.nangua.moneyledger.ledger.LedgerEntry;
import com.nangua.moneyledger.ledger.TradeLogBackfiller;
import org.bukkit.Bukkit;
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
import java.util.TreeMap;

/** /moneyledger 查询、回填与对账命令（1.1.0）。 */
public class MoneyLedgerCommand implements CommandExecutor, TabCompleter {

    private static final String[] SUBS = {"recent", "summary", "export", "stats", "backfill", "verify"};

    private final MoneyLedger plugin;

    public MoneyLedgerCommand(MoneyLedger plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!checkAdmin(sender)) return true;
        if (args.length == 0) {
            sender.sendMessage("§6[南瓜账本] §e/moneyledger recent <玩家> [类别] §7查看流水");
            sender.sendMessage("§e/moneyledger summary <玩家> §7按类别汇总");
            sender.sendMessage("§e/moneyledger export [玩家] §7导出 CSV");
            sender.sendMessage("§e/moneyledger stats §7统计总笔数与记录来源");
            sender.sendMessage("§e/moneyledger backfill [full] §7从 trade.log 幂等补记漏掉的变动");
            sender.sendMessage("§e/moneyledger verify §7与 trade.log 对账 + 数据质量体检");
            return true;
        }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "recent" -> { return recent(sender, args); }
            case "summary" -> { return summary(sender, args); }
            case "export" -> { return export(sender, args); }
            case "stats" -> { return stats(sender); }
            case "backfill" -> { return backfill(sender, args); }
            case "verify" -> { return verify(sender); }
            default -> sender.sendMessage(usage());
        }
        return true;
    }

    private boolean checkAdmin(CommandSender sender) {
        if (sender.hasPermission("moneyledger.admin") || sender.isOp()) return true;
        sender.sendMessage("§c你没有权限使用南瓜账本（需要 moneyledger.admin）。");
        return false;
    }

    private boolean recent(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage(usage()); return true; }
        String key = args[1];
        Category cat = args.length >= 3 ? Category.fromName(args[2]) : null;
        int limit = plugin.getConfig().getInt("query-limit", 30);
        List<LedgerEntry> rows = cat == null
                ? plugin.getStore().recent(key, limit)
                : plugin.getStore().byCategory(key, cat.name(), limit);
        if (rows.isEmpty()) {
            sender.sendMessage("§6[南瓜账本] §c没有找到 §e" + key + (cat != null ? " §c的 §e" + cat.label + " §c记录" : " §c的记录"));
            return true;
        }
        sender.sendMessage("§6[南瓜账本] §f" + key + (cat != null ? " · " + cat.label : "") + " §7(共 " + rows.size() + " 条, 按时间倒序)");
        SimpleDateFormat fmt = new SimpleDateFormat("MM-dd HH:mm:ss");
        for (LedgerEntry e : rows) {
            String arrow = e.delta() >= 0 ? "§a+" + e.displayDelta() : "§c" + e.displayDelta();
            String tag = "BACKFILL".equals(e.safeOrigin()) ? " §7[日志补记]" : "";
            sender.sendMessage("  §7" + fmt.format(new Date(e.time())) + " §8[" + label(e.category()) + "§8] "
                    + arrow + " §7余额 " + e.displayNew() + sourceSuffix(e.source()) + tag);
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

    private boolean stats(CommandSender sender) {
        sender.sendMessage("§6[南瓜账本] 总笔数: §e" + plugin.getStore().count());
        for (Map.Entry<String, Long> en : plugin.getStore().originCounts().entrySet()) {
            String name = switch (en.getKey()) {
                case "EVENT" -> "事件实时记录";
                case "BACKFILL" -> "trade.log 补记";
                case "FAIL_REPLAY" -> "失败重放";
                default -> en.getKey();
            };
            sender.sendMessage("  §7" + name + ": §f" + en.getValue());
        }
        return true;
    }

    private boolean backfill(CommandSender sender, String[] args) {
        boolean full = args.length >= 2 && args[1].equalsIgnoreCase("full");
        sender.sendMessage("§6[南瓜账本] §e开始" + (full ? "全量" : "增量") + "回填 trade.log…");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            TradeLogBackfiller.Result r = plugin.getBackfiller().run(full);
            Bukkit.getScheduler().runTask(plugin, () ->
                    sender.sendMessage("§6[南瓜账本] §a回填完成：§f" + r));
        });
        return true;
    }

    private boolean verify(CommandSender sender) {
        sender.sendMessage("§6[南瓜账本] §e正在与 Essentials trade.log 对账…");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Map<String, Long> daily = new TreeMap<>(plugin.getBackfiller().dailyCounts());
            // 账本启用前的历史日志不计入缺口
            Map<String, Long> mine = plugin.getStore().dailyCounts();
            if (!mine.isEmpty()) {
                String first = mine.keySet().iterator().next();
                daily.keySet().removeIf(d -> d.compareTo(first) < 0);
            }
            String report = plugin.getStore().verify(daily);
            int orphan = plugin.getBackfiller().orphanSetRows();
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage("§6[南瓜账本] §f对账 / 体检报告");
                for (String line : report.trim().split("\n")) sender.sendMessage("  §7" + line);
                sender.sendMessage("  §7独占 Set 行: " + orphan
                        + " 笔（日志仅记变动后余额，未自动补记；运行期已由事件直记）");
            });
        });
        return true;
    }

    private boolean export(CommandSender sender, String[] args) {
        String key = args.length >= 2 ? args[1] : null;
        List<LedgerEntry> rows = key == null ? allRows() : plugin.getStore().all(key);
        File dir = new File(plugin.getDataFolder(), "export");
        dir.mkdirs();
        String name = key == null ? "all" : sanitize(key);
        File csv = new File(dir, "ledger_" + name + "_" + System.currentTimeMillis() + ".csv");
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try (BufferedWriter w = Files.newBufferedWriter(csv.toPath(), StandardCharsets.UTF_8)) {
            w.write("time,uuid,name,category,cause,source,origin,delta,delta_exact,old_balance,old_exact,new_balance,new_exact");
            w.newLine();
            for (LedgerEntry e : rows) {
                w.write(String.join(",",
                        fmt.format(new Date(e.time())),
                        quote(e.uuid()), quote(e.name()), quote(label(e.category())),
                        quote(e.cause()), quote(e.source()), quote(e.safeOrigin()),
                        String.format("%.2f", e.delta()), quote(e.displayDelta()),
                        String.format("%.2f", e.oldBalance()), quote(e.displayOld()),
                        String.format("%.2f", e.newBalance()), quote(e.displayNew())));
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

    /** CSV 字段转义，防止备注/来源里的逗号与换行破坏格式。 */
    private String quote(String s) {
        String v = s == null ? "" : s.replace("\r", " ").replace("\n", " ");
        if (v.contains(",") || v.contains("\"")) return "\"" + v.replace("\"", "\"\"") + "\"";
        return v;
    }

    private List<LedgerEntry> allRows() { return plugin.getStore().allByNameNull(); }

    private String label(String cat) {
        Category c = Category.fromName(cat);
        return c == null ? cat : c.label;
    }

    private String sourceSuffix(String src) {
        return src == null || src.isEmpty() ? "" : " §8(" + src + ")";
    }

    private String sanitize(String s) { return s.replaceAll("[^\\w\\-.]", "_"); }

    private String usage() {
        return "§6[南瓜账本] §e/moneyledger <recent|summary|export|stats|backfill|verify>";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : SUBS) if (s.startsWith(args[0].toLowerCase())) out.add(s);
        } else if (args.length == 3 && args[0].equalsIgnoreCase("recent")) {
            for (Category c : Category.values()) if (c.label.startsWith(args[2])) out.add(c.name());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("backfill")) {
            if ("full".startsWith(args[1].toLowerCase())) out.add("full");
        }
        return out;
    }
}
