package com.nangua.offlinereward.storage;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

/**
 * SQLite 存储层。offline.db，表 players(player_uuid, last_logout, last_reward,
 * last_login, online_reward_today, online_date)。
 * 单服使用；每次退出写一次、上线读一次，量级极小。
 */
public class Storage {

    private final Connection conn;

    public Storage(File dbFile) throws Exception {
        Class.forName("org.sqlite.JDBC");
        this.conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS players(
                    player_uuid TEXT PRIMARY KEY,
                    last_logout INTEGER DEFAULT 0,
                    last_reward INTEGER DEFAULT 0
                )""");
            // 在线奖励新增列（旧库无这些列，忽略"已存在"异常）
            for (String col : new String[]{"last_login", "online_reward_today", "online_date"}) {
                try {
                    st.executeUpdate("ALTER TABLE players ADD COLUMN " + col + " INTEGER DEFAULT 0");
                } catch (SQLException ignored) { /* 列已存在 */ }
            }
        }
    }

    /** 最后退出时间戳（毫秒），无记录返回 0。 */
    public synchronized long getLastLogout(UUID uuid) {
        return getLong(uuid, "last_logout");
    }

    /** 上次离线发放时间戳（毫秒），无记录返回 0。 */
    public synchronized long getLastReward(UUID uuid) {
        return getLong(uuid, "last_reward");
    }

    /** 本次在线起点时间戳（毫秒），未在线返回 0。 */
    public synchronized long getLastLogin(UUID uuid) {
        return getLong(uuid, "last_login");
    }

    /** 本日已发在线奖励币数。 */
    public synchronized long getOnlineRewardToday(UUID uuid) {
        return getLong(uuid, "online_reward_today");
    }

    /** 在线奖励每日累计对应的日期（YYYYMMDD 整数）。 */
    public synchronized long getOnlineDate(UUID uuid) {
        return getLong(uuid, "online_date");
    }

    public synchronized void setLastLogout(UUID uuid, long millis) {
        upsert(uuid, "last_logout", millis);
    }

    public synchronized void setLastReward(UUID uuid, long millis) {
        upsert(uuid, "last_reward", millis);
    }

    public synchronized void setLastLogin(UUID uuid, long millis) {
        upsert(uuid, "last_login", millis);
    }

    public synchronized void setOnlineRewardToday(UUID uuid, long v) {
        upsert(uuid, "online_reward_today", v);
    }

    public synchronized void setOnlineDate(UUID uuid, long v) {
        upsert(uuid, "online_date", v);
    }

    private long getLong(UUID uuid, String col) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT " + col + " FROM players WHERE player_uuid=?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0L;
    }

    private void upsert(UUID uuid, String col, long millis) {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO players(player_uuid," + col + ") VALUES(?,?) " +
                        "ON CONFLICT(player_uuid) DO UPDATE SET " + col + "=excluded." + col)) {
            ps.setString(1, uuid.toString());
            ps.setLong(2, millis);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public synchronized void close() {
        try { if (conn != null) conn.close(); } catch (SQLException ignored) { }
    }
}
