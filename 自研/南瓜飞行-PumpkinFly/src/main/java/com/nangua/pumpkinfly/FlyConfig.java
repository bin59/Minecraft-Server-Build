package com.nangua.pumpkinfly;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** 集中读取 config.yml，所有键都有代码内默认值，旧配置文件缺键也能正常启动。 */
public final class FlyConfig {

    private final PumpkinFlyPlugin plugin;

    // 价格
    public double permanentPrice;
    public double perMinute;
    public int maxMinutesPerBuy;
    public int maxBalanceMinutes;
    public boolean allowDuplicatePermanent;

    // 计时
    public String timingMode;
    public int tickSeconds;
    public final List<Integer> warnSeconds = new ArrayList<>();
    public boolean trackPermanentSessions;

    // 区域
    public String zoneMode;
    public final Set<String> mainResidences = new HashSet<>();
    public boolean allowOwn;
    public boolean allowTrusted;
    public boolean allowOther;
    public boolean allowWilderness;
    public final Set<String> wildernessWorlds = new HashSet<>();
    public final Set<String> residenceWhitelist = new HashSet<>();
    public final Set<String> residenceBlacklist = new HashSet<>();
    public final Set<String> worldWhitelist = new HashSet<>();
    public final Set<String> worldBlacklist = new HashSet<>();
    public String whenResidenceMissing;

    // 安全
    public boolean safeLand;
    public int searchDepth;
    public int noFallSeconds;
    public boolean disableOnWorldChange;
    public boolean disableOnGamemodeChange;
    public boolean notifyOnLeaveZone;

    // 存储
    public int saveIntervalSeconds;
    public int maxRecords;
    public boolean recordPurchases;
    public boolean recordSessions;
    public boolean updateNameOnJoin;

    // 协作
    public boolean blockEssentialsFly;
    public boolean warnEssentialsAutoFly;

    // 消息
    public String prefix;

    public FlyConfig(PumpkinFlyPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        plugin.reloadConfig();
        org.bukkit.configuration.file.FileConfiguration c = plugin.getConfig();

        permanentPrice = c.getDouble("prices.permanent", 100000D);
        perMinute = c.getDouble("prices.per-minute", 20D);
        maxMinutesPerBuy = c.getInt("prices.max-minutes-per-buy", 60);
        maxBalanceMinutes = c.getInt("prices.max-balance-minutes", 10080);
        allowDuplicatePermanent = c.getBoolean("prices.allow-duplicate-permanent", false);

        timingMode = c.getString("timing.mode", "FLYING_ONLY").trim().toUpperCase(Locale.ROOT);
        tickSeconds = Math.max(1, c.getInt("timing.tick-seconds", 1));
        warnSeconds.clear();
        for (Object o : c.getList("timing.warn-seconds", java.util.Arrays.asList(60, 30, 10))) {
            try { warnSeconds.add(Integer.parseInt(String.valueOf(o))); } catch (NumberFormatException ignored) { }
        }
        trackPermanentSessions = c.getBoolean("timing.track-permanent-sessions", true);

        zoneMode = c.getString("zones.mode", "OWN_AND_MAIN").trim().toUpperCase(Locale.ROOT);
        fill(mainResidences, c.getStringList("zones.main-residences"),
                c.contains("zones.main-residences") ? new String[0] : new String[]{"zhucheng"});
        allowOwn = c.getBoolean("zones.allow-own-residences", true);
        allowTrusted = c.getBoolean("zones.allow-trusted-residences", false);
        allowOther = c.getBoolean("zones.allow-other-residences", false);
        allowWilderness = c.getBoolean("zones.allow-wilderness", false);
        fill(wildernessWorlds, c.getStringList("zones.wilderness-worlds"));
        fill(residenceWhitelist, c.getStringList("zones.residence-whitelist"));
        fill(residenceBlacklist, c.getStringList("zones.residence-blacklist"));
        fill(worldWhitelist, c.getStringList("zones.world-whitelist"));
        fill(worldBlacklist, c.getStringList("zones.world-blacklist"),
                c.contains("zones.world-blacklist") ? new String[0] : new String[]{"world_nether", "world_the_end"});
        whenResidenceMissing = c.getString("zones.when-residence-missing", "WILDERNESS")
                .trim().toUpperCase(Locale.ROOT);

        safeLand = c.getBoolean("safety.safe-land", true);
        searchDepth = c.getInt("safety.search-depth", 32);
        noFallSeconds = c.getInt("safety.no-fall-damage-seconds", 5);
        disableOnWorldChange = c.getBoolean("safety.disable-on-world-change", true);
        disableOnGamemodeChange = c.getBoolean("safety.disable-on-gamemode-change", true);
        notifyOnLeaveZone = c.getBoolean("safety.notify-on-leave-zone", true);

        saveIntervalSeconds = Math.max(5, c.getInt("storage.save-interval-seconds", 60));
        maxRecords = Math.max(10, c.getInt("storage.max-records-per-file", 5000));
        recordPurchases = c.getBoolean("storage.record-purchases", true);
        recordSessions = c.getBoolean("storage.record-sessions", true);
        updateNameOnJoin = c.getBoolean("storage.update-name-on-join", true);

        blockEssentialsFly = c.getBoolean("integration.block-essentials-fly", true);
        warnEssentialsAutoFly = c.getBoolean("integration.warn-essentials-auto-fly", true);

        prefix = color(c.getString("messages.prefix", "&8[&6南瓜飞行&8] &r"));
    }

    /** 取一条消息并替换 {占位符}，键值对按 key,value,key,value 传入。 */
    public String msg(String key, String... kv) {
        String raw = plugin.getConfig().getString("messages." + key, "&c[缺少消息: " + key + "]");
        String out = color(raw);
        for (int i = 0; i + 1 < kv.length; i += 2) {
            out = out.replace("{" + kv[i] + "}", kv[i + 1] == null ? "" : kv[i + 1]);
        }
        return prefix + out;
    }

    public static String color(String s) {
        return s == null ? "" : ChatColor.translateAlternateColorCodes('&', s);
    }

    private static void fill(Set<String> target, List<String> src, String... defaults) {
        target.clear();
        if (src != null) for (String s : src) if (s != null && !s.trim().isEmpty()) target.add(s.trim().toLowerCase(Locale.ROOT));
        if (target.isEmpty()) for (String d : defaults) target.add(d.toLowerCase(Locale.ROOT));
    }

    public boolean isRealTime() {
        return "REAL_TIME".equals(timingMode);
    }

    public String zoneSummary() {
        switch (zoneMode) {
            case "EVERYWHERE": return "全部区域";
            case "ALL_RESIDENCES": return "所有领地";
            case "WHITELIST": return "指定领地（" + String.join(", ", residenceWhitelist) + "）";
            default:
                StringBuilder sb = new StringBuilder("自己的领地");
                if (!mainResidences.isEmpty()) sb.append(" + 主城（").append(String.join(", ", mainResidences)).append("）");
                if (allowTrusted) sb.append(" + 被信任领地");
                if (allowOther) sb.append(" + 其它领地");
                if (allowWilderness) sb.append(" + 野外");
                return sb.toString();
        }
    }

    public ConfigurationSection raw() {
        return plugin.getConfig();
    }
}
