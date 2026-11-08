package com.nangua.orbit;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

/**
 * 定时巡视：由一名巡视员依次环绕观察所有在线玩家。
 *
 * 一轮内锁定巡视员；目标离线立即跳到下一个；一轮结束后等待 interval-seconds
 * 再按最新在线名单重新排队，保证新上线的玩家也会进入下一轮。
 */
public final class TourManager {

    private final OrbitPlugin plugin;
    private final List<Player> queue = new ArrayList<>();
    private BukkitTask task;
    private boolean running;
    private boolean manual;
    private Player manualViewer;
    private Player currentViewer;
    private int index;
    private int secondsLeft;
    private int waitLeft;
    private boolean inRound;

    public TourManager(OrbitPlugin plugin) {
        this.plugin = plugin;
    }

    public void startAuto() {
        if (!plugin.getOrbitConfig().isAutoTourEnabled()) {
            return;
        }
        startTask();
    }

    /** 手动巡视以发起者为巡视员；控制台发起（starter=null）时回退到配置/权限解析。 */
    public void startManual(Player starter) {
        this.manual = true;
        this.manualViewer = starter;
        startTask();
    }

    public void stopManual() {
        shutdown();
    }

    private void startTask() {
        if (running) {
            return;
        }
        running = true;
        inRound = false;
        waitLeft = 0;
        index = 0;
        secondsLeft = 0;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tickSecond, 20L, 20L);
    }

    private void tickSecond() {
        if (!running) {
            return;
        }
        OrbitConfig config = plugin.getOrbitConfig();

        // 巡视员失效（退出/被踢）：清理本轮并重新解析
        if (currentViewer == null || !currentViewer.isOnline()) {
            resetRound();
            currentViewer = resolveViewer();
            if (currentViewer == null) {
                return;
            }
            waitLeft = 0;
        }

        if (!inRound) {
            if (waitLeft > 0) {
                waitLeft--;
                return;
            }
            buildQueue(currentViewer);
            if (queue.isEmpty()) {
                waitLeft = config.getIntervalSeconds();
                return;
            }
            index = 0;
            inRound = true;
            secondsLeft = config.getPerPlayerSeconds();
            if (!startNext(currentViewer)) {
                endRound(config);
            }
            return;
        }

        // 当前目标离线 / 会话意外中断：立刻切下一个，不空等整段 per-player-seconds
        OrbitSession session = plugin.getSession(currentViewer);
        if (session == null || !session.isActive()
                || session.getTarget() == null || !session.getTarget().isOnline()) {
            advance(config);
            return;
        }

        secondsLeft--;
        if (secondsLeft > 0) {
            return;
        }
        advance(config);
    }

    private void advance(OrbitConfig config) {
        index++;
        while (index < queue.size()) {
            secondsLeft = config.getPerPlayerSeconds();
            if (startNext(currentViewer)) {
                return;
            }
            index++;
        }
        endRound(config);
    }

    private void endRound(OrbitConfig config) {
        if (currentViewer != null) {
            plugin.stopSession(currentViewer);
        }
        inRound = false;
        waitLeft = config.getIntervalSeconds();
        queue.clear();
        index = 0;
        secondsLeft = 0;
    }

    private void resetRound() {
        inRound = false;
        queue.clear();
        index = 0;
        secondsLeft = 0;
        waitLeft = 0;
    }

    private void buildQueue(Player viewer) {
        queue.clear();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getUniqueId().equals(viewer.getUniqueId())) {
                continue;
            }
            if (player.hasPermission("orbit.bypass")) {
                continue;
            }
            if (!player.isOnline() || player.isDead()) {
                continue;
            }
            queue.add(player);
        }
    }

    /** @return 是否成功切换到有效目标 */
    private boolean startNext(Player viewer) {
        while (index >= 0 && index < queue.size()) {
            Player target = queue.get(index);
            if (target == null || !target.isOnline()) {
                index++;
                continue;
            }
            OrbitSession existing = plugin.getSession(viewer);
            if (existing != null && existing.isActive()) {
                existing.retarget(target, plugin.getOrbitConfig());
            } else {
                plugin.startSession(viewer, target);
            }
            notifySwitch(viewer, target);
            return true;
        }
        return false;
    }

    private void notifySwitch(Player viewer, Player target) {
        String msg = plugin.getOrbitConfig().getMsgTourSwitch()
                .replace("%player%", target.getName())
                .replace("%index%", String.valueOf(index + 1))
                .replace("%total%", String.valueOf(queue.size()));
        viewer.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
    }

    private Player resolveViewer() {
        // 手动启动优先锁定发起者
        if (manualViewer != null && manualViewer.isOnline()) {
            return manualViewer;
        }
        String name = plugin.getOrbitConfig().getAutoTourViewer();
        if (name != null && !name.isBlank()) {
            Player configured = Bukkit.getPlayerExact(name);
            return configured != null && configured.isOnline() ? configured : null;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("orbit.admin")) {
                return player;
            }
        }
        return null;
    }

    public void onViewerQuit(Player player) {
        if (manualViewer != null && manualViewer.equals(player)) {
            manualViewer = null;
            manual = false;
        }
        if (currentViewer != null && currentViewer.equals(player)) {
            currentViewer = null;
            resetRound();
        }
    }

    public void reload() {
        OrbitConfig config = plugin.getOrbitConfig();
        if (!config.isAutoTourEnabled() && !manual && running) {
            shutdown();
        } else if (config.isAutoTourEnabled() && !running) {
            startAuto();
        }
    }

    public void shutdown() {
        running = false;
        if (task != null) {
            task.cancel();
            task = null;
        }
        if (currentViewer != null && currentViewer.isOnline()) {
            plugin.stopSession(currentViewer);
        }
        resetRound();
        currentViewer = null;
        manualViewer = null;
        manual = false;
    }

    public boolean isRunning() {
        return running;
    }

    public String status() {
        if (!running) {
            return "§7自动巡视：§c未运行 §7(enabled="
                    + plugin.getOrbitConfig().isAutoTourEnabled() + ")";
        }
        OrbitConfig config = plugin.getOrbitConfig();
        StringBuilder sb = new StringBuilder();
        sb.append("§7自动巡视：§a运行中 ").append(manual ? "§7(手动)" : "§7(自动)");
        sb.append("\n§7巡视员：§e")
                .append(currentViewer == null ? "等待在线管理员" : currentViewer.getName());
        if (!inRound) {
            sb.append("\n§7状态：§7等待下一轮 §e").append(waitLeft).append("s");
        } else {
            OrbitSession session = currentViewer == null ? null : plugin.getSession(currentViewer);
            Player target = session == null ? null : session.getTarget();
            sb.append("\n§7当前：§e").append(target == null ? "-" : target.getName())
                    .append(" §7(").append(index + 1).append("/").append(queue.size()).append(")")
                    .append(" §7剩余 §e").append(Math.max(0, secondsLeft)).append("s");
        }
        sb.append("\n§7间隔 §e").append(config.getIntervalSeconds())
                .append("s §7/ 每人 §e").append(config.getPerPlayerSeconds()).append("s");
        return sb.toString();
    }
}
