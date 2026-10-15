package com.nangua.moneyledger.ledger;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * SQLite 存储：ledger 表，按玩家 + 类别查询。
 *
 * 性能改造（2026-10-15）：
 * 1) open() 建表后开启 PRAGMA：journal_mode=WAL、synchronous=NORMAL、busy_timeout=5000，
 *    消除每笔 INSERT 一次 fsync 的主线程阻塞。
 * 2) insert() 不再同步写库，只入内存队列（LinkedBlockingQueue）；
 *    单条后台守护线程（MoneyLedger-DbWriter）批量消费：攒够 BATCH_SIZE 条或每
 *    FLUSH_INTERVAL_MS 秒触发一次 addBatch/executeBatch。SQLite 连接仅在本类内部的
 *    synchronized 块中被访问（写线程 flushBatch / 主线程读查询），同时只有一个线程在用连接。
 * 3) close() 先置 closed、中断写线程，写线程退出前 flush 队列剩余条目，再关闭连接。
 */
public class LedgerStore {

    /** 攒够 N 条立即批量刷盘。 */
    private static final int BATCH_SIZE = 50;
    /** 最长等待 1 秒强制刷一次，避免低峰期记录长时间滞留内存。 */
    private static final long FLUSH_INTERVAL_MS = 1000L;

    private final File file;
    private Connection conn;

    /** 待落盘内存队列：主线程 insert 只入队，不碰 JDBC。 */
    private final BlockingQueue<LedgerEntry> queue = new LinkedBlockingQueue<>();
    /** 单后台守护线程：唯一执行批量写 JDBC 的线程。 */
    private ExecutorService writer;
    private volatile boolean closed;

    public LedgerStore(File file) {
        this.file = file;
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
                s.execute("CREATE INDEX IF NOT EXISTS idx_name ON ledger(name)");
                s.execute("CREATE INDEX IF NOT EXISTS idx_cat ON ledger(category)");
                // 性能改造一：WAL + NORMAL 同步级别 + 忙等待超时
                s.execute("PRAGMA journal_mode=WAL");
                s.execute("PRAGMA synchronous=NORMAL");
                s.execute("PRAGMA busy_timeout=5000");
            }
            closed = false;
            writer = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "MoneyLedger-DbWriter");
                t.setDaemon(true);
                return t;
            });
            writer.execute(this::writerLoop);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 入队即返回（方法签名与调用方 onBalanceUpdate 保持不变）：
     * 主线程余额变动事件不再同步写 SQLite。
     */
    public void insert(LedgerEntry e) {
        if (closed || conn == null || e == null) return;
        queue.offer(e);
    }

    /** 写线程主循环：poll 超时 1 秒唤醒一次，攒够 50 条立即刷盘；关闭时刷剩余队列。 */
    private void writerLoop() {
        while (true) {
            LedgerEntry first;
            try {
                first = queue.poll(FLUSH_INTERVAL_MS, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ie) {
                flushRemaining();
                return;
            }
            if (first == null) {
                if (closed) {
                    flushRemaining();
                    return;
                }
                continue;
            }
            List<LedgerEntry> batch = new ArrayList<>(BATCH_SIZE);
            batch.add(first);
            queue.drainTo(batch, BATCH_SIZE - 1);
            flushBatch(batch);
        }
    }

    private void flushRemaining() {
        List<LedgerEntry> rest = new ArrayList<>();
        queue.drainTo(rest);
        if (!rest.isEmpty()) flushBatch(rest);
    }

    /**
     * 批量落盘。与所有读查询共用同一把 this 锁，
     * 保证 SQLite 连接同一时刻只被一个线程（写线程或主线程命令查询）使用。
     */
    private void flushBatch(List<LedgerEntry> batch) {
        synchronized (this) {
            if (conn == null || batch.isEmpty()) return;
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO ledger(uuid,name,time,old_balance,new_balance,delta,cause,category,source) VALUES(?,?,?,?,?,?,?,?,?)")) {
                for (LedgerEntry e : batch) {
                    ps.setString(1, e.uuid());
                    ps.setString(2, e.name());
                    ps.setLong(3, e.time());
                    ps.setDouble(4, e.oldBalance());
                    ps.setDouble(5, e.newBalance());
                    ps.setDouble(6, e.delta());
                    ps.setString(7, e.cause());
                    ps.setString(8, e.category());
                    ps.setString(9, e.source());
                    ps.addBatch();
                }
                ps.executeBatch();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    /** 最近 N 条（按名字或 UUID 匹配），时间正序返回。 */
    public synchronized List<LedgerEntry> recent(String key, int limit) {
        return query("SELECT * FROM ledger WHERE (name=? COLLATE NOCASE OR uuid=?) ORDER BY id DESC LIMIT ?",
                ps -> {
                    ps.setString(1, key);
                    ps.setString(2, key);
                    ps.setInt(3, limit);
                });
    }

    /** 某玩家按类别过滤最近 N 条。 */
    public synchronized List<LedgerEntry> byCategory(String key, String category, int limit) {
        return query("SELECT * FROM ledger WHERE (name=? COLLATE NOCASE OR uuid=?) AND category=? ORDER BY id DESC LIMIT ?",
                ps -> {
                    ps.setString(1, key);
                    ps.setString(2, key);
                    ps.setString(3, category);
                    ps.setInt(4, limit);
                });
    }

    /** 某玩家按类别汇总净额（正=入，负=出）。 */
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
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return out;
    }

    /** 某玩家全部记录（时间正序）。 */
    public synchronized List<LedgerEntry> all(String key) {
        return query("SELECT * FROM ledger WHERE (name=? COLLATE NOCASE OR uuid=?) ORDER BY id ASC",
                ps -> {
                    ps.setString(1, key);
                    ps.setString(2, key);
                });
    }

    /** 全表记录（用于导出）。 */
    public synchronized List<LedgerEntry> allByNameNull() {
        return query("SELECT * FROM ledger ORDER BY id ASC", ps -> { });
    }

    public synchronized long count() {
        if (conn == null) return 0;
        try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM ledger")) {
            return rs.getLong(1);
        } catch (SQLException e) {
            return 0;
        }
    }

    private interface Binder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    private synchronized List<LedgerEntry> query(String sql, Binder b) {
        List<LedgerEntry> out = new ArrayList<>();
        if (conn == null) return out;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            b.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(row(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return out;
    }

    private LedgerEntry row(ResultSet rs) throws SQLException {
        return new LedgerEntry(
                rs.getString("uuid"),
                rs.getString("name"),
                rs.getLong("time"),
                rs.getDouble("old_balance"),
                rs.getDouble("new_balance"),
                rs.getDouble("delta"),
                rs.getString("cause"),
                rs.getString("category"),
                rs.getString("source"));
    }

    /**
     * 关闭：先停止接受新记录，中断写线程让其把队列剩余条目 flush 完，
     * 再关闭连接（close 与 flushBatch 同为 synchronized，等待写线程退出即保证刷盘完成）。
     */
    public synchronized void close() {
        closed = true;
        if (writer != null) {
            writer.shutdownNow(); // 阻塞 poll，触发 flushRemaining
            try {
                if (!writer.awaitTermination(10, TimeUnit.SECONDS)) {
                    writer.shutdownNow();
                }
            } catch (InterruptedException ignored) {
            }
        }
        try {
            if (conn != null) conn.close();
        } catch (SQLException ignored) {
        }
        conn = null;
    }
}
