package com.plantop;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 定时从 Plan 数据库聚合 playtime，内存缓存 Top N。
 * 查询在异步线程执行，不阻塞主线程；失败时保留上次成功缓存。
 */
public class PlaytimeCache implements Runnable {

    private final JavaPlugin plugin;
    private final String url;
    private final String user;
    private final String password;
    private final int serverId;
    private final int topSize;
    private final int queryTimeoutSeconds;
    private final long refreshTicks;

    private BukkitTask task;
    private volatile List<String[]> top = new ArrayList<>(); // [name, seconds]
    private volatile long lastSuccess = 0L;
    private volatile String lastError = "";

    public PlaytimeCache(JavaPlugin plugin) {
        this.plugin = plugin;
        FileConfiguration c = plugin.getConfig();
        String host = c.getString("database.host", "127.0.0.1");
        int port = c.getInt("database.port", 3306);
        String db = c.getString("database.database", "Plan");
        this.user = c.getString("database.user", "root");
        this.password = c.getString("database.password", "");
        this.serverId = c.getInt("database.server-id", 1);
        this.topSize = c.getInt("top-size", 30);
        this.queryTimeoutSeconds = c.getInt("query-timeout-seconds", 15);
        int refreshSeconds = c.getInt("refresh-seconds", 300);
        if (refreshSeconds < 10) {
            refreshSeconds = 10;
        }
        this.refreshTicks = refreshSeconds * 20L;
        this.url = "jdbc:mysql://" + host + ":" + port + "/" + db
                + "?useSSL=false&serverTimezone=Asia/Shanghai&connectTimeout=5000&socketTimeout="
                + (queryTimeoutSeconds * 1000);
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this, 0L, refreshTicks);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
    }

    @Override
    public void run() {
        try {
            refresh();
            lastError = "";
        } catch (Exception e) {
            lastError = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            plugin.getLogger().warning("PlanTop 刷新失败（保留上次缓存）: " + lastError);
        }
    }

    private void refresh() throws SQLException {
        List<String[]> rows = new ArrayList<>();
        try (Connection con = DriverManager.getConnection(url, user, password)) {
            String sql = "SELECT u.name, SUM(s.session_end - s.session_start) / 1000.0 AS secs "
                    + "FROM plan_sessions s JOIN plan_users u ON u.id = s.user_id "
                    + "WHERE s.server_id = ? GROUP BY u.id, u.name ORDER BY secs DESC LIMIT ?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, serverId);
                ps.setInt(2, topSize);
                ps.setQueryTimeout(queryTimeoutSeconds);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        double secs = rs.getDouble(2);
                        rows.add(new String[]{rs.getString(1), String.valueOf((long) secs)});
                    }
                }
            }
        }
        if (rows.isEmpty()) {
            plugin.getLogger().warning("PlanTop 查询结果为空（检查 server-id 与数据库连接）");
        }
        top = rows;
        lastSuccess = System.currentTimeMillis();
    }

    public String get(int rank, String field) {
        if (rank < 1 || rank > top.size()) {
            return "no-data";
        }
        String[] e = top.get(rank - 1);
        if ("time".equals(field)) {
            return fmt(Long.parseLong(e[1]));
        }
        return e[0]; // name / displayname
    }

    public int size() {
        return top.size();
    }

    public long lastSuccess() {
        return lastSuccess;
    }

    public String lastError() {
        return lastError;
    }

    private static String fmt(long seconds) {
        if (seconds < 0) {
            seconds = 0;
        }
        return ((seconds + 30L) / 60L) + "分";
    }
}
