package com.nangua.moneyledger.ledger;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * SQLite 存储：ledger 表 + backfill_state 表。
 *
 * 1.1.0 准确性改造：
 *  1) 默认「同步落盘」：每笔余额变动在事件线程内直接 INSERT（WAL + synchronous=NORMAL，
 *     单条约 0.1ms），彻底消除旧版内存队列在崩溃 / 关服 / SQLException 时整批丢记录的风险。
 *  2) 写失败不再静默吞掉：记录 Base64 序列化到 ledger-failed.log，下次启动自动重放。
 *  3) 新增精确金额列 old_exact / new_exact / delta_exact（BigDecimal 文本）、
 *     origin（EVENT / BACKFILL / FAIL_REPLAY / ADJUST）、dedup_base / dedup_key（幂等去重）。
 *  4) 旧库自动 ALTER 加列并幂等回填 dedup_base / dedup_key，历史字段一条不改写。
 *  5) verify() 提供字段、金额、余额链与每日笔数的对账报告。
 */
public class LedgerStore {

    private static final String INSERT_SQL =
            "INSERT INTO ledger(uuid,name,time,old_balance,new_balance,delta,cause,category,source," +
                    "old_exact,new_exact,delta_exact,origin,dedup_base,dedup_key) " +
                    "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final File file;
    private final File failFile;
    private final Logger logger;
    private Connection conn;
    private volatile boolean closed;

    public LedgerStore(File file, Logger logger) {
        this.file = file;
        this.failFile = new File(file.getParentFile(), "ledger-failed.log");
        this.logger = logger == null ? Logger.getLogger("MoneyLedger") : logger;
    }

    /** 旧构造器兼容。 */
    public LedgerStore(File file) {
        this(file, null);
    }

    public synchronized void open() {
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
            try (Statement s = conn.createStatement()) {
                s.execute("CREATE TABLE IF NOT EXISTS ledger(" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "uuid TEXT, name TEXT, time INTEGER," +
                        "old_balance REAL, new_balance REAL, delta REAL," +
                        "cause TEXT, category TEXT, source TEXT)");
                s.execute("CREATE TABLE IF NOT EXISTS backfill_state(key TEXT PRIMARY KEY, value TEXT)");
                s.execute("PRAGMA journal_mode=WAL");
                s.execute("PRAGMA synchronous=NORMAL");
                s.execute("PRAGMA busy_timeout=5000");
            }
            migrate();
            backfillDedupKeys();
            replayFailed();
            closed = false;
        } catch (Exception e) {
            logger.severe("[MoneyLedger] 打开账本数据库失败: " + e);
            e.printStackTrace();
        }
    }

    /** 幂等加列（SQLite 不支持 IF NOT EXISTS 的 ADD COLUMN）。 */
    private void migrate() throws SQLException {
        List<String> cols = new ArrayList<>();
        try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery("PRAGMA table_info(ledger)")) {
            while (rs.next()) cols.add(rs.getString(2));
        }
        Map<String, String> add = new LinkedHashMap<>();
        add.put("old_exact", "TEXT");
        add.put("new_exact", "TEXT");
        add.put("delta_exact", "TEXT");
        add.put("origin", "TEXT");
        add.put("dedup_base", "TEXT");
        add.put("dedup_key", "TEXT");
        try (Statement s = conn.createStatement()) {
            for (Map.Entry<String, String> en : add.entrySet()) {
                if (!cols.contains(en.getKey())) {
                    s.execute("ALTER TABLE ledger ADD COLUMN " + en.getKey() + " " + en.getValue());
                    logger.info("[MoneyLedger] 旧库加列: " + en.getKey());
                }
            }
            s.execute("CREATE INDEX IF NOT EXISTS idx_name ON ledger(name)");
            s.execute("CREATE INDEX IF NOT EXISTS idx_cat ON ledger(category)");
            s.execute("CREATE INDEX IF NOT EXISTS idx_time ON ledger(time)");
            s.execute("CREATE INDEX IF NOT EXISTS idx_uuid ON ledger(uuid)");
            s.execute("CREATE INDEX IF NOT EXISTS idx_dedup_base ON ledger(dedup_base)");
            s.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_dedup_key ON ledger(dedup_key)");
            s.execute("UPDATE ledger SET origin='EVENT' WHERE origin IS NULL OR origin=''");
        }
    }

    /** 归一化金额：货币统一按 2 位小数比较，消除 double 尾巴。 */
    public static String money(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /** 幂等去重基键：玩家 + 秒 + 金额 + 变动后余额。 */
    public static String dedupBase(String name, long timeMillis, double delta, double newBalance) {
        return (name == null ? "" : name.toLowerCase()) + "|" + (timeMillis / 1000L)
                + "|" + money(delta) + "|" + money(newBalance);
    }

    /** 给历史记录补 dedup 键；同基键多条按出现顺序追加 #2/#3，保证 dedup_key 唯一。 */
    private void backfillDedupKeys() throws SQLException {
        long total = 0;
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM ledger WHERE dedup_base IS NULL OR dedup_base=''")) {
            if (rs.next()) total = rs.getLong(1);
        }
        if (total == 0) return;
        Map<String, Integer> seen = new HashMap<>();
        // 先把已有基数装载进 seen，避免重复运行时编号冲突
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT dedup_base, COUNT(*) FROM ledger WHERE dedup_base IS NOT NULL AND dedup_base<>'' GROUP BY dedup_base")) {
            while (rs.next()) seen.put(rs.getString(1), rs.getInt(2));
        }
        List<Object[]> rows = new ArrayList<>();
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT id,name,time,delta,new_balance FROM ledger WHERE dedup_base IS NULL OR dedup_base='' ORDER BY id")) {
            while (rs.next()) rows.add(new Object[]{rs.getLong(1), rs.getString(2), rs.getLong(3),
                    rs.getDouble(4), rs.getDouble(5)});
        }
        conn.setAutoCommit(false);
        try (PreparedStatement ps = conn.prepareStatement("UPDATE ledger SET dedup_base=?, dedup_key=? WHERE id=?")) {
            for (Object[] r : rows) {
                String base = dedupBase((String) r[1], (Long) r[2], (Double) r[3], (Double) r[4]);
                int n = seen.merge(base, 1, Integer::sum);
                String key = n == 1 ? base : base + "#" + n;
                ps.setString(1, base);
                ps.setString(2, key);
                ps.setLong(3, (Long) r[0]);
                ps.addBatch();
            }
            ps.executeBatch();
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
        logger.info("[MoneyLedger] 历史 " + rows.size() + " 条已补去重键（原字段未改写）");
    }

    /** 主入口：事件写入。同步落盘，失败落文件等待重放。 */
    public synchronized void insert(LedgerEntry e) {
        if (closed || conn == null || e == null) return;
        try {
            writeOne(e, true);
        } catch (SQLException ex) {
            logger.severe("[MoneyLedger] 写入失败，已转存待重放: " + ex.getMessage());
            appendFailed(e);
        }
    }

    /** 批量写入（旧签名，内部自行查库编号）。 */
    public synchronized int insertBatch(List<LedgerEntry> entries) {
        return insertBatch(entries, new HashMap<>());
    }

    /**
     * 批量写入（trade.log 回填用），返回实际插入条数。
     * counts 由调用方一次性从 baseCounts() 载入并随插入递增，避免上万次逐条查询。
     */
    public synchronized int insertBatch(List<LedgerEntry> entries, Map<String, Integer> counts) {
        if (conn == null || entries == null || entries.isEmpty()) return 0;
        // 先为整批预分配去重键：同一 base 的第 n 条追加 #n，避免 UNIQUE 冲突
        List<String[]> keys = new ArrayList<>(entries.size());
        for (LedgerEntry e : entries) {
            String base = dedupBase(e.name(), e.time(), e.delta(), e.newBalance());
            Integer n = counts.get(base);
            if (n == null) n = countByBase(base) + 1;
            keys.add(new String[]{base, n == 1 ? base : base + "#" + n});
            counts.put(base, n + 1);
        }
        int ok = 0;
        try {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
                for (int i = 0; i < entries.size(); i++) {
                    bind(ps, entries.get(i), keys.get(i)[0], keys.get(i)[1]);
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit();
                ok = entries.size();
            } catch (SQLException e) {
                conn.rollback();
                logger.severe("[MoneyLedger] 批量写入失败，逐条重试: " + e.getMessage());
            }
        } catch (SQLException ignored) {
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException ignored) { }
        }
        if (ok == 0) {
            for (LedgerEntry e : entries) {
                try {
                    writeOne(e, true);
                    ok++;
                } catch (SQLException ex) {
                    appendFailed(e);
                }
            }
        }
        return ok;
    }

    /** 计算不与现有记录冲突的 dedup_key。 */
    private String dedupKeyFor(LedgerEntry e) {
        String base = dedupBase(e.name(), e.time(), e.delta(), e.newBalance());
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM ledger WHERE dedup_base=?")) {
            ps.setString(1, base);
            try (ResultSet rs = ps.executeQuery()) {
                int n = rs.next() ? rs.getInt(1) + 1 : 1;
                return n == 1 ? base : base + "#" + n;
            }
        } catch (SQLException ex) {
            return base + "#" + System.nanoTime();
        }
    }

    private void writeOne(LedgerEntry e, boolean computeKey) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
            String base = dedupBase(e.name(), e.time(), e.delta(), e.newBalance());
            bind(ps, e, base, computeKey ? dedupKeyFor(e) : base);
            ps.executeUpdate();
        }
    }

    private void bind(PreparedStatement ps, LedgerEntry e, String base, String key) throws SQLException {
        ps.setString(1, e.uuid());
        ps.setString(2, e.name());
        ps.setLong(3, e.time());
        ps.setDouble(4, e.oldBalance());
        ps.setDouble(5, e.newBalance());
        ps.setDouble(6, e.delta());
        ps.setString(7, e.cause());
        ps.setString(8, e.category());
        ps.setString(9, e.source());
        ps.setString(10, e.oldExact());
        ps.setString(11, e.newExact());
        ps.setString(12, e.deltaExact());
        ps.setString(13, e.safeOrigin());
        ps.setString(14, base);
        ps.setString(15, key);
    }

    /** 某基键已有多少条（回填去重）。 */
    public synchronized int countByBase(String base) {
        if (conn == null) return 0;
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM ledger WHERE dedup_base=?")) {
            ps.setString(1, base);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        } catch (SQLException e) { return 0; }
    }

    /** 一次性载入所有去重基键的已有条数（回填性能优化）。 */
    public synchronized Map<String, Integer> baseCounts() {
        Map<String, Integer> out = new HashMap<>();
        if (conn == null) return out;
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery(
                     "SELECT dedup_base, COUNT(*) FROM ledger WHERE dedup_base IS NOT NULL AND dedup_base<>'' GROUP BY dedup_base")) {
            while (rs.next()) out.put(rs.getString(1), rs.getInt(2));
        } catch (SQLException ignored) { }
        return out;
    }

    /** 按玩家名从历史记录里找最可信的真实 UUID（回填时补 uuid 用）。 */
    public synchronized String uuidByName(String name) {
        if (conn == null || name == null || name.isEmpty()) return "";
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT uuid, COUNT(*) c FROM ledger WHERE name=? COLLATE NOCASE " +
                        "AND uuid IS NOT NULL AND uuid<>'' GROUP BY uuid ORDER BY c DESC LIMIT 1")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getString(1) : ""; }
        } catch (SQLException e) { return ""; }
    }

    // ---------- 失败重放 ----------

    private void appendFailed(LedgerEntry e) {
        String[] f = {e.uuid(), e.name(), e.cause(), e.category(), e.source(),
                e.oldExact(), e.newExact(), e.deltaExact(), e.safeOrigin()};
        StringBuilder sb = new StringBuilder();
        sb.append(e.time()).append('\t')
                .append(e.oldBalance()).append('\t').append(e.newBalance()).append('\t').append(e.delta());
        for (String s : f) sb.append('\t').append(b64(s == null ? "" : s));
        try {
            Files.writeString(failFile.toPath(), sb + "\n", StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception ex) {
            logger.severe("[MoneyLedger] 连失败日志都写不进去: " + ex);
        }
    }

    private static String b64(String s) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    private static String unb64(String s) {
        if (s == null || s.isEmpty()) return "";
        return new String(Base64.getUrlDecoder().decode(s), StandardCharsets.UTF_8);
    }

    private void replayFailed() {
        if (!failFile.exists() || failFile.length() == 0) return;
        int ok = 0, fail = 0;
        try {
            List<String> lines = Files.readAllLines(failFile.toPath(), StandardCharsets.UTF_8);
            List<String> left = new ArrayList<>();
            for (String line : lines) {
                String[] p = line.split("\t", -1);
                if (p.length < 13) { left.add(line); continue; }
                try {
                    LedgerEntry e = new LedgerEntry(unb64(p[4]), unb64(p[5]), Long.parseLong(p[0]),
                            Double.parseDouble(p[1]), Double.parseDouble(p[2]), Double.parseDouble(p[3]),
                            unb64(p[6]), unb64(p[7]), unb64(p[8]),
                            unb64(p[9]), unb64(p[10]), unb64(p[11]), unb64(p[12]));
                    writeOne(e, true);
                    ok++;
                } catch (Exception ex) {
                    left.add(line);
                    fail++;
                }
            }
            Files.write(failFile.toPath(), left, StandardCharsets.UTF_8);
            logger.info("[MoneyLedger] 失败记录重放: 成功 " + ok + " / 失败 " + fail);
        } catch (Exception ex) {
            logger.severe("[MoneyLedger] 失败记录重放异常: " + ex);
        }
    }

    // ---------- 状态 ----------

    public synchronized String state(String key) {
        if (conn == null) return null;
        try (PreparedStatement ps = conn.prepareStatement("SELECT value FROM backfill_state WHERE key=?")) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getString(1) : null; }
        } catch (SQLException e) { return null; }
    }

    public synchronized void setState(String key, String value) {
        if (conn == null) return;
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO backfill_state(key,value) VALUES(?,?) " +
                        "ON CONFLICT(key) DO UPDATE SET value=excluded.value")) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.warning("[MoneyLedger] 保存回填进度失败: " + e.getMessage());
        }
    }

    // ---------- 查询（保持旧签名） ----------

    public synchronized List<LedgerEntry> recent(String key, int limit) {
        return query("SELECT * FROM ledger WHERE (name=? COLLATE NOCASE OR uuid=?) ORDER BY id DESC LIMIT ?",
                ps -> { ps.setString(1, key); ps.setString(2, key); ps.setInt(3, limit); });
    }

    public synchronized List<LedgerEntry> byCategory(String key, String category, int limit) {
        return query("SELECT * FROM ledger WHERE (name=? COLLATE NOCASE OR uuid=?) AND category=? ORDER BY id DESC LIMIT ?",
                ps -> { ps.setString(1, key); ps.setString(2, key); ps.setString(3, category); ps.setInt(4, limit); });
    }

    public synchronized Map<String, Double> summary(String key) {
        Map<String, Double> out = new LinkedHashMap<>();
        if (conn == null) return out;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT category, SUM(delta) AS s FROM ledger WHERE (name=? COLLATE NOCASE OR uuid=?) GROUP BY category ORDER BY s DESC")) {
            ps.setString(1, key);
            ps.setString(2, key);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.put(rs.getString(1), rs.getDouble(2));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return out;
    }

    public synchronized List<LedgerEntry> all(String key) {
        return query("SELECT * FROM ledger WHERE (name=? COLLATE NOCASE OR uuid=?) ORDER BY id ASC",
                ps -> { ps.setString(1, key); ps.setString(2, key); });
    }

    public synchronized List<LedgerEntry> allByNameNull() {
        return query("SELECT * FROM ledger ORDER BY id ASC", ps -> { });
    }

    public synchronized long count() {
        if (conn == null) return 0;
        try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM ledger")) {
            return rs.getLong(1);
        } catch (SQLException e) { return 0; }
    }

    /** 按来源统计（EVENT / BACKFILL / ...）。 */
    public synchronized Map<String, Long> originCounts() {
        Map<String, Long> out = new LinkedHashMap<>();
        if (conn == null) return out;
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT COALESCE(origin,'EVENT'), COUNT(*) FROM ledger GROUP BY 1 ORDER BY 2 DESC")) {
            while (rs.next()) out.put(rs.getString(1), rs.getLong(2));
        } catch (SQLException ignored) { }
        return out;
    }

    /** 按自然日统计笔数（Asia/Shanghai）。 */
    public synchronized Map<String, Long> dailyCounts() {
        Map<String, Long> out = new java.util.TreeMap<>();
        if (conn == null) return out;
        try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery("SELECT time FROM ledger")) {
            while (rs.next()) {
                String d = Instant.ofEpochMilli(rs.getLong(1)).atZone(ZONE).format(DAY);
                out.merge(d, 1L, Long::sum);
            }
        } catch (SQLException ignored) { }
        return out;
    }

    /** 数据质量体检报告。 */
    public synchronized String verify(Map<String, Long> tradeDaily) {
        StringBuilder sb = new StringBuilder();
        try {
            sb.append("总笔数: ").append(count()).append('\n');
            for (Map.Entry<String, Long> en : originCounts().entrySet())
                sb.append("  ").append(en.getKey()).append(": ").append(en.getValue()).append('\n');
            try (Statement s = conn.createStatement()) {
                sb.append("  delta ≠ new-old: ").append(scalar(s,
                        "SELECT COUNT(*) FROM ledger WHERE ABS(delta-(new_balance-old_balance))>0.005")).append('\n');
                sb.append("  delta = 0: ").append(scalar(s, "SELECT COUNT(*) FROM ledger WHERE delta=0")).append('\n');
                sb.append("  玩家名/来源为空: ").append(scalar(s,
                        "SELECT COUNT(*) FROM ledger WHERE name IS NULL OR name='' OR source IS NULL OR source=''")).append('\n');
                sb.append("  去重键为空: ").append(scalar(s,
                        "SELECT COUNT(*) FROM ledger WHERE dedup_key IS NULL OR dedup_key=''")).append('\n');
                sb.append("  无精确金额(1.1.0 前历史): ").append(scalar(s,
                        "SELECT COUNT(*) FROM ledger WHERE (delta_exact IS NULL OR delta_exact='') AND COALESCE(origin,'EVENT')='EVENT'")).append('\n');
            }
            int emptyUuidEvent = 0, emptyUuidBackfill = 0, guessedName = 0;
            try (Statement s = conn.createStatement();
                 ResultSet rs = s.executeQuery("SELECT name,uuid,COALESCE(origin,'EVENT') FROM ledger")) {
                while (rs.next()) {
                    String n = rs.getString(1), u = rs.getString(2), o = rs.getString(3);
                    if (n != null && n.length() == 8 && n.matches("[0-9a-fA-F]{8}")) guessedName++;
                    if (u == null || u.isEmpty()) {
                        if ("BACKFILL".equals(o)) emptyUuidBackfill++; else emptyUuidEvent++;
                    }
                }
            }
            sb.append("  UUID 缺失: 事件 ").append(emptyUuidEvent).append(" / 日志补记 ").append(emptyUuidBackfill).append('\n');
            sb.append("  名称兜底(UUID前8位): ").append(guessedName).append('\n');

            if (tradeDaily != null && !tradeDaily.isEmpty()) {
                Map<String, Long> mine = dailyCounts();
                long gap = 0, extra = 0;
                int gapDays = 0;
                sb.append("  trade.log 对账（仅列有缺口的日子）:\n");
                for (Map.Entry<String, Long> en : new java.util.TreeMap<>(tradeDaily).entrySet()) {
                    long a = en.getValue(), b = mine.getOrDefault(en.getKey(), 0L);
                    if (a > b) {
                        gap += a - b; gapDays++;
                        sb.append("    ").append(en.getKey()).append(" 日志 ").append(a)
                                .append(" / 账本 ").append(b).append(" 缺 ").append(a - b).append('\n');
                    } else if (b > a) {
                        extra += b - a; // 事件直记（/eco give 等）在 trade.log 里没有变动行，属正常
                    }
                }
                sb.append("  缺口合计: ").append(gap).append(" 笔 / ").append(gapDays).append(" 天\n");
                sb.append("  账本多出: ").append(extra).append(" 笔（事件直记，日志仅写 Set 行，属正常）\n");
            }
            // 余额链：按 uuid（缺失则按名字）串接，给出前 3 个不连续样例
            int broken = 0;
            List<String> samples = new ArrayList<>();
            Map<String, Double> prev = new HashMap<>();
            try (Statement s = conn.createStatement();
                 ResultSet rs = s.executeQuery("SELECT uuid,name,old_balance,new_balance,time FROM ledger ORDER BY time,id")) {
                while (rs.next()) {
                    String u = rs.getString(1);
                    if (u == null || u.isEmpty()) u = "N:" + rs.getString(2);
                    Double p = prev.get(u);
                    if (p != null && Math.abs(p - rs.getDouble(3)) > 0.01) {
                        broken++;
                        if (samples.size() < 3) samples.add(rs.getString(2) + " @" +
                                Instant.ofEpochMilli(rs.getLong(5)).atZone(ZONE).toLocalDate()
                                + " " + money(p) + "→" + money(rs.getDouble(3)));
                    }
                    prev.put(u, rs.getDouble(4));
                }
            }
            sb.append("  余额衔接不连续: ").append(broken)
                    .append(" 处（直接 setMoney / 并发同毫秒 / 账本启用前漏记均可能）\n");
            for (String x : samples) sb.append("    例: ").append(x).append('\n');
        } catch (SQLException e) {
            sb.append("  体检异常: ").append(e.getMessage()).append('\n');
        }
        return sb.toString();
    }

    private long scalar(Statement s, String sql) throws SQLException {
        try (ResultSet rs = s.executeQuery(sql)) { return rs.next() ? rs.getLong(1) : 0; }
    }

    private interface Binder { void bind(PreparedStatement ps) throws SQLException; }

    private synchronized List<LedgerEntry> query(String sql, Binder b) {
        List<LedgerEntry> out = new ArrayList<>();
        if (conn == null) return out;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            b.bind(ps);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(row(rs)); }
        } catch (SQLException e) { e.printStackTrace(); }
        return out;
    }

    private LedgerEntry row(ResultSet rs) throws SQLException {
        return new LedgerEntry(
                rs.getString("uuid"), rs.getString("name"), rs.getLong("time"),
                rs.getDouble("old_balance"), rs.getDouble("new_balance"), rs.getDouble("delta"),
                rs.getString("cause"), rs.getString("category"), rs.getString("source"),
                rs.getString("old_exact"), rs.getString("new_exact"), rs.getString("delta_exact"),
                rs.getString("origin"));
    }

    public synchronized void close() {
        closed = true;
        try { if (conn != null) conn.close(); } catch (SQLException ignored) { }
        conn = null;
    }
}
