package com.nangua.bedrockskinrecorder;

import com.google.common.cache.Cache;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.geyser.api.skin.Skin;
import org.geysermc.geyser.skin.SkinProvider;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;

/**
 * 皮肤抓取器：基岩玩家上线后，从 Geyser 的基岩皮肤缓存
 * （SkinProvider.CACHED_BEDROCK_SKINS）取出其 Xbox 皮肤纹理数据
 * （PNG byte[]，Geyser 已在玩家加入时从基岩客户端拉取），直接存盘，无需联网。
 *
 * 兜底：若 Geyser 缓存多次未命中（基岩客户端没下发/缓存丢了），
 * 则改从玩家当前 GameProfile 的 textures 属性读取皮肤——
 * SkinsRestorer 会把它选择的皮肤（/skin 设置的皮肤）注入这里，
 * 解码出纹理 URL 后下载 PNG 存盘，实现"参考 SkinsRestorer"。
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

    /**
     * 是否为基岩玩家。三重判定全部通过才认定为基岩：
     * 1) UUID 是 Floodgate 格式（isFloodgatePlayer，静态特征）；
     * 2) 此刻确实挂在 Geyser 会话上（getPlayer 返回非 null，实时连接）；
     * 3) 客户端品牌是 Geyser（品牌双保险：Geyser 基岩客户端品牌为 "Geyser"，
     *    Java 客户端为 vanilla/fabric/forge/lunar 等——品牌明显是 Java 就直接排除，
     *    防止 Floodgate 在本服误把 Java 连接注册成基岩会话）。
     */
    public boolean isBedrock(Player player) {
        UUID uuid = player.getUniqueId();
        FloodgateApi api = FloodgateApi.getInstance();
        if (!api.isFloodgatePlayer(uuid)) {
            return false;
        }
        if (api.getPlayer(uuid) == null) {
            return false;
        }
        String brand = clientBrand(player);
        if (brand != null && !brand.isEmpty()) {
            String b = brand.toLowerCase();
            if (!b.contains("geyser") && !b.contains("floodgate")) {
                plugin.getLogger().info("跳过基岩皮肤记录：客户端品牌为 Java（" + brand + "），玩家 " + player.getName());
                return false;
            }
        }
        return true;
    }

    /** 反射取客户端品牌（Paper 的 getClientBrandName()，spigot-api 编译期没有） */
    private String clientBrand(Player player) {
        try {
            Method m = player.getClass().getMethod("getClientBrandName");
            Object v = m.invoke(player);
            return v == null ? null : v.toString();
        } catch (Throwable t) {
            return null;
        }
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
            // Geyser 最终仍未命中 → 兜底参考 SkinsRestorer
            if (trySkinsRestorerFallback(player)) {
                return;
            }
            plugin.getLogger().warning("基岩皮肤记录未完成（已重试 " + MAX_ATTEMPTS
                    + " 次）: " + player.getName() + " | " + reason
                    + "（SkinsRestorer 兜底也未取到皮肤）");
            return;
        }
        plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin,
                () -> capture(player, attempt + 1), RETRY_DELAY_TICKS);
    }

    /**
     * 兜底：反射读取玩家当前 GameProfile 的 textures 属性（SkinsRestorer 会把皮肤写进这里），
     * 解码出纹理 URL，下载 PNG 存盘。在异步线程执行，不阻塞主线程。
     *
     * @return true=成功取到并记录
     */
    private boolean trySkinsRestorerFallback(Player player) {
        try {
            Object profile = call(player, "getPlayerProfile");
            if (profile == null) return false;
            Set<?> props = (Set<?>) call(profile, "getProperties");
            if (props == null || props.isEmpty()) return false;
            for (Object prop : props) {
                String name = str(call(prop, "getName"));
                if (!"textures".equals(name)) continue;
                String value = str(call(prop, "getValue"));
                if (value == null || value.isEmpty()) return false;
                String json = new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
                JsonObject root = JsonParser.parseString(json).getAsJsonObject();
                JsonObject textures = root.getAsJsonObject("textures");
                if (textures == null || !textures.has("SKIN")) return false;
                String url = textures.getAsJsonObject("SKIN").get("url").getAsString();
                if (url == null || url.isEmpty()) return false;
                byte[] png = download(url);
                if (png == null || png.length < 100) return false;
                store.save(player, url, png, value);
                plugin.getLogger().info("已记录基岩皮肤(SkinsRestorer兜底): " + player.getName()
                        + " (" + png.length + "B) <- " + url);
                return true;
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("SkinsRestorer 皮肤兜底失败: " + player.getName() + " | " + t.getMessage());
        }
        return false;
    }

    /** 下载皮肤 PNG（异步线程内调用） */
    private byte[] download(String urlStr) {
        try (InputStream in = new URL(urlStr).openStream();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
            return out.toByteArray();
        } catch (Throwable t) {
            return null;
        }
    }

    private static Object call(Object target, String method) throws Exception {
        Method m = target.getClass().getMethod(method);
        m.setAccessible(true);
        return m.invoke(target);
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }
}
