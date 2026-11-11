package com.nangua.moneyledger.ledger;

import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * EssentialsX trade.log 幂等回填器（1.1.0 新增）。
 *
 * 背景：账本原本只依赖 UserBalanceUpdateEvent，实测 2026-10-30 ~ 11-08 期间
 * ledger.db 一条记录都没有，而同期 trade.log 有 5000+ 笔真实变动。
 * trade.log 是 EssentialsX 自己写的权威流水，用它做「第二数据源」补齐，
 * 才能真正做到所有变动都留痕。
 *
 * 三类行的处理（按实测 72126 行日志归纳）：
 *  1) API,Add / API,Subtract：字段 5 = 金额、末尾字段 = 变动后余额 → old = new - delta。
 *  2) Command,Pay：一行代表付款人与收款人两笔变动，字段 4/8 = 双方名字、字段 5 = 金额；
 *     紧邻其前的两条 Update,Set 行分别给出双方变动后的余额，据此生成两笔准确记录。
 *  3) Update,Set / API,Set：伴随行，不计为独立变动。
 *
 * 去重键 = 玩家 + 秒 + 2 位小数金额 + 2 位小数结果余额；同一基键出现 N 次，
 * 先查账本已有 M 条，只补第 M+1..N 条，重复运行不会多插。
 * 进度按「已处理行数」保存；最后一行不完整时留到下次重读。
 * 来源一律标 Essentials/trade.log，不臆测是哪个插件触发的。
 */
public final class TradeLogBackfiller {

    private static final Pattern DATE = Pattern.compile(
            "(\\d{4})年(\\d{1,2})月(\\d{1,2})日.*?(\\d{1,2}):(\\d{1,2}):(\\d{1,2})");
    private static final Pattern NUM = Pattern.compile("[-+]?[0-9]*\\.?[0-9]+");
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final File tradeLog;
    private final File userdataDir;
    private final LedgerStore store;
    private final Logger logger;
    private final String minDate;
    /** 玩家名(小写) → UUID。 */
    private final Map<String, String> uuidCache = new HashMap<>();
    /** UUID(小写) → 玩家名，用于把日志里直接写成 UUID 的字段还原成真实账号名。 */
    private final Map<String, String> uuidNames = new HashMap<>();

    public TradeLogBackfiller(File tradeLog, File userdataDir, LedgerStore store, Logger logger) {
        this(tradeLog, userdataDir, store, logger, null);
    }

    public TradeLogBackfiller(File tradeLog, File userdataDir, LedgerStore store, Logger logger, String minDate) {
        this.tradeLog = tradeLog;
        this.userdataDir = userdataDir;
        this.store = store;
        this.logger = logger == null ? Logger.getLogger("MoneyLedger") : logger;
        this.minDate = minDate == null || minDate.isBlank() ? null : minDate.trim();
    }

    public static final class Result {
        public int lines, changes, matched, inserted, skipped, payPairs, payMissing;
        public long offsetFrom, offsetTo;
        public String error;

        @Override public String toString() {
            return "扫描 " + lines + " 行 / 变动 " + changes + " 笔（含 /pay " + payPairs + " 笔"
                    + (payMissing > 0 ? "，缺余额 " + payMissing + " 笔" : "") + "），已存在 " + matched
                    + "，新补 " + inserted + "，跳过 " + skipped
                    + (error == null ? "" : "，错误: " + error);
        }
    }

    private record Row(String name, String uuidHint, long time, BigDecimal delta,
                       BigDecimal newBal, String category, String note, int lineNo) { }

    /** 执行一次回填。full=true 时从第 0 行重新扫描（靠去重保证幂等）。 */
    public synchronized Result run(boolean full) {
        Result r = new Result();
        if (tradeLog == null || !tradeLog.exists()) {
            r.error = "trade.log 不存在: " + tradeLog;
            return r;
        }
        loadNames();
        try {
            List<String> lines = Files.readAllLines(tradeLog.toPath(), StandardCharsets.UTF_8);
            r.lines = lines.size();
            String saved = full ? null : store.state("tradelog.line");
            int start = 0;
            if (saved != null) {
                try { start = Math.max(0, Math.min(Integer.parseInt(saved), lines.size())); }
                catch (NumberFormatException ignored) { }
            }
            r.offsetFrom = start;

            Map<String, List<Row>> groups = new LinkedHashMap<>();
            int processedTo = start;
            for (int i = start; i < lines.size(); i++) {
                String line = lines.get(i);
                if (i == lines.size() - 1 && looksTruncated(line)) {
                    r.skipped++;
                    break; // 正在写入的半行，留到下次
                }
                List<Row> rows = parse(line, i + 1, lines);
                processedTo = i + 1;
                for (Row row : rows) {
                    if (beforeMin(row.time())) continue;
                    r.changes++;
                    if ("/pay".equals(row.note())) r.payPairs++;
                    groups.computeIfAbsent(
                            LedgerStore.dedupBase(row.name(), row.time(), row.delta().doubleValue(), row.newBal().doubleValue()),
                            k -> new ArrayList<>()).add(row);
                }
            }
            r.offsetTo = processedTo;

            // 一次性载入已有去重计数，避免上万次逐条查库
            Map<String, Integer> counts = store.baseCounts();
            List<LedgerEntry> batch = new ArrayList<>();
            for (Map.Entry<String, List<Row>> en : groups.entrySet()) {
                int have = counts.getOrDefault(en.getKey(), 0);
                List<Row> rows = en.getValue();
                for (int k = 0; k < rows.size(); k++) {
                    if (k < have) { r.matched++; continue; }
                    batch.add(toEntry(rows.get(k)));
                    if (batch.size() >= 400) {
                        r.inserted += store.insertBatch(batch, counts);
                        batch.clear();
                    }
                }
            }
            if (!batch.isEmpty()) r.inserted += store.insertBatch(batch, counts);
            store.setState("tradelog.line", String.valueOf(processedTo));
        } catch (Exception e) {
            r.error = e.toString();
            logger.severe("[MoneyLedger] trade.log 回填失败: " + e);
        }
        return r;
    }

    /** 按自然日统计 trade.log 的变动笔数（/pay 一行算两笔，与账本事件口径一致）。 */
    public Map<String, Long> dailyCounts() {
        Map<String, Long> out = new TreeMap<>();
        if (tradeLog == null || !tradeLog.exists()) return out;
        loadNames();
        try {
            List<String> lines = Files.readAllLines(tradeLog.toPath(), StandardCharsets.UTF_8);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (!isChangeLine(line)) continue;
                String[] p = line.split(",", -1);
                if (p.length < 6) continue;
                Matcher m = DATE.matcher(p[3]);
                if (!m.find()) continue;
                try {
                    long millis = timeOf(p[3]);
                    if (beforeMin(millis)) continue;
                    String day = Instant.ofEpochMilli(millis).atZone(ZONE).toLocalDate().toString();
                    // 与 run() 保持一致：Pay 行按能还原出的笔数计，避免误报缺口
                    int n = parse(line, i + 1, lines).size();
                    if (n > 0) out.merge(day, (long) n, Long::sum);
                } catch (Exception ignored) { }
            }
        } catch (Exception ignored) { }
        return out;
    }

    /**
     * 统计「独占 Update,Set 行」：后面没有紧随的同秒同名 API 变动行。
     * 这类行只给出变动后余额，无法还原变动前余额，故不自动补记（不编造金额）；
     * 账本运行期间它们已由 UserBalanceUpdateEvent 直接记录，verify 会单列提示。
     */
    public int orphanSetRows() {
        if (tradeLog == null || !tradeLog.exists()) return 0;
        int n = 0;
        try {
            List<String> lines = Files.readAllLines(tradeLog.toPath(), StandardCharsets.UTF_8);
            for (int i = 0; i < lines.size(); i++) {
                String[] p = lines.get(i).split(",", -1);
                if (p.length < 6 || !"Update".equals(p[0]) || !"Set".equals(p[1])) continue;
                String[] q = i + 1 < lines.size() ? lines.get(i + 1).split(",", -1) : new String[0];
                boolean paired = q.length > 5 && "API".equals(q[0])
                        && (q[1].equals("Add") || q[1].equals("Subtract") || q[1].equals("Set"))
                        && q[3].equals(p[3]) && q[4].equals(p[4]);
                if (!paired) {
                    try { if (!beforeMin(timeOf(p[3]))) n++; } catch (Exception ignored) { }
                }
            }
        } catch (Exception ignored) { }
        return n;
    }

    private LedgerEntry toEntry(Row row) {
        BigDecimal old = row.newBal().subtract(row.delta());
        String uuid = row.uuidHint();
        if (uuid == null || uuid.isEmpty()) uuid = uuidCache.get(row.name().toLowerCase());
        if (uuid == null || uuid.isEmpty()) uuid = store.uuidByName(row.name());
        String src = "Essentials/trade.log | 行 " + row.lineNo() + ("/pay".equals(row.note()) ? " | /pay 转账" : "");
        return new LedgerEntry(
                uuid == null ? "" : uuid, row.name(), row.time(),
                old.doubleValue(), row.newBal().doubleValue(), row.delta().doubleValue(),
                "TRADE_LOG", row.category(), src,
                old.toPlainString(), row.newBal().toPlainString(), row.delta().toPlainString(),
                "BACKFILL");
    }

    private static boolean isChangeLine(String line) {
        if (line == null || line.length() < 20) return false;
        int a = line.indexOf(',');
        if (a < 0) return false;
        int b = line.indexOf(',', a + 1);
        if (b < 0) return false;
        String op = line.substring(a + 1, b);
        return op.equals("Add") || op.equals("Subtract") || op.equals("Pay");
    }

    private static boolean looksTruncated(String line) {
        return line.split(",", -1).length < 8 || !DATE.matcher(line).find();
    }

    /** 解析一行日志；返回 0~2 笔变动。 */
    private List<Row> parse(String line, int lineNo, List<String> lines) {
        if (!isChangeLine(line)) return List.of();
        String[] p = line.split(",", -1);
        if (p.length < 8) return List.of();
        long time;
        try { time = timeOf(p[3]); } catch (Exception e) { return List.of(); }
        String op = p[1];
        try {
            if ("Pay".equals(op)) return parsePay(p, time, lineNo, lines);
            BigDecimal amount = new BigDecimal(strip(p[5]));
            BigDecimal newBal = lastNumber(p, 6);
            if (newBal == null) return List.of();
            BigDecimal delta = "Subtract".equals(op) ? amount.negate() : amount;
            String raw = strip(p[4]);
            if (raw.isEmpty()) return List.of();
            String hint = isUuid(raw) ? raw : "";
            String name = hint.isEmpty() ? raw : normalizeUuidName(raw);
            return List.of(new Row(name, hint, time, delta, newBal,
                    Category.BACKFILL.name(), "", lineNo));
        } catch (NumberFormatException e) {
            return List.of();
        }
    }

    /**
     * /pay 行：Command,Pay,Player,"时间","付款人",金额,money,$,"收款人",金额,money,$,"世界",x,y,z,付款人余额,
     * 紧邻其前的 Update,Set 行给出双方变动后的余额；解析不出的一方不生成（宁缺勿造）。
     */
    private List<Row> parsePay(String[] p, long time, int lineNo, List<String> lines) {
        if (p.length < 10) return List.of();
        String payerRaw = strip(p[4]);
        String receiverRaw = strip(p[8]);
        if (payerRaw.isEmpty() || receiverRaw.isEmpty()) return List.of();
        // 自己给自己 /pay：余额净变化为 0，日志会写两条同名 Set，
        // 强行拆成两笔会算出负余额，直接跳过（事件链路若真触发仍会照常记录）。
        if (payerRaw.equalsIgnoreCase(receiverRaw)) return List.of();
        BigDecimal amount = new BigDecimal(strip(p[5]));

        BigDecimal payerBal = null, receiverBal = null;
        String stamp = p[3];
        int from = Math.max(0, lineNo - 9); // lineNo 是 1-based，往前最多 8 行
        for (int j = lineNo - 2; j >= from; j--) {
            String[] q = lines.get(j).split(",", -1);
            if (q.length < 6 || !"Update".equals(q[0]) || !"Set".equals(q[1])) continue;
            if (!stamp.equals(q[3])) break; // 不是同一秒，停止回溯
            try {
                BigDecimal bal = new BigDecimal(strip(q[5]));
                String nm = strip(q[4]);
                if (payerBal == null && nm.equalsIgnoreCase(payerRaw)) payerBal = bal;
                if (receiverBal == null && nm.equalsIgnoreCase(receiverRaw)) receiverBal = bal;
            } catch (NumberFormatException ignored) { }
        }
        // 兜底：Pay 行最后一个数字就是付款人变动后余额
        if (payerBal == null) payerBal = lastNumber(p, 10);
        if (payerBal == null) return List.of();

        List<Row> out = new ArrayList<>(2);
        String ph = isUuid(payerRaw) ? payerRaw : "";
        out.add(new Row(ph.isEmpty() ? payerRaw : normalizeUuidName(payerRaw), ph, time,
                amount.negate(), payerBal, Category.TRANSFER.name(), "/pay", lineNo));
        if (receiverBal != null) {
            String rh = isUuid(receiverRaw) ? receiverRaw : "";
            out.add(new Row(rh.isEmpty() ? receiverRaw : normalizeUuidName(receiverRaw), rh, time,
                    amount, receiverBal, Category.TRANSFER.name(), "/pay", lineNo));
        }
        return out;
    }

    /** 从字段 from 起倒序找第一个数字（末尾余额）。 */
    private static BigDecimal lastNumber(String[] p, int from) {
        for (int i = p.length - 1; i >= from; i--) {
            Matcher m = NUM.matcher(p[i]);
            if (m.find()) {
                try { return new BigDecimal(m.group()); } catch (NumberFormatException ignored) { }
            }
        }
        return null;
    }

    private static long timeOf(String dateField) {
        Matcher m = DATE.matcher(dateField);
        if (!m.find()) throw new IllegalArgumentException("bad date: " + dateField);
        LocalDateTime dt = LocalDateTime.of(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)),
                Integer.parseInt(m.group(3)), Integer.parseInt(m.group(4)),
                Integer.parseInt(m.group(5)), Integer.parseInt(m.group(6)));
        return dt.atZone(ZONE).toInstant().toEpochMilli();
    }

    private static boolean isUuid(String s) {
        return s != null && s.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    }

    private String normalizeUuidName(String uuid) {
        String n = uuidNames.get(uuid.toLowerCase());
        return n == null || n.isEmpty() ? uuid : n;
    }

    private boolean beforeMin(long time) {
        if (minDate == null) return false;
        return Instant.ofEpochMilli(time).atZone(ZONE).toLocalDate().toString().compareTo(minDate) < 0;
    }

    private static String strip(String s) {
        if (s == null) return "";
        return s.replace("\"", "").trim();
    }

    /** 从 Essentials userdata/*.yml 建立 玩家名 ↔ UUID 双向映射（不联网、不猜 UUID）。 */
    private void loadNames() {
        if (userdataDir == null || !userdataDir.isDirectory()) return;
        File[] fs = userdataDir.listFiles((d, n) -> n.endsWith(".yml"));
        if (fs == null) return;
        for (File f : fs) {
            try {
                String uuid = f.getName().substring(0, f.getName().length() - 4);
                for (String line : Files.readAllLines(f.toPath(), StandardCharsets.UTF_8)) {
                    if (line.startsWith("last-account-name:")) {
                        String name = strip(line.substring("last-account-name:".length()));
                        if (!name.isEmpty()) {
                            uuidCache.put(name.toLowerCase(), uuid);
                            uuidNames.put(uuid.toLowerCase(), name);
                        }
                        break;
                    }
                }
            } catch (Exception ignored) { }
        }
    }
}
