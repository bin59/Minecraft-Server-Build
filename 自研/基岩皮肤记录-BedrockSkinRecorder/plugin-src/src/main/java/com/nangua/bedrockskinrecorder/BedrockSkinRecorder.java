package com.nangua.bedrockskinrecorder;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;

/**
 * 基岩皮肤记录器主类。
 * 基岩玩家上线时，从 Floodgate 获取其 Xbox 皮肤纹理（Mojang 签名 base64），
 * 解码出 textures.minecraft.net 的 PNG 地址并下载保存到插件目录，
 * 供后续「拉取全服皮肤」等用途使用。
 */
public final class BedrockSkinRecorder extends JavaPlugin {

    private SkinStore store;
    private SkinFetcher fetcher;

    @Override
    public void onEnable() {
        store = new SkinStore(this);
        fetcher = new SkinFetcher(this, store);
        getServer().getPluginManager().registerEvents(new SkinCaptureListener(this, fetcher), this);

        // 启动时补抓：把已记录的 meta 与磁盘核对一次（幂等，仅日志）
        getLogger().info("基岩皮肤记录器已启用 | 皮肤目录: " + store.getSkinsDir().getPath());
        getLogger().info("已记录玩家数: " + store.countRecorded());
    }

    @Override
    public void onDisable() {
        getLogger().info("基岩皮肤记录器已停用");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§e[基岩皮肤记录] §f已记录 §b" + store.countRecorded()
                    + " §f名基岩玩家皮肤，目录: §7" + store.getSkinsDir().getPath());
            sender.sendMessage("§7使用 /bsr list 查看列表，/bsr reload 重载");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "list" -> {
                List<String> names = store.listRecorded();
                if (names.isEmpty()) {
                    sender.sendMessage("§e[基岩皮肤记录] §7尚未记录任何皮肤");
                } else {
                    sender.sendMessage("§e[基岩皮肤记录] §f最近记录（共 §b" + names.size() + " §f人）:");
                    int n = Math.min(30, names.size());
                    for (int i = 0; i < n; i++) {
                        String[] meta = names.get(i).split("\\|");
                        sender.sendMessage("  §7" + (i + 1) + ". §f" + meta[0]
                                + " §7(" + (meta.length > 1 ? meta[1] : "?") + ")");
                    }
                }
                return true;
            }
            case "reload" -> {
                store.reload();
                sender.sendMessage("§e[基岩皮肤记录] §f已重载，当前记录 §b" + store.countRecorded() + " §f人");
                return true;
            }
            default -> sender.sendMessage("§e[基岩皮肤记录] §7用法: /bsr | /bsr list | /bsr reload");
        }
        return true;
    }

    public SkinStore getStore() {
        return store;
    }

    public SkinFetcher getFetcher() {
        return fetcher;
    }

    public File getDataDir() {
        return store.getSkinsDir();
    }
}
