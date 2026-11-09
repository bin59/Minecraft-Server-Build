package com.nangua.pumpkinfly;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 本地 YAML 数据存储。三个文件各司其职，避免状态文件被历史记录撑大：
 *   data/players.yml   余额、永久标记、累计时长（唯一真源）
 *   data/purchases.yml 购买 / 管理员发放记录
 *   data/sessions.yml  每次开启飞行的时间、结束时间与结束原因
 */
public final class DataStore {

    private final PumpkinFlyPlugin plugin;
    private final File playersFile;
    private final File purchasesFile;
    private final File sessionsFile;

    private YamlConfiguration playersYml = new YamlConfiguration();
    private YamlConfiguration purchasesYml = new YamlConfiguration();
    private YamlConfiguration sessionsYml = new YamlConfiguration();

    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();
    private final List<Map<String, Object>> purchases = new ArrayList<>();
    private final List<Map<String, Object>> sessions = new ArrayList<>();
    private volatile boolean dirty = false;

    public DataStore(PumpkinFlyPlugin plugin) {
        this.plugin = plugin;
        File dir = new File(plugin.getDataFolder(), "data");
        if (!dir.exists() && !dir.mkdirs()) {
            plugin.getLogger().warning("无法创建数据目录 " + dir);
        }
        this.playersFile = new File(dir, "players.yml");
        this.purchasesFile = new File(dir, "purchases.yml");
        this.sessionsFile = new File(dir, "sessions.yml");
    }

    public void load() {
        playersYml = loadFile(playersFile);
        purchasesYml = loadFile(purchasesFile);
        sessionsYml = loadFile(sessionsFile);

        cache.clear();
        ConfigurationSection root = playersYml.getConfigurationSection("players");
        if (root != null) {
            for (String key : root.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    PlayerData d = new PlayerData();
                    d.load(root.getConfigurationSection(key));
                    cache.put(uuid, d);
                } catch (IllegalArgumentException ignored) { }
            }
        }
        purchases.clear();
        purchases.addAll(toMaps(purchasesYml.getMapList("records")));
        sessions.clear();
        sessions.addAll(toMaps(sessionsYml.getMapList("records")));
        plugin.getLogger().info("已载入 " + cache.size() + " 个飞行账户、"
                + purchases.size() + " 条购买记录、" + sessions.size() + " 条飞行会话。");
    }

    private static List<Map<String, Object>> toMaps(List<Map<?, ?>> src) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (src == null) return out;
        for (Map<?, ?> m : src) {
            Map<String, Object> one = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : m.entrySet()) one.put(String.valueOf(e.getKey()), e.getValue());
            out.add(one);
        }
        return out;
    }

    private YamlConfiguration loadFile(File f) {
        if (!f.exists()) return new YamlConfiguration();
        try {
            return YamlConfiguration.loadConfiguration(f);
        } catch (Exception e) {
            plugin.getLogger().severe("读取 " + f.getName() + " 失败：" + e.getMessage());
            return new YamlConfiguration();
        }
    }

    public PlayerData get(UUID uuid, String name) {
        PlayerData d = cache.get(uuid);
        if (d == null) {
            d = new PlayerData();
            d.name = name == null ? "" : name;
            cache.put(uuid, d);
            dirty = true;
        }
        return d;
    }

    public PlayerData peek(UUID uuid) {
        return cache.get(uuid);
    }

    public Map<UUID, PlayerData> all() {
        return cache;
    }

    public void markDirty() {
        dirty = true;
    }

    public void addPurchase(Map<String, Object> record) {
        if (!plugin.config().recordPurchases) return;
        purchases.add(record);
        trim(purchases);
        dirty = true;
    }

    public void addSession(Map<String, Object> record) {
        if (!plugin.config().recordSessions) return;
        sessions.add(record);
        trim(sessions);
        dirty = true;
    }

    private void trim(List<Map<String, Object>> list) {
        int max = plugin.config().maxRecords;
        if (list.size() > max) {
            list.subList(0, list.size() - max).clear();
        }
    }

    public List<Map<String, Object>> purchasesOf(UUID uuid, int limit) {
        return filter(purchases, uuid, limit);
    }

    public List<Map<String, Object>> sessionsOf(UUID uuid, int limit) {
        return filter(sessions, uuid, limit);
    }

    private static List<Map<String, Object>> filter(List<Map<String, Object>> src, UUID uuid, int limit) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (int i = src.size() - 1; i >= 0 && out.size() < limit; i--) {
            Map<String, Object> m = src.get(i);
            if (uuid == null || uuid.toString().equals(String.valueOf(m.get("uuid")))) out.add(m);
        }
        Collections.reverse(out);
        return out;
    }

    /** 立即落盘；由定时任务、购买、退出、关服调用。必须在主线程或已同步的场合使用。 */
    public synchronized void save() {
        if (!dirty) return;
        try {
            playersYml.set("version", 1);
            playersYml.set("players", null);
            ConfigurationSection root = playersYml.createSection("players");
            for (Map.Entry<UUID, PlayerData> e : cache.entrySet()) {
                e.getValue().save(root.createSection(e.getKey().toString()));
            }
            purchasesYml.set("version", 1);
            purchasesYml.set("records", purchases);
            sessionsYml.set("version", 1);
            sessionsYml.set("records", sessions);

            saveFile(playersYml, playersFile);
            saveFile(purchasesYml, purchasesFile);
            saveFile(sessionsYml, sessionsFile);
            dirty = false;
        } catch (Exception e) {
            plugin.getLogger().severe("保存飞行数据失败：" + e.getMessage());
        }
    }

    private void saveFile(YamlConfiguration yml, File f) throws IOException {
        yml.options().header("PumpkinFly 本地数据，勿在运行中手改；由插件定时写入。");
        yml.options().indent(2);
        yml.save(f);
    }
}
