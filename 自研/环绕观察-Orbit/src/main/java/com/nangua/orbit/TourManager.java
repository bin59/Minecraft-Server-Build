package com.nangua.orbit;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

/**
 * 定时巡视：由一名巡视员依次环绕观察所有在线玩家。
 * 每轮巡视结束后等待 interval-seconds 再开始下一轮。
 */
public final class TourManager {

    private final OrbitPlugin plugin;
    private BukkitTask task;
    private boolean running;
    private boolean manual;
    private Player currentViewer;
    private List<Player> queue = new ArrayList<>();
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

    public void startManual() {
        this.manual = true;
        startTask();
    }

    public void stopManual() {
        this.manual = false;
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
        Player viewer = resolveViewer();
        if (viewer == null) {
            currentViewer = null;
            return;
        }
        currentViewer = viewer;

        if (!inRound) {
            if (waitLeft > 0) {
                waitLeft--;
                return;
            }
            queue = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getUniqueId().equals(viewer.getUniqueId())) {
                    continue;
                }
                if (player.hasPermission("orbit.bypass")) {
                    continue;
                }
                queue.add(player);
            }
            index = 0;
            if (queue.isEmpty()) {
                waitLeft = config.getIntervalSeconds();
                return;
            }
            inRound = true;
            secondsLeft = config.getPerPlayerSeconds();
            startNext(viewer);
            return;
        }

        secondsLeft--;
        if (secondsLeft > 0) {
            return;
        }
        index++;
        if (index >= queue.size()) {
            plugin.stopSession(viewer);
            inRound = false;
            waitLeft = config.getIntervalSeconds();
        } else {
            secondsLeft = config.getPerPlayerSeconds();
            startNext(viewer);
        }
    }

    private void startNext(Player viewer) {
        if (index < 0 || index >= queue.size()) {
            return;
        }
        Player target = queue.get(index);
        if (target == null || !target.isOnline()) {
            return;
        }
        OrbitSession existing = plugin.getSession(viewer);
        if (existing != null && existing.isActive()) {
            existing.retarget(target, plugin.getOrbitConfig());
        } else {
            plugin.startSession(viewer, target);
        }
    }

    private Player resolveViewer() {
        String name = plugin.getOrbitConfig().getAutoTourViewer();
        if (name != null && !name.isBlank()) {
            return Bukkit.getPlayerExact(name);
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("orbit.admin")) {
                return player;
            }
        }
        return null;
    }

    public void onViewerQuit(Player player) {
        if (running && player != null && player.equals(currentViewer)) {
            currentViewer = null;
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
        inRound = false;
        currentViewer = null;
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public boolean isRunning() {
        return running;
    }
}
