package com.nangua.pumpkinmail;

import com.nangua.pumpkinmail.cmd.MailboxCommand;
import com.nangua.pumpkinmail.gui.GuiManager;
import com.nangua.pumpkinmail.storage.Storage;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Level;

/**
 * 南瓜邮箱 PumpkinMail
 * 一次性搭建、长期复用：管理员一键批量发放活动奖励，玩家在 /mailbox 领取。
 */
public final class PumpkinMail extends JavaPlugin {

    private static PumpkinMail instance;
    private Storage storage;
    private GuiManager guiManager;

    public static PumpkinMail get() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        try {
            storage = new Storage(new File(getDataFolder(), "mailbox.db"));
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "无法初始化数据库，插件停用。", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        guiManager = new GuiManager(this);

        getServer().getPluginManager().registerEvents(guiManager, this);
        getServer().getPluginManager().registerEvents(new ChatInput(this), this);

        var cmd = getCommand("mailbox");
        if (cmd != null) {
            cmd.setExecutor(new MailboxCommand(this));
            cmd.setTabCompleter(new MailboxCommand(this));
        }

        new HintTask(this).start();

        getLogger().info("南瓜邮箱已启用。管理：/mailbox admin 或 /mailbox give <奖品ID> <名单>");
    }

    @Override
    public void onDisable() {
        if (guiManager != null) guiManager.closeAll();
        if (storage != null) {
            try { storage.close(); } catch (Exception ignored) { }
        }
        instance = null;
    }

    public Storage storage() { return storage; }
    public GuiManager guiManager() { return guiManager; }
}
