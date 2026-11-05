package com.nangua.pumpkinmail.storage;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * SQLite 存储层。负责 presets（奖品库）与 rewards（玩家邮箱）两张表。
 * 底层由 sqlite-jdbc 提供（已内嵌进插件 jar）。
 */
public class Storage {

    private final Connection conn;

    /**
     * 待领数量缓存：key = player_uuid（无 uuid 时取小写玩家名），value = 该玩家待领条数。
     * 仅用于 countPending() 加速 HintTask 高频轮询。所有读写都在 synchronized 方法内，
     * 与 conn 同一把锁，天然线程安全。
     * 一致性：addReward 成功写入时自增；markClaimed 时按玩家失效该条缓存（下次查库回填）。
     * 防膨胀：条目只对"被 countPending 查过的玩家"存在；markClaimed 后即移除；
     * Storage 实例随插件 onDisable/服务器重启销毁，缓存自然清空，下次回退查库。
     */
    private final Map<String, Integer> pendingCache = new HashMap<>();

    public Storage(File dbFile) throws Exception {
        Class.forName("org.sqlite.JDBC");
        this.conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        try (Statement st = conn.createStatement()) {
            // 性能：WAL 模式 + NORMAL 同步 + 忙等超时，降低高频轮询/写入时的锁竞争
            st.executeUpdate("PRAGMA journal_mode=WAL");
            st.executeUpdate("PRAGMA synchronous=NORMAL");
            st.executeUpdate("PRAGMA busy_timeout=5000");
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

    public synchronized void savePreset(Preset p) {
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

    public synchronized Preset getPreset(String id) {
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

    public synchronized List<Preset> listPresets() {
        List<Preset> out = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT id,reward_type,data,desc FROM presets ORDER BY created_time")) {
            while (rs.next()) out.add(new Preset(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return out;
    }

    public synchronized void removePreset(String id) {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM presets WHERE id=?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ---------- rewards ----------

    public synchronized void addReward(Reward r) {
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
            int affected = ps.executeUpdate();
            // 仅当真的插入了新行（INSERT OR IGNORE 未去重）时，才推进该玩家的待领缓存。
            // 若缓存中没有该玩家（从未被 countPending 过），不主动写入——下次 countPending 会直接查库回填，
            // 避免基于未知旧值自增造成偏差。
            if (affected > 0) {
                String key = cacheKey(r.playerName(), r.playerUuid());
                if (pendingCache.containsKey(key)) {
                    pendingCache.merge(key, affected, Integer::sum);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** 查询某玩家的全部待领取奖励（按名字或 UUID 匹配）。 */
    public synchronized List<Reward> getPending(String playerName, java.util.UUID uuid) {
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

    public synchronized void markClaimed(String rewardId) {
        // 先查出该 reward 归属的玩家，便于精准失效对应缓存条目（而非全量清空）。
        String ownerName = null;
        String ownerUuid = null;
        try (PreparedStatement sel = conn.prepareStatement(
                "SELECT player_name, player_uuid FROM rewards WHERE id=?")) {
            sel.setString(1, rewardId);
            try (ResultSet rs = sel.executeQuery()) {
                if (rs.next()) {
                    ownerName = rs.getString(1);
                    ownerUuid = rs.getString(2);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE rewards SET claimed=1, claimed_time=? WHERE id=?")) {
            ps.setLong(1, System.currentTimeMillis());
            ps.setString(2, rewardId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        // 不做"缓存 -1"（无法确切知道剩余条数：可能同 ID 多发、可能并发），
        // 直接失效该玩家条目，下次 countPending 回退查库回填，保证最终一致。
        if (ownerName != null || ownerUuid != null) {
            pendingCache.remove(cacheKey(ownerName, ownerUuid));
        } else {
            pendingCache.clear();
        }
    }

    /**
     * 待领数量：优先命中 pendingCache；未命中时执行 COUNT(*) 查询并回填缓存。
     * 异常时回退直查（本方法本身就是兜底路径），吞掉 SQL 异常并返回 0，与历史行为一致。
     */
    public synchronized int countPending(String playerName, java.util.UUID uuid) {
        String key = cacheKey(playerName, uuid == null ? null : uuid.toString());
        Integer cached = pendingCache.get(key);
        if (cached != null) return cached;
        int n = countPendingFromDb(playerName, uuid);
        pendingCache.put(key, n);
        return n;
    }

    /** 直接执行 COUNT(*)，不再物化整行 Reward。 */
    private int countPendingFromDb(String playerName, java.util.UUID uuid) {
        String sql = "SELECT COUNT(*) FROM rewards WHERE claimed=0 AND (player_name=? COLLATE NOCASE";
        if (uuid != null) sql += " OR player_uuid=?";
        sql += ")";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerName);
            if (uuid != null) ps.setString(2, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /** 缓存 key：优先 uuid，否则小写玩家名。与查询语义（name NOCASE OR uuid）对齐。 */
    private String cacheKey(String playerName, String playerUuid) {
        if (playerUuid != null && !playerUuid.isEmpty()) return playerUuid;
        return playerName == null ? "" : playerName.toLowerCase(Locale.ROOT);
    }

    /** 清空待领缓存（插件 onDisable / 重启时调用；下次 countPending 自动回退数据库回填）。 */
    public synchronized void clearPendingCache() {
        pendingCache.clear();
    }

    public synchronized List<Reward> listForPlayer(String playerName) {
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
    public synchronized long[] stats() {
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

    public synchronized void close() {
        try { if (conn != null) conn.close(); } catch (SQLException ignored) { }
    }
}
