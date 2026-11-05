package com.nangua.buildrate;

import com.nangua.buildrate.cmd.BuildRateCommand;
import com.nangua.buildrate.gui.GuiManager;
import com.nangua.buildrate.storage.Db;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/** 建筑评分 - 活动制多玩家打分评审 */
public final class BuildRate extends JavaPlugin {
    private static BuildRate instance;
    private Db db;
    private GuiManager gui;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        File dataFolder = getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            getLogger().warning("无法创建数据目录：" + dataFolder);
        }
        db = new Db(new File(dataFolder, "data.db"));
        gui = new GuiManager(this);
        getCommand("br").setExecutor(new BuildRateCommand(this));
        getServer().getPluginManager().registerEvents(gui, this);
        getLogger().info("BuildRate 建筑评分 v" + getDescription().getVersion() + " 已启用");
    }

    @Override
    public void onDisable() {
        getLogger().info("BuildRate 已卸载");
    }

    public static BuildRate i() { return instance; }

    public Db db() { return db; }
    public GuiManager gui() { return gui; }

    public boolean isAdmin(CommandSender s) { return s.hasPermission("buildrate.admin"); }

    public int minScore() { return getConfig().getInt("scoring.min-score", 1); }
    public int maxScore() { return getConfig().getInt("scoring.max-score", 10); }
    public boolean dropHiLo() { return getConfig().getBoolean("scoring.drop-highest-lowest", true); }
    public boolean allowSelfVote() { return getConfig().getBoolean("scoring.allow-self-vote", false); }
    public boolean onePerPlayer() { return getConfig().getBoolean("scoring.one-build-per-player", true); }
}
