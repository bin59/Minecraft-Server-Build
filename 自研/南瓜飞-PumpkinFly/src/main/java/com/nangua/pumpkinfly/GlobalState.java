package com.nangua.pumpkinfly;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

/**
 * 全局飞行管理状态：总开关、全服免费飞行、全服限时体验。
 *
 * <p>由 {@code /pfly global} 修改，单独存 {@code data/global.yml}，重启不丢。
 * config.yml 里的 {@code global.enabled} / {@code global.free-fly} 只在
 * global.yml 尚不存在时作为初值，避免每次 /pfly reload 覆盖管理员的实时操作。
 */
public final class GlobalState {

    private final PumpkinFlyPlugin plugin;
    private final File file;

    /** false = 全服强制禁飞（所有人安全落地），优先级最高。 */
    public boolean enabled = true;
    /** true = 所有人无需凭证即可飞，仍受 zones 区域规则限制。 */
    public boolean freeFly = false;
    /** 全服限时体验到期时间戳（毫秒），0 = 未开启；离线玩家上线后同样生效。 */
    public long trialExpireAt = 0L;
    /** 本次全服体验总时长（秒），供 HUD 进度条使用。 */
    public long trialTotalSeconds = 0L;
    public String trialGrantedBy = "";

    public GlobalState(PumpkinFlyPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(new File(plugin.getDataFolder(), "data"), "global.yml");
    }

    public void load() {
        enabled = plugin.config().globalEnabled;
        freeFly = plugin.config().globalFreeFly;
        if (!file.exists()) {
            save();
            return;
        }
        try {
            YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
            enabled = y.getBoolean("enabled", enabled);
            freeFly = y.getBoolean("free-fly", freeFly);
            trialExpireAt = y.getLong("trial-expire-at", 0L);
            trialTotalSeconds = y.getLong("trial-total-seconds", 0L);
            trialGrantedBy = y.getString("trial-granted-by", "");
        } catch (Throwable t) {
            plugin.getLogger().warning("读取 global.yml 失败，使用 config.yml 初值：" + t.getMessage());
        }
        if (checkExpiry(System.currentTimeMillis())) save();
    }

    public void save() {
        try {
            File dir = file.getParentFile();
            if (!dir.exists() && !dir.mkdirs()) {
                plugin.getLogger().warning("无法创建数据目录 " + dir);
                return;
            }
            YamlConfiguration y = new YamlConfiguration();
            y.set("enabled", enabled);
            y.set("free-fly", freeFly);
            y.set("trial-expire-at", trialExpireAt);
            y.set("trial-total-seconds", trialTotalSeconds);
            y.set("trial-granted-by", trialGrantedBy);
            y.options().header("PumpkinFly 全局飞行状态，由 /pfly global 维护；勿在运行中手改。");
            y.options().indent(2);
            y.save(file);
        } catch (Throwable t) {
            plugin.getLogger().severe("保存 global.yml 失败：" + t.getMessage());
        }
    }

    public boolean trialActive(long now) {
        return trialExpireAt > now;
    }

    public long trialRemainingSeconds(long now) {
        return trialActive(now) ? Math.max(0L, (trialExpireAt - now + 999L) / 1000L) : 0L;
    }

    /** 主循环每秒调用；到期后清零并写盘，返回本次是否刚到期。 */
    public boolean checkExpiry(long now) {
        if (trialExpireAt > 0L && now >= trialExpireAt) {
            trialExpireAt = 0L;
            trialTotalSeconds = 0L;
            trialGrantedBy = "";
            plugin.getLogger().info("全服限时体验已到期，恢复按个人凭证判定。");
            return true;
        }
        return false;
    }
}
