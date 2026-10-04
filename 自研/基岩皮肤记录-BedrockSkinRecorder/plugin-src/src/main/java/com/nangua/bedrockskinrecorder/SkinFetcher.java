package com.nangua.bedrockskinrecorder;

import com.google.common.cache.Cache;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.geyser.api.skin.Skin;
import org.geysermc.geyser.skin.SkinProvider;

import java.lang.reflect.Field;
import java.util.UUID;

/**
 * 皮肤抓取器：基岩玩家上线后，从 Geyser 的基岩皮肤缓存
 * （SkinProvider.CACHED_BEDROCK_SKINS）取出其 Xbox 皮肤纹理数据
 * （PNG byte[]，Geyser 已在玩家加入时从基岩客户端拉取），直接存盘，无需联网。
 */
public class SkinFetcher {

    private static final int MAX_ATTEMPTS = 5;
    private static final long RETRY_DELAY_TICKS = 80L; // 4 秒

    private final BedrockSkinRecorder plugin;
    private final SkinStore store;

    public SkinFetcher(BedrockSkinRecorder plugin, SkinStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    /** 是否为 Floodgate 基岩玩家 */
    public boolean isBedrock(UUID uuid) {
        return FloodgateApi.getInstance().isFloodgatePlayer(uuid);
    }

    /** 抓取并保存（attempt 0 起，最多重试 MAX_ATTEMPTS 次） */
    public void capture(Player player, int attempt) {
        if (!player.isOnline()) {
            return;
        }
        Skin skin = lookupCachedSkin(player.getUniqueId());
        if (skin == null) {
            scheduleRetry(player, attempt, "Geyser 皮肤缓存未命中");
            return;
        }
        if (skin.failed()) {
            scheduleRetry(player, attempt, "Geyser 皮肤加载失败");
            return;
        }
        byte[] png = skin.skinData();
        if (png == null || png.length < 100) {
            scheduleRetry(player, attempt, "皮肤数据为空");
            return;
        }
        store.save(player, skin.textureUrl(), png, null);
        plugin.getLogger().info("已记录基岩皮肤: " + player.getName()
                + " (" + png.length + "B) -> " + store.getSkinsDir().getPath());
    }

    /** 从 Geyser SkinProvider 缓存读取基岩玩家皮肤（反射访问 private 静态字段） */
    @SuppressWarnings("unchecked")
    private Skin lookupCachedSkin(UUID uuid) {
        try {
            Field field = SkinProvider.class.getDeclaredField("CACHED_BEDROCK_SKINS");
            field.setAccessible(true);
            Cache<String, Skin> cache = (Cache<String, Skin>) field.get(null);
            String key = SkinProvider.shorthandUUID(uuid);
            return cache.getIfPresent(key);
        } catch (Throwable t) {
            plugin.getLogger().warning("读取 Geyser 皮肤缓存失败: " + t.getMessage());
            return null;
        }
    }

    private void scheduleRetry(Player player, int attempt, String reason) {
        if (attempt >= MAX_ATTEMPTS) {
            plugin.getLogger().warning("基岩皮肤记录未完成（已重试 " + MAX_ATTEMPTS
                    + " 次）: " + player.getName() + " | " + reason);
            return;
        }
        plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin,
                () -> capture(player, attempt + 1), RETRY_DELAY_TICKS);
    }
}
