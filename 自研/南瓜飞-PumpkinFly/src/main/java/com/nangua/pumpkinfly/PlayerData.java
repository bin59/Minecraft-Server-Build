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
    /** 是否在界面显示飞行状态与剩余时长（/pfly hud 切换），默认开。 */
    public boolean hudEnabled = true;

    // ---------- 限时体验奖励 ----------
    /** 个人限时体验到期时间戳（毫秒），按现实时间走，离线也消耗。 */
    public long trialExpireAt = 0L;
    /** 当前体验周期总时长（秒），供 HUD 进度条使用。 */
    public long trialTotalSeconds = 0L;
    public String trialGrantedAt = "";
    public String trialGrantedBy = "";
    /** /pfly claim 领取日期（ONCE 模式只要非空即视为已领；DAILY 模式按日期比对）。 */
    public String claimDate = "";
    /** 是否已领过「首次进服赠送」体验。 */
    public boolean joinTrialGiven = false;
    /** 管理员单独禁用该玩家飞行（不受凭证影响）。 */
    public boolean banned = false;

    public String firstPurchase = "";
    public String lastActive = "";

    public boolean trialActive(long now) {
        return trialExpireAt > now;
    }

    public long trialRemainingSeconds(long now) {
        return trialActive(now) ? Math.max(0L, (trialExpireAt - now + 999L) / 1000L) : 0L;
    }

    /** 个人凭证（不含全局状态）：永久 > 限时体验 > 余额；被封禁一律 false。 */
    public boolean hasPass(long now) {
        if (banned) return false;
        return permanent || trialActive(now) || remainingSeconds > 0L;
    }

    public boolean hasPass() {
        return hasPass(System.currentTimeMillis());
    }

    /** 是否要扣余额：永久、个人体验、全服体验/免费飞行期间都不扣。 */
    public boolean usesTime(long now) {
        return !permanent && !trialActive(now);
    }

    public boolean usesTime() {
        return usesTime(System.currentTimeMillis());
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
        hudEnabled = s.getBoolean("hud-enabled", true);
        trialExpireAt = s.getLong("trial-expire-at", 0L);
        trialTotalSeconds = s.getLong("trial-total-seconds", 0L);
        trialGrantedAt = s.getString("trial-granted-at", "");
        trialGrantedBy = s.getString("trial-granted-by", "");
        claimDate = s.getString("claim-date", "");
        joinTrialGiven = s.getBoolean("join-trial-given", false);
        banned = s.getBoolean("banned", false);
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
        s.set("hud-enabled", hudEnabled);
        s.set("trial-expire-at", trialExpireAt);
        s.set("trial-total-seconds", trialTotalSeconds);
        s.set("trial-granted-at", trialGrantedAt);
        s.set("trial-granted-by", trialGrantedBy);
        s.set("claim-date", claimDate);
        s.set("join-trial-given", joinTrialGiven);
        s.set("banned", banned);
        s.set("first-purchase", firstPurchase);
        s.set("last-active", lastActive);
    }

    public static String now() {
        synchronized (FMT) {
            return FMT.format(new Date());
        }
    }
}
