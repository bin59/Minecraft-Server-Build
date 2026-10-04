package com.nangua.pumpkinmail.storage;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * SQLite 存储层。负责 presets（奖品库）与 rewards（玩家邮箱）两张表。
 * 底层由 sqlite-jdbc 提供（已内嵌进插件 jar）。
 */
public class Storage {

    private final Connection conn;

    public Storage(File dbFile) throws Exception {
        Class.forName("org.sqlite.JDBC");
        this.conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS presets(
                    id TEXT PRIMARY KEY,
                    reward_type TEXT NOT NULL,
                    data TEXT NOT NULL,
                    desc TEXT,
                    created_time INTEGER
                )""");
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS rewards(
                    id TEXT PRIMARY KEY,
                    player_uuid TEXT,
                    player_name TEXT NOT NULL,
                    preset_id TEXT,
                    reward_type TEXT NOT NULL,
                    data TEXT NOT NULL,
                    desc TEXT,
                    issued_by TEXT,
                    issued_time INTEGER,
                    claimed INTEGER DEFAULT 0,
                    claimed_time INTEGER
                )""");
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_rewards_player ON rewards(player_name, claimed)");
        }
    }

    // ---------- presets ----------

    public void savePreset(Preset p) {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT OR REPLACE INTO presets(id,reward_type,data,desc,created_time) VALUES(?,?,?,?,?)")) {
            ps.setString(1, p.id());
            ps.setString(2, p.rewardType());
            ps.setString(3, p.data());
            ps.setString(4, p.desc());
            ps.setLong(5, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Preset getPreset(String id) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id,reward_type,data,desc FROM presets WHERE id=?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return new Preset(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Preset> listPresets() {
        List<Preset> out = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT id,reward_type,data,desc FROM presets ORDER BY created_time")) {
            while (rs.next()) out.add(new Preset(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return out;
    }

    public void removePreset(String id) {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM presets WHERE id=?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ---------- rewards ----------

    public void addReward(Reward r) {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT OR IGNORE INTO rewards(id,player_uuid,player_name,preset_id,reward_type,data,desc,issued_by,issued_time,claimed,claimed_time)
                VALUES(?,?,?,?,?,?,?,?,?,0,NULL)""")) {
            ps.setString(1, r.id());
            ps.setString(2, r.playerUuid());
            ps.setString(3, r.playerName());
            ps.setString(4, r.presetId());
            ps.setString(5, r.rewardType());
            ps.setString(6, r.data());
            ps.setString(7, r.desc());
            ps.setString(8, r.issuedBy());
            ps.setLong(9, r.issuedTime());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** 查询某玩家的全部待领取奖励（按名字或 UUID 匹配）。 */
    public List<Reward> getPending(String playerName, java.util.UUID uuid) {
        List<Reward> out = new ArrayList<>();
        String sql = "SELECT id,player_uuid,player_name,preset_id,reward_type,data,desc,issued_by,issued_time,claimed " +
                "FROM rewards WHERE claimed=0 AND (player_name=? COLLATE NOCASE";
        if (uuid != null) sql += " OR player_uuid=?";
        sql += ") ORDER BY issued_time";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerName);
            if (uuid != null) ps.setString(2, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(rowToReward(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return out;
    }

    public void markClaimed(String rewardId) {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE rewards SET claimed=1, claimed_time=? WHERE id=?")) {
            ps.setLong(1, System.currentTimeMillis());
            ps.setString(2, rewardId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int countPending(String playerName, java.util.UUID uuid) {
        return getPending(playerName, uuid).size();
    }

    public List<Reward> listForPlayer(String playerName) {
        List<Reward> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id,player_uuid,player_name,preset_id,reward_type,data,desc,issued_by,issued_time,claimed " +
                        "FROM rewards WHERE player_name=? COLLATE NOCASE ORDER BY claimed, issued_time DESC")) {
            ps.setString(1, playerName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(rowToReward(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return out;
    }

    /** 发放统计：待领总数 / 已领总数。 */
    public long[] stats() {
        long[] s = {0, 0};
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) total, SUM(CASE WHEN claimed=1 THEN 1 ELSE 0 END) claimed FROM rewards")) {
            if (rs.next()) {
                s[0] = rs.getLong(1);
                s[1] = rs.getLong(2);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return s;
    }

    private Reward rowToReward(ResultSet rs) throws SQLException {
        return new Reward(
                rs.getString("id"),
                rs.getString("player_uuid"),
                rs.getString("player_name"),
                rs.getString("preset_id"),
                rs.getString("reward_type"),
                rs.getString("data"),
                rs.getString("desc"),
                rs.getString("issued_by"),
                rs.getLong("issued_time"),
                rs.getInt("claimed") == 1
        );
    }

    public void close() {
        try { if (conn != null) conn.close(); } catch (SQLException ignored) { }
    }
}
