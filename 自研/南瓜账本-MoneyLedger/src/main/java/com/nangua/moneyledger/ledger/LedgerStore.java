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

/**
 * SQLite 存储：ledger 表，按玩家 + 类别查询。
 */
public class LedgerStore {

    private final File file;
    private Connection conn;

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
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void insert(LedgerEntry e) {
        if (conn == null) return;
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO ledger(uuid,name,time,old_balance,new_balance,delta,cause,category,source) VALUES(?,?,?,?,?,?,?,?,?)")) {
            ps.setString(1, e.uuid());
            ps.setString(2, e.name());
            ps.setLong(3, e.time());
            ps.setDouble(4, e.oldBalance());
            ps.setDouble(5, e.newBalance());
            ps.setDouble(6, e.delta());
            ps.setString(7, e.cause());
            ps.setString(8, e.category());
            ps.setString(9, e.source());
            ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
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

    public synchronized void close() {
        try {
            if (conn != null) conn.close();
        } catch (SQLException ignored) {
        }
    }
}
