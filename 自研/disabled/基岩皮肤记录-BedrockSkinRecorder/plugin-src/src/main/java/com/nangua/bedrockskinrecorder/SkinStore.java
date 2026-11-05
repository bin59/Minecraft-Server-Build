package com.nangua.bedrockskinrecorder;

import com.google.gson.JsonObject;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 皮肤存储：PNG + 同名 meta JSON（玩家名、UUID、抓取时间、纹理 URL）。
 * 目录：plugins/BedrockSkinRecorder/skins/
 */
public class SkinStore {

    private static final Pattern SAFE = Pattern.compile("[^a-zA-Z0-9._-]");

    private final BedrockSkinRecorder plugin;
    private final File skinsDir;

    public SkinStore(BedrockSkinRecorder plugin) {
        this.plugin = plugin;
        this.skinsDir = new File(plugin.getDataFolder(), "skins");
        if (!skinsDir.exists() && !skinsDir.mkdirs()) {
            plugin.getLogger().warning("无法创建皮肤目录: " + skinsDir.getPath());
        }
    }

    public File getSkinsDir() {
        return skinsDir;
    }

    /** 保存皮肤：覆盖同名 PNG 并写 meta */
    public void save(Player player, String textureUrl, byte[] png, String value) {
        String base = SAFE.matcher(player.getName()).replaceAll("_");
        if (base.isEmpty()) {
            base = player.getUniqueId().toString().substring(0, 8);
        }
        File pngFile = new File(skinsDir, base + ".png");
        File metaFile = new File(skinsDir, base + ".json");
        try {
            Files.write(pngFile.toPath(), png);
            JsonObject meta = new JsonObject();
            meta.addProperty("name", player.getName());
            meta.addProperty("uuid", player.getUniqueId().toString());
            meta.addProperty("capturedAt", System.currentTimeMillis());
            meta.addProperty("textureUrl", textureUrl);
            meta.addProperty("size", png.length);
            if (value != null && value.length() > 32) {
                meta.addProperty("valuePreview", value.substring(0, 32) + "...");
            }
            Files.write(metaFile.toPath(), meta.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            plugin.getLogger().warning("保存皮肤失败: " + player.getName() + " | " + e.getMessage());
        }
    }

    /** 已记录人数（按 PNG 计数） */
    public int countRecorded() {
        if (!skinsDir.exists()) {
            return 0;
        }
        File[] files = skinsDir.listFiles((d, n) -> n.endsWith(".png"));
        return files == null ? 0 : files.length;
    }

    /** 最近记录列表（玩家名|日期，按 meta 修改时间倒序） */
    public List<String> listRecorded() {
        List<String> result = new ArrayList<>();
        if (!skinsDir.exists()) {
            return result;
        }
        File[] metas = skinsDir.listFiles((d, n) -> n.endsWith(".json"));
        if (metas == null) {
            return result;
        }
        java.util.Arrays.sort(metas, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
        for (File m : metas) {
            String name = m.getName().substring(0, m.getName().length() - 5);
            result.add(name + "|" + fmt.format(m.lastModified()));
        }
        return result;
    }

    /** 重载（无配置，仅核对目录） */
    public void reload() {
        if (!skinsDir.exists() && !skinsDir.mkdirs()) {
            plugin.getLogger().warning("无法创建皮肤目录: " + skinsDir.getPath());
        }
    }
}
