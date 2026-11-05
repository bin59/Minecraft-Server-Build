package com.nangua.orbit;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class OrbitPlugin extends JavaPlugin implements Listener {

    private final Map<UUID, OrbitSession> sessions = new ConcurrentHashMap<>();
    private OrbitConfig config;
    private TourManager tourManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        this.config = new OrbitConfig(getConfig());

        OrbitCommand command = new OrbitCommand(this);
        if (getCommand("orbit") != null) {
            getCommand("orbit").setExecutor(command);
            getCommand("orbit").setTabCompleter(command);
        }

        Bukkit.getPluginManager().registerEvents(this, this);

        this.tourManager = new TourManager(this);
        this.tourManager.startAuto();

        getLogger().info("Orbit 已启用");
    }

    @Override
    public void onDisable() {
        if (tourManager != null) {
            tourManager.shutdown();
        }
        for (OrbitSession session : sessions.values()) {
            session.stop();
        }
        sessions.clear();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        OrbitSession session = sessions.remove(player.getUniqueId());
        if (session != null) {
            session.stop();
        }
        if (tourManager != null) {
            tourManager.onViewerQuit(player);
        }
    }

    public OrbitConfig getOrbitConfig() {
        return config;
    }

    public void reloadPlugin() {
        reloadConfig();
        this.config = new OrbitConfig(getConfig());
        for (OrbitSession session : sessions.values()) {
            session.updateConfig(config);
        }
        if (tourManager != null) {
            tourManager.reload();
        }
    }

    public OrbitSession getSession(Player viewer) {
        return sessions.get(viewer.getUniqueId());
    }

    public OrbitSession startSession(Player viewer, Player target) {
        OrbitSession existing = sessions.remove(viewer.getUniqueId());
        if (existing != null) {
            existing.stop();
        }
        OrbitSession session = new OrbitSession(this, viewer, target, config);
        sessions.put(viewer.getUniqueId(), session);
        session.start();
        return session;
    }

    public void stopSession(Player viewer) {
        OrbitSession session = sessions.remove(viewer.getUniqueId());
        if (session != null) {
            session.stop();
        }
    }

    public TourManager getTourManager() {
        return tourManager;
    }
}
