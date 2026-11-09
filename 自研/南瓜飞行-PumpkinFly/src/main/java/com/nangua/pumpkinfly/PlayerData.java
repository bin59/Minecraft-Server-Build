package com.nangua.pumpkinfly;

import org.bukkit.configuration.ConfigurationSection;

import java.text.SimpleDateFormat;
import java.util.Date;

/** 单个玩家的飞行账户状态（存 data/players.yml）。 */
public final class PlayerData {

    private static final SimpleDateFormat FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public String name = "";
    public boolean permanent = false;
    /** 剩余可飞行秒数；永久飞行时该值不消耗。 */
    public long remainingSeconds = 0L;
    public long totalPurchasedSeconds = 0L;
    public long totalUsedSeconds = 0L;
    /** REAL_TIME 模式使用：到期时间戳（毫秒），0 表示未启用。 */
    public long expireAt = 0L;
    /** 玩家手动 /pfly off，主循环不再自动开飞。 */
    public boolean manualOff = false;
    public String firstPurchase = "";
    public String lastActive = "";

    public boolean hasPass() {
        return permanent || remainingSeconds > 0L;
    }

    public boolean usesTime() {
        return !permanent;
    }

    public void load(ConfigurationSection s) {
        if (s == null) return;
        name = s.getString("name", name);
        permanent = s.getBoolean("permanent", false);
        remainingSeconds = s.getLong("remaining-seconds", 0L);
        totalPurchasedSeconds = s.getLong("total-purchased-seconds", 0L);
        totalUsedSeconds = s.getLong("total-used-seconds", 0L);
        expireAt = s.getLong("expire-at", 0L);
        manualOff = s.getBoolean("manual-off", false);
        firstPurchase = s.getString("first-purchase", "");
        lastActive = s.getString("last-active", "");
    }

    public void save(ConfigurationSection s) {
        s.set("name", name);
        s.set("permanent", permanent);
        s.set("remaining-seconds", remainingSeconds);
        s.set("total-purchased-seconds", totalPurchasedSeconds);
        s.set("total-used-seconds", totalUsedSeconds);
        s.set("expire-at", expireAt);
        s.set("manual-off", manualOff);
        s.set("first-purchase", firstPurchase);
        s.set("last-active", lastActive);
    }

    public static String now() {
        return FMT.format(new Date());
    }
}
