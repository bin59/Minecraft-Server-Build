package com.nangua.pumpkinmail.storage;

import java.io.File;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * SQLite 存储层。三张表：
 *  - presets     奖品库
 *  - rewards     玩家邮箱条目（每笔发放一行，领取只改状态，永不删除）
 *  - reward_log  **全量审计流水**（只追加）：发放成功 / 发放被去重 / 领取成功 / 重复领取 / 领取失败，
 *                每条都记下来源、对象、真实奖励内容、时间与结果，用于和南瓜账本对账。
 *
 * 设计约束（2026-11-12 起）：
 *  1) 「所有都要记录」：rewards 的写入与 reward_log 的 ISSUE 在**同一个事务**里提交，
 *     任何一条失败就整体回滚，绝不允许出现「发了钱但没记录」或「有记录但没发」。
 *  2) 「要准确」：发放不再伪造离线模式 UUID；desc / issued_by / source 必填；
 *     stats 返回真实总数/已领/待领；重复发放与领取失败同样留痕。
 *  3) 二进制兼容：addReward(Reward) / markClaimed(String) / stats() / listForPlayer(String)
 *     等旧签名一字不改，OfflineReward、BuildRate 等已编译插件无需重新编译。
 *  4) 旧库自动迁移（ALTER 加列）+ 幂等回填历史事件，原记录字段一个都不改写。
 */
public class Storage {

    private final Connection conn;

    /**
     * 待领数量缓存：key = player_uuid（无 uuid 时取小写玩家名），value = 该玩家待领条数。
     * 所有读写都在 synchronized 方法内，与 conn 同一把锁；写入成功时自增，
     * 领取 / 异常时失效对应条目，下次 countPending 回退查库回填。
     */
    private final Map<String, Integer> pendingCache = new HashMap<>();
    private static final SimpleDateFormat FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public Storage(File dbFile) throws Exception {
        Class.forName("org.sqlite.JDBC");
        this.conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        try (Statement st = conn.createStatement()) {
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
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS reward_log(
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    reward_id TEXT NOT NULL,
                    event TEXT NOT NULL,
                    event_time INTEGER NOT NULL,
                    player_uuid TEXT,
                    player_name TEXT,
                    issued_by TEXT,
                    source TEXT,
                    reward_type TEXT,
                    data TEXT,
                    desc TEXT,
                    detail TEXT
                )""");
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_log_reward ON reward_log(reward_id, event)");
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_log_player ON reward_log(player_name, event_time)");
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_log_time ON reward_log(event_time)");
        }
        migrateColumns();
        backfillSource();
        backfillLog();
    }

    // ================= 旧库迁移与历史回填（幂等，不改写任何原有字段） =================

    private void migrateColumns() throws SQLException {
        Set<String> cols = new HashSet<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA table_info(rewards)")) {
            while (rs.next()) cols.add(rs.getString("name"));
        }
        try (Statement st = conn.createStatement()) {
            if (!cols.contains("source")) st.executeUpdate("ALTER TABLE rewards ADD COLUMN source TEXT");
            if (!cols.contains("claimed_by")) st.executeUpdate("ALTER TABLE rewards ADD COLUMN claimed_by TEXT");
            if (!cols.contains("claimed_name")) st.executeUpdate("ALTER TABLE rewards ADD COLUMN claimed_name TEXT");
        }
    }

    /** 只填充新增的 source 列；历史原有字段保持原值。 */
    private void backfillSource() {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("""
                UPDATE rewards SET source = CASE
                    WHEN issued_by='OfflineReward' THEN 'OfflineReward:' || COALESCE(NULLIF(preset_id,''),'unknown')
                    WHEN issued_by='BuildRate' THEN 'BuildRate'
                    ELSE 'MANUAL:' || COALESCE(NULLIF(issued_by,''),'UNKNOWN') END
                WHERE source IS NULL OR source=''""");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 为迁移前已有的 rewards 补 ISSUE / CLAIM 审计事件。
     * 用 NOT EXISTS 做幂等，每次启动都跑一遍也只会补缺失的，不会重复。
     */
    private void backfillLog() {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("""
                INSERT INTO reward_log(reward_id,event,event_time,player_uuid,player_name,issued_by,source,reward_type,data,desc,detail)
                SELECT r.id,'ISSUE',COALESCE(r.issued_time,0),r.player_uuid,r.player_name,r.issued_by,r.source,
                       r.reward_type,r.data,r.desc,'BACKFILL:迁移前历史发放'
                FROM rewards r
                WHERE NOT EXISTS (SELECT 1 FROM reward_log l WHERE l.reward_id=r.id AND l.event='ISSUE')""");
            st.executeUpdate("""
                INSERT INTO reward_log(reward_id,event,event_time,player_uuid,player_name,issued_by,source,reward_type,data,desc,detail)
                SELECT r.id,'CLAIM',r.claimed_time,r.player_uuid,r.player_name,r.issued_by,r.source,
                       r.reward_type,r.data,r.desc,'BACKFILL:迁移前历史领取'
                FROM rewards r
                WHERE r.claimed=1 AND r.claimed_time IS NOT NULL
                  AND NOT EXISTS (SELECT 1 FROM reward_log l WHERE l.reward_id=r.id AND l.event='CLAIM')""");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ================= presets =================

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

    // ================= rewards：写入 =================

    /** 兼容旧插件的入口（OfflineReward / BuildRate 编译期就绑定了这个 void 签名）。 */
    public synchronized void addReward(Reward r) {
        addRewardLogged(r, null);
    }

    /**
     * 写入一条发放，并在同一事务里写审计流水。
     *
     * @param source 来源分类；为空时按 issuedBy / presetId 推断
     * @return 是否真的插入了新行（false = id 重复被去重，或写库失败）
     */
    public synchronized boolean addRewardLogged(Reward r, String source) {
        if (r == null || r.id() == null || r.id().isBlank()) return false;
        String desc = (r.desc() == null || r.desc().isBlank()) ? defaultDesc(r) : r.desc();
        String issuer = (r.issuedBy() == null || r.issuedBy().isBlank()) ? "SYSTEM" : r.issuedBy();
        String src = (source == null || source.isBlank()) ? inferSource(issuer, r.presetId()) : source;
        long now = r.issuedTime() > 0 ? r.issuedTime() : System.currentTimeMillis();
        boolean auto = true;
        try {
            auto = conn.getAutoCommit();
            conn.setAutoCommit(false);
            int affected;
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT OR IGNORE INTO rewards
                        (id,player_uuid,player_name,preset_id,reward_type,data,desc,issued_by,issued_time,source,claimed,claimed_time)
                    VALUES(?,?,?,?,?,?,?,?,?,?,0,NULL)""")) {
                ps.setString(1, r.id());
                ps.setString(2, r.playerUuid());
                ps.setString(3, r.playerName());
                ps.setString(4, r.presetId());
                ps.setString(5, r.rewardType());
                ps.setString(6, r.data());
                ps.setString(7, desc);
                ps.setString(8, issuer);
                ps.setLong(9, now);
                ps.setString(10, src);
                affected = ps.executeUpdate();
            }
            insertLog(r.id(), affected > 0 ? "ISSUE" : "ISSUE_DUP", now,
                    r.playerUuid(), r.playerName(), issuer, src,
                    r.rewardType(), r.data(), desc,
                    affected > 0 ? "发放成功" : "发放被忽略：id 已存在（重复发放）");
            conn.commit();
            if (affected > 0) {
                String key = cacheKey(r.playerName(), r.playerUuid());
                if (pendingCache.containsKey(key)) pendingCache.merge(key, affected, Integer::sum);
            }
            return affected > 0;
        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ignored) { }
            e.printStackTrace();
            return false;
        } finally {
            try { conn.setAutoCommit(auto); } catch (SQLException ignored) { }
        }
    }

    private void insertLog(String rewardId, String event, long time, String uuid, String name,
                           String issuer, String source, String type, String data,
                           String desc, String detail) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO reward_log(reward_id,event,event_time,player_uuid,player_name,issued_by,source,
                                       reward_type,data,desc,detail)
                VALUES(?,?,?,?,?,?,?,?,?,?,?)""")) {
            ps.setString(1, rewardId);
            ps.setString(2, event);
            ps.setLong(3, time);
            ps.setString(4, uuid);
            ps.setString(5, name);
            ps.setString(6, issuer);
            ps.setString(7, source);
            ps.setString(8, type);
            ps.setString(9, data);
            ps.setString(10, desc);
            ps.setString(11, detail);
            ps.executeUpdate();
        }
    }

    private static String defaultDesc(Reward r) {
        String base = (r.presetId() == null || r.presetId().isBlank())
                ? String.valueOf(r.rewardType()) : r.presetId();
        return base + "：" + String.valueOf(r.data());
    }

    private static String inferSource(String issuer, String presetId) {
        if ("OfflineReward".equals(issuer)) {
            return "OfflineReward:" + (presetId == null || presetId.isBlank() ? "unknown" : presetId);
        }
        if ("BuildRate".equals(issuer)) return "BuildRate";
        return "MANUAL:" + issuer;
    }

    /** 取一条 rewards 的审计所需字段；不存在返回 null。 */
    private String[] lookup(String rewardId) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT player_uuid,player_name,issued_by,source,reward_type,data,desc FROM rewards WHERE id=?")) {
            ps.setString(1, rewardId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new String[]{rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                            rs.getString(5), rs.getString(6), rs.getString(7)};
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ================= rewards：领取 =================

    /** 兼容旧签名：无领取人信息时按记录本身归属记账。 */
    public synchronized void markClaimed(String rewardId) {
        markClaimed(rewardId, null, null);
    }

    /**
     * 领取成功：只把 claimed=0 的行改成已领（不会重复刷新 claimed_time），
     * 记录真实领取人，并写 CLAIM 审计；重复点击写 CLAIM_DUP，不吞掉事件。
     */
    public synchronized void markClaimed(String rewardId, String claimantUuid, String claimantName) {
        String[] row = lookup(rewardId);
        if (row == null) {
            try {
                insertLog(rewardId, "CLAIM_MISSING", System.currentTimeMillis(),
                        claimantUuid, claimantName, null, null, null, null, null,
                        "领取失败：rewards 中找不到该 id");
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return;
        }
        long now = System.currentTimeMillis();
        boolean auto = true;
        try {
            auto = conn.getAutoCommit();
            conn.setAutoCommit(false);
            int affected;
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE rewards SET claimed=1, claimed_time=?, claimed_by=?, claimed_name=? WHERE id=? AND claimed=0")) {
                ps.setLong(1, now);
                ps.setString(2, claimantUuid);
                ps.setString(3, claimantName);
                ps.setString(4, rewardId);
                affected = ps.executeUpdate();
            }
            insertLog(rewardId, affected > 0 ? "CLAIM" : "CLAIM_DUP", now,
                    claimantUuid != null ? claimantUuid : row[0],
                    claimantName != null ? claimantName : row[1],
                    row[2], row[3], row[4], row[5], row[6],
                    affected > 0 ? "领取成功" : "重复领取：该奖励此前已标记为已领");
            conn.commit();
        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ignored) { }
            e.printStackTrace();
        } finally {
            try { conn.setAutoCommit(auto); } catch (SQLException ignored) { }
        }
        pendingCache.remove(cacheKey(row[1], row[0]));
    }

    /** 领取失败（命令发放失败 / 物品解析失败 / 未知类型）同样留痕，便于排查漏发。 */
    public synchronized void logClaimFailed(String rewardId, String playerUuid, String playerName, String reason) {
        String[] row = lookup(rewardId);
        try {
            insertLog(rewardId, "CLAIM_FAIL", System.currentTimeMillis(),
                    playerUuid, playerName,
                    row == null ? null : row[2], row == null ? null : row[3],
                    row == null ? null : row[4], row == null ? null : row[5],
                    row == null ? null : row[6],
                    "领取失败：" + (reason == null ? "未知原因" : reason));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ================= rewards：查询 =================

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

    public synchronized int countPending(String playerName, java.util.UUID uuid) {
        String key = cacheKey(playerName, uuid == null ? null : uuid.toString());
        Integer cached = pendingCache.get(key);
        if (cached != null) return cached;
        int n = countPendingFromDb(playerName, uuid);
        pendingCache.put(key, n);
        return n;
    }

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

    private String cacheKey(String playerName, String playerUuid) {
        if (playerUuid != null && !playerUuid.isEmpty()) return playerUuid;
        return playerName == null ? "" : playerName.toLowerCase(Locale.ROOT);
    }

    public synchronized void clearPendingCache() {
        pendingCache.clear();
    }

    public synchronized List<Reward> listForPlayer(String playerName) {
        return listForPlayer(playerName, null);
    }

    /** 按名字 + 真实 UUID 一起查，避免改名 / 基岩前缀导致记录查不全。 */
    public synchronized List<Reward> listForPlayer(String playerName, java.util.UUID uuid) {
        List<Reward> out = new ArrayList<>();
        String u = uuid == null ? null : uuid.toString();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id,player_uuid,player_name,preset_id,reward_type,data,desc,issued_by,issued_time,claimed " +
                        "FROM rewards WHERE (player_name=? COLLATE NOCASE OR (? IS NOT NULL AND player_uuid=?)) " +
                        "ORDER BY claimed, issued_time DESC")) {
            ps.setString(1, playerName);
            ps.setString(2, u);
            ps.setString(3, u);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(rowToReward(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return out;
    }

    /** 统计：{总数, 已领, 待领}——旧版误把总数当待领显示，这里返回真实三项。 */
    public synchronized long[] stats() {
        long[] s = {0, 0, 0};
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT COUNT(*), COALESCE(SUM(CASE WHEN claimed=1 THEN 1 ELSE 0 END),0), " +
                             "COALESCE(SUM(CASE WHEN claimed=0 THEN 1 ELSE 0 END),0) FROM rewards")) {
            if (rs.next()) {
                s[0] = rs.getLong(1);
                s[1] = rs.getLong(2);
                s[2] = rs.getLong(3);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return s;
    }

    // ================= reward_log：查询与对账 =================

    /** 审计流水；player 为空时查全服，limit <= 0 时默认 20，最多 200 条。 */
    public synchronized List<String> listLog(String player, int limit) {
        List<String> out = new ArrayList<>();
        int n = limit <= 0 ? 20 : Math.min(limit, 200);
        boolean byPlayer = player != null && !player.isBlank();
        String sql = "SELECT event_time,event,player_name,reward_type,data,desc,issued_by,source,detail FROM reward_log " +
                (byPlayer ? "WHERE player_name=? COLLATE NOCASE " : "") +
                "ORDER BY event_time DESC, id DESC LIMIT ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int i = 1;
            if (byPlayer) ps.setString(i++, player);
            ps.setInt(i, n);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add("&7" + FMT.format(new Date(rs.getLong(1))) +
                            " &e[" + rs.getString(2) + "]&f " + rs.getString(3) +
                            " &7" + nullSafe(rs.getString(4)) + " " + nullSafe(rs.getString(5)) +
                            " &8来源=" + nullSafe(rs.getString(8)) +
                            " 发放人=" + nullSafe(rs.getString(7)) +
                            " &7" + nullSafe(rs.getString(9)));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return out;
    }

    /** 各事件计数 + 总数，用于 /mailbox stats。 */
    public synchronized Map<String, Long> logStats() {
        Map<String, Long> out = new LinkedHashMap<>();
        out.put("TOTAL", 0L);
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT event, COUNT(*) FROM reward_log GROUP BY event")) {
            long total = 0;
            while (rs.next()) {
                out.put(rs.getString(1), rs.getLong(2));
                total += rs.getLong(2);
            }
            out.put("TOTAL", total);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return out;
    }

    /** 对账：任何一笔发放/领取缺流水、或关键字段为空，都算「不准确」并列出来。 */
    public synchronized String verify() {
        StringBuilder sb = new StringBuilder();
        try (Statement st = conn.createStatement()) {
            long rewards = one(st, "SELECT COUNT(*) FROM rewards");
            long logs = one(st, "SELECT COUNT(*) FROM reward_log");
            long missingIssue = one(st, "SELECT COUNT(*) FROM rewards r WHERE NOT EXISTS " +
                    "(SELECT 1 FROM reward_log l WHERE l.reward_id=r.id AND l.event='ISSUE')");
            long missingClaim = one(st, "SELECT COUNT(*) FROM rewards r WHERE r.claimed=1 AND NOT EXISTS " +
                    "(SELECT 1 FROM reward_log l WHERE l.reward_id=r.id AND l.event='CLAIM')");
            long wrongData = one(st, "SELECT COUNT(*) FROM rewards r JOIN reward_log l " +
                    "ON l.reward_id=r.id AND l.event='ISSUE' WHERE COALESCE(l.data,'')<>COALESCE(r.data,'')");
            long blankDesc = one(st, "SELECT COUNT(*) FROM rewards WHERE desc IS NULL OR TRIM(desc)=''");
            long blankUuid = one(st, "SELECT COUNT(*) FROM rewards WHERE player_uuid IS NULL OR TRIM(player_uuid)=''");
            long blankSource = one(st, "SELECT COUNT(*) FROM rewards WHERE source IS NULL OR TRIM(source)=''");
            long fail = one(st, "SELECT COUNT(*) FROM reward_log WHERE event='CLAIM_FAIL'");
            sb.append("&e邮箱对账：&f邮件 &e").append(rewards)
                    .append(" &f条 / 审计事件 &e").append(logs).append(" &f条\n");
            sb.append("  &7缺发放流水 &f").append(missingIssue)
                    .append(" &7| 缺领取流水 &f").append(missingClaim)
                    .append(" &7| 金额不一致 &f").append(wrongData).append("\n");
            sb.append("  &7描述为空 &f").append(blankDesc)
                    .append(" &7| UUID 为空 &f").append(blankUuid)
                    .append(" &7| 来源为空 &f").append(blankSource)
                    .append(" &7| 领取失败事件 &f").append(fail).append("\n");
            boolean ok = missingIssue == 0 && missingClaim == 0 && wrongData == 0
                    && blankDesc == 0 && blankSource == 0;
            sb.append(ok ? "  &a✔ 发放与领取流水完整、金额一致、必填字段齐全"
                    : "  &c✘ 存在缺口，见上方计数（历史遗留记录请单独清洗）");
        } catch (SQLException e) {
            e.printStackTrace();
            return "&c对账失败：" + e.getMessage();
        }
        return sb.toString();
    }

    private static long one(Statement st, String sql) throws SQLException {
        try (ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }

    private static String nullSafe(String s) {
        return s == null || s.isBlank() ? "-" : s;
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
