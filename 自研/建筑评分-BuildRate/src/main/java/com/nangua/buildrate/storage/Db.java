package com.nangua.buildrate.storage;

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

/** SQLite 数据访问层（events / builds / scores） */
public final class Db {
    private final String url;

    public Db(File dbFile) {
        this.url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        init();
    }

    private Connection open() throws SQLException {
        Connection c = DriverManager.getConnection(url);
        try (Statement s = c.createStatement()) {
            s.execute("PRAGMA busy_timeout=5000");
            s.execute("PRAGMA journal_mode=WAL");
        }
        return c;
    }

    private void init() {
        try (Connection c = open(); Statement s = c.createStatement()) {
            s.execute("CREATE TABLE IF NOT EXISTS events("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "name TEXT NOT NULL,"
                    + "status INTEGER NOT NULL DEFAULT 1,"
                    + "created_at INTEGER NOT NULL)");
            s.execute("CREATE TABLE IF NOT EXISTS builds("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "event_id INTEGER NOT NULL,"
                    + "player TEXT NOT NULL,"
                    + "name TEXT NOT NULL,"
                    + "world TEXT,"
                    + "x INTEGER, y INTEGER, z INTEGER)");
            s.execute("CREATE TABLE IF NOT EXISTS scores("
                    + "event_id INTEGER NOT NULL,"
                    + "build_id INTEGER NOT NULL,"
                    + "voter TEXT NOT NULL,"
                    + "score INTEGER NOT NULL,"
                    + "updated_at INTEGER NOT NULL,"
                    + "PRIMARY KEY(build_id, voter))");
        } catch (SQLException e) {
            throw new RuntimeException("初始化 BuildRate 数据库失败", e);
        }
    }

    // ---------- 活动 ----------

    /** 创建新活动；若已存在进行中的活动则先关闭它（同一时间仅一场进行中） */
    public Models.Event createEvent(String name) {
        try (Connection c = open()) {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE events SET status=2 WHERE status=1")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO events(name,status,created_at) VALUES(?,0,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, name);
                ps.setLong(2, System.currentTimeMillis());
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return new Models.Event(rs.getInt(1), name, 0, System.currentTimeMillis());
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("创建活动失败", e);
        }
        return null;
    }

    public Models.Event getActiveEvent() {
        return queryEvent("SELECT * FROM events WHERE status=1 ORDER BY id DESC LIMIT 1");
    }

    /** 报名/查看用：取最近一场尚未结束的活动（待开始0 或 进行中1） */
    public Models.Event getOpenEvent() {
        return queryEvent("SELECT * FROM events WHERE status IN (0,1) ORDER BY id DESC LIMIT 1");
    }

    public Models.Event getEvent(int id) {
        return queryEvent("SELECT * FROM events WHERE id=" + id);
    }

    public List<Models.Event> listEvents() {
        List<Models.Event> out = new ArrayList<>();
        try (Connection c = open();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT * FROM events ORDER BY id DESC")) {
            while (rs.next()) out.add(mapEvent(rs));
        } catch (SQLException e) {
            throw new RuntimeException("查询活动列表失败", e);
        }
        return out;
    }

    private Models.Event queryEvent(String sql) {
        try (Connection c = open();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            return rs.next() ? mapEvent(rs) : null;
        } catch (SQLException e) {
            throw new RuntimeException("查询活动失败", e);
        }
    }

    private Models.Event mapEvent(ResultSet rs) throws SQLException {
        return new Models.Event(rs.getInt("id"), rs.getString("name"),
                rs.getInt("status"), rs.getLong("created_at"));
    }

    public void setEventStatus(int eventId, int status) {
        try (Connection c = open();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE events SET status=? WHERE id=?")) {
            ps.setInt(1, status);
            ps.setInt(2, eventId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("更新活动状态失败", e);
        }
    }

    // ---------- 参评建筑 ----------

    public Models.Build addBuild(int eventId, String player, String name,
                                 String world, int x, int y, int z) {
        try (Connection c = open();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO builds(event_id,player,name,world,x,y,z) VALUES(?,?,?,?,?,?,?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, eventId);
            ps.setString(2, player);
            ps.setString(3, name);
            ps.setString(4, world);
            ps.setInt(5, x);
            ps.setInt(6, y);
            ps.setInt(7, z);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return new Models.Build(rs.getInt(1), eventId, player, name, world, x, y, z);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("添加参评建筑失败", e);
        }
        return null;
    }

    /** 某玩家在某活动中的参评建筑（用于自主报名去重 / 改名） */
    public Models.Build getPlayerBuild(int eventId, String player) {
        List<Models.Build> all = getBuilds(eventId);
        String low = player.toLowerCase();
        for (Models.Build b : all) {
            if (b.player().equalsIgnoreCase(low)) return b;
        }
        return null;
    }

    public List<Models.Build> getBuilds(int eventId) {
        List<Models.Build> out = new ArrayList<>();
        try (Connection c = open();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT * FROM builds WHERE event_id=" + eventId + " ORDER BY id")) {
            while (rs.next()) out.add(mapBuild(rs));
        } catch (SQLException e) {
            throw new RuntimeException("查询参评建筑失败", e);
        }
        return out;
    }

    public Models.Build getBuild(int id) {
        try (Connection c = open();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT * FROM builds WHERE id=" + id)) {
            return rs.next() ? mapBuild(rs) : null;
        } catch (SQLException e) {
            throw new RuntimeException("查询建筑失败", e);
        }
    }

    private Models.Build mapBuild(ResultSet rs) throws SQLException {
        return new Models.Build(rs.getInt("id"), rs.getInt("event_id"),
                rs.getString("player"), rs.getString("name"),
                rs.getString("world"), rs.getInt("x"), rs.getInt("y"), rs.getInt("z"));
    }

    public void removeBuild(int buildId) {
        try (Connection c = open()) {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM scores WHERE build_id=?")) {
                ps.setInt(1, buildId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM builds WHERE id=?")) {
                ps.setInt(1, buildId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("移除参评建筑失败", e);
        }
    }

    /** 修改建筑名（自主报名重新报名即视为改名） */
    public void updateBuildName(int buildId, String newName) {
        try (Connection c = open();
             PreparedStatement ps = c.prepareStatement("UPDATE builds SET name=? WHERE id=?")) {
            ps.setString(1, newName);
            ps.setInt(2, buildId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("修改建筑名失败", e);
        }
    }

    // ---------- 评分 ----------

    /** 写分 / 改分：同一评分人同一建筑覆盖为最新分 */
    public void upsertScore(int eventId, int buildId, String voter, int score) {
        try (Connection c = open();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO scores(event_id,build_id,voter,score,updated_at) VALUES(?,?,?,?,?)"
                     + " ON CONFLICT(build_id,voter) DO UPDATE SET score=excluded.score, updated_at=excluded.updated_at")) {
            ps.setInt(1, eventId);
            ps.setInt(2, buildId);
            ps.setString(3, voter);
            ps.setInt(4, score);
            ps.setLong(5, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("写入评分失败", e);
        }
    }

    public List<Models.Score> getScores(int buildId) {
        List<Models.Score> out = new ArrayList<>();
        try (Connection c = open();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT * FROM scores WHERE build_id=" + buildId)) {
            while (rs.next()) {
                out.add(new Models.Score(rs.getInt("build_id"), rs.getString("voter"),
                        rs.getInt("score"), rs.getLong("updated_at")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询评分失败", e);
        }
        return out;
    }

    /** 某活动全部评分，按 build_id 分组 */
    public Map<Integer, List<Models.Score>> getScoresByBuild(int eventId) {
        Map<Integer, List<Models.Score>> out = new LinkedHashMap<>();
        try (Connection c = open();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(
                     "SELECT * FROM scores WHERE event_id=" + eventId + " ORDER BY build_id")) {
            while (rs.next()) {
                Models.Score sc = new Models.Score(rs.getInt("build_id"), rs.getString("voter"),
                        rs.getInt("score"), rs.getLong("updated_at"));
                out.computeIfAbsent(sc.buildId(), k -> new ArrayList<>()).add(sc);
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询活动评分失败", e);
        }
        return out;
    }

    /** 某活动每个评分人投了几票（参与度统计） */
    public Map<String, Integer> getVoterStats(int eventId) {
        Map<String, Integer> out = new LinkedHashMap<>();
        try (Connection c = open();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(
                     "SELECT voter, COUNT(*) AS n FROM scores WHERE event_id=" + eventId
                     + " GROUP BY voter ORDER BY n DESC")) {
            while (rs.next()) out.put(rs.getString("voter"), rs.getInt("n"));
        } catch (SQLException e) {
            throw new RuntimeException("查询投票统计失败", e);
        }
        return out;
    }

    /** 某评分人已投给某建筑的分数（面板显示当前分；未投返回 -1） */
    public int getMyScore(int buildId, String voter) {
        try (Connection c = open();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT score FROM scores WHERE build_id=? AND lower(voter)=lower(?)")) {
            ps.setInt(1, buildId);
            ps.setString(2, voter);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询个人评分失败", e);
        }
    }
}
