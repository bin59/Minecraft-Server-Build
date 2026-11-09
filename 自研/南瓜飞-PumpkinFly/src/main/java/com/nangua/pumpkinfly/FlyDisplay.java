package com.nangua.pumpkinfly;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * 飞行界面显示：飞行中在屏幕上实时显示「飞行状态 + 剩余时长 + 本次/累计已飞」，
 * 不需要再敲 /pfly status。
 *
 * <p>支持 ACTION_BAR（物品栏上方快捷栏提示）、BOSS_BAR（顶部血条）、BOTH、OFF；
 * 每个玩家一条独立 BossBar，离开允许区域 / 关闭飞行 / 退出时自动移除。
 *
 * <p>剩余时长按凭证优先级显示：永久 → 全服免费 → 全服体验 → 个人限时体验 → 时长余额。
 */
public final class FlyDisplay {

    private final PumpkinFlyPlugin plugin;
    private final Map<UUID, BossBar> bars = new HashMap<>();
    private final Map<UUID, Long> lastSent = new HashMap<>();

    public FlyDisplay(PumpkinFlyPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 由 FlyManager.tick() 每个在线玩家每帧调用。
     *
     * @param enabled        有凭证且区域允许（含「已开飞但还没起飞」）
     * @param flying         是否真正在飞行（isFlying）
     * @param zone           当前区域标签
     * @param sessionSeconds 本次飞行会话已累计秒数
     */
    public void update(Player p, PlayerData d, boolean enabled, boolean flying,
                       String zone, long sessionSeconds, long now) {
        FlyConfig c = plugin.config();
        boolean want = !"OFF".equals(c.displayMode)
                && d.hudEnabled
                && enabled
                && (!"FLYING".equals(c.displayShowWhen) || flying);
        if (!want) {
            clear(p);
            return;
        }

        boolean bedrock = isBedrock(p);
        UUID uuid = p.getUniqueId();
        Long last = lastSent.get(uuid);
        // 留 100ms 容差，避免 tick 抖动导致隔帧刷新（ActionBar 会淡出）
        if (last != null && now - last < c.displayUpdateSeconds * 1000L - 100L) return;
        lastSent.put(uuid, now);

        GlobalState g = plugin.global();
        long rem = remaining(d, now);
        boolean warn = rem >= 0L && rem <= c.displayWarnSeconds;
        Map<String, String> vars = new HashMap<>();
        // 用中文而非 ∞：Minecraft 默认字体不含数学符号，会显示成方框
        vars.put("remaining", remainingText(d, now, rem, warn));
        vars.put("session", TimeUtil.format(sessionSeconds));
        vars.put("total", TimeUtil.format(d.totalUsedSeconds));
        vars.put("state", flying ? "飞行中" : "待起飞");
        vars.put("zone", zone == null ? "" : zone);
        vars.put("source", source(d, now));
        // 基岩端（Geyser）对纯文本进度条 ■ 渲染不稳定，且原生 BossBar 自带图形进度条；
        // 故基岩端不发送文本进度条。Java 端按配置决定是否显示。
        vars.put("bar", (bedrock || !c.displayProgressBar) ? "" : progressBar(d, now, rem));

        // 基岩端 ActionBar 经 Geyser 经常整条不显示，统一改用原生 BossBar（基岩端渲染稳定）：
        // bedrock 下 ACTION_BAR → 升级为 BOSS_BAR；BOTH → 两者都发（BossBar 为主）；BOSS_BAR 不变。
        boolean action = ("ACTION_BAR".equals(c.displayMode) || "BOTH".equals(c.displayMode)) && !bedrock;
        boolean boss = "BOSS_BAR".equals(c.displayMode) || "BOTH".equals(c.displayMode)
                || (bedrock && "ACTION_BAR".equals(c.displayMode));

        if (action) {
            String tpl = flying ? c.displayActionFlying : c.displayActionIdle;
            p.sendActionBar(FlyConfig.color(render(tpl, vars)));
        }
        if (boss) {
            BossBar bar = bars.get(uuid);
            if (bar == null) {
                bar = Bukkit.createBossBar("", parseColor(c.displayBarColor),
                        parseStyle(c.displayBarStyle));
                bar.addPlayer(p);
                bars.put(uuid, bar);
            }
            bar.setTitle(FlyConfig.color(render(c.displayBossTitle, vars)));
            bar.setColor(warn ? BarColor.RED
                    : (rem < 0L ? BarColor.BLUE : parseColor(c.displayBarColor)));
            bar.setProgress(progress(d, now, rem));
            bar.setVisible(true);
        } else {
            removeBar(p);
        }
    }

    /** 是否基岩版玩家：Floodgate 进服的 UUID 以 "." 开头（本项目约定），再用 Geyser 客户端品牌兜底。 */
    private static boolean isBedrock(Player p) {
        if (p == null) return false;
        if (p.getUniqueId().toString().startsWith(".")) return true;
        String brand = p.getClientBrandName();
        return brand != null && brand.toLowerCase(Locale.ROOT).contains("geyser");
    }

    /** 停止该玩家的显示，并清掉客户端上残留的 ActionBar 文本。 */
    public void clear(Player p) {
        if (p == null) return;
        if (lastSent.remove(p.getUniqueId()) != null) {
            try { p.sendActionBar(""); } catch (Throwable ignored) { }
        }
        removeBar(p);
    }

    /** 关服 / 重载时清空全部显示。 */
    public void clearAll() {
        for (Map.Entry<UUID, BossBar> e : bars.entrySet()) {
            Player p = Bukkit.getPlayer(e.getKey());
            if (p != null && p.isOnline()) {
                try { p.sendActionBar(""); } catch (Throwable ignored) { }
            }
            e.getValue().removeAll();
        }
        bars.clear();
        lastSent.clear();
    }

    public void onReload() {
        clearAll();
    }

    private void removeBar(Player p) {
        BossBar bar = bars.remove(p.getUniqueId());
        if (bar != null) {
            bar.removePlayer(p);
            bar.removeAll();
        }
    }

    /** 有效剩余秒数；-1 表示不消耗（永久 / 全服免费）。 */
    private long remaining(PlayerData d, long now) {
        return plugin.fly().effectiveRemainingSeconds(d, now);
    }

    private String source(PlayerData d, long now) {
        GlobalState g = plugin.global();
        if (d.permanent) return "永久";
        if (g.freeFly) return "全服免费";
        if (g.trialActive(now)) return "全服体验";
        if (d.trialActive(now)) return "限时体验";
        return "时长余额";
    }

    private String remainingText(PlayerData d, long now, long rem, boolean warn) {
        if (d.permanent) return "永久";
        if (plugin.global().freeFly) return "全服免费";
        String prefix = plugin.global().trialActive(now) ? "全服体验 "
                : (d.trialActive(now) ? "体验 " : "");
        String text = prefix + TimeUtil.format(Math.max(0L, rem));
        return warn ? "&c&l" + text + "&r" : text;
    }

    /** 进度条分母：体验用本次发放总时长，余额用累计购买（至少 60 秒）。 */
    private long total(PlayerData d, long now) {
        GlobalState g = plugin.global();
        if (g.trialActive(now)) return Math.max(1L, g.trialTotalSeconds);
        if (d.trialActive(now)) return Math.max(1L, d.trialTotalSeconds);
        return Math.max(60L, d.totalPurchasedSeconds);
    }

    /** BossBar 进度：限时按「剩余 / 总量」比例，永久 / 免费飞行恒满。 */
    private double progress(PlayerData d, long now, long rem) {
        if (rem < 0L) return 1.0D;
        double base = (double) total(d, now);
        return Math.max(0D, Math.min(1D, rem / base));
    }

    /** 10 格文本进度条，供 {bar} 占位符使用。 */
    private String progressBar(PlayerData d, long now, long rem) {
        if (!plugin.config().displayProgressBar) return "";
        int filled = (int) Math.round(progress(d, now, rem) * 10D);
        filled = Math.max(0, Math.min(10, filled));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) sb.append(i < filled ? "&a■" : "&8■");
        return sb.append("&r").toString();
    }

    private static String render(String tpl, Map<String, String> vars) {
        String out = tpl == null ? "" : tpl;
        for (Map.Entry<String, String> e : vars.entrySet()) {
            out = out.replace("{" + e.getKey() + "}", e.getValue() == null ? "" : e.getValue());
        }
        return out;
    }

    private static BarColor parseColor(String name) {
        try { return BarColor.valueOf(name); } catch (Exception e) { return BarColor.GREEN; }
    }

    private static BarStyle parseStyle(String name) {
        try { return BarStyle.valueOf(name); } catch (Exception e) { return BarStyle.SOLID; }
    }
}
