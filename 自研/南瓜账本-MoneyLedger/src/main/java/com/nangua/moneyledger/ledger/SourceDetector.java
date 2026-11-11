package com.nangua.moneyledger.ledger;

import net.ess3.api.events.UserBalanceUpdateEvent;

/**
 * 把「余额变动事件 + 调用栈」归类成精准的类别 + 来源描述。
 *
 * 1.1.0 修正（诊断依据：本地 ledger.db 18240 条实测）：
 *  - Quests 5.3.2 包名已从 com.leonardobishop.quests 改为 me.pikamug.quests，
 *    旧映射完全失效，任务奖励被记成 OTHER。
 *  - 旧实现取「第一个非忽略帧」，命令发放时抓到的是 Brigadier 的
 *    context / ContextChain.runExecutable，而不是真实发奖方；Vault / Residence 桥同理。
 *    现改为「扫描整个调用栈，优先命中已知插件规则，桥与命令框架一律跳过」。
 *  - 补上 DailySell / PumpkinFly / OfflineReward / QuickMenu / Residence 的真实包名。
 */
public final class SourceDetector {

    /** 框架、桥、命令调度器：一律跳过，不能当作来源。 */
    private static final String[] IGNORE = {
            "com.nangua.moneyledger",
            "org.bukkit", "io.papermc", "io.netty",
            "net.ess3", "com.earth2me.essentials", "net.essentialsx",
            "net.milkbowl.vault", "vault", "vaultinterface",
            "com.mojang.brigadier", "net.minecraft", "com.destroystokyo.paper",
            "java.", "javax.", "sun.", "jdk.", "kotlin.",
            "com.google", "org.yaml", "org.sqlite", "it.unimi", "org.slf4j",
            "org.apache", "org.jetbrains", "net.kyori", "org.joml"
    };

    /** 包名前缀 → {友好插件名, 类别}。顺序即优先级。 */
    private static final String[][] RULES = {
            {"com.skyblockexp.eztax",          "EzTax",          "TAX"},
            {"me.pikamug.quests",              "Quests",         "QUEST"},
            {"com.leonardobishop.quests",      "Quests",         "QUEST"},
            {"com.hh.dailysell",               "DailySell",      "BUYOUT"},
            {"com.nangua.pumpkinfly",          "PumpkinFly",     "FLIGHT"},
            {"com.nangua.pumpkinmail",         "PumpkinMail",    "ACTIVITY"},
            {"com.nangua.offlinereward",       "OfflineReward",  "ACTIVITY"},
            {"com.nangua.buildrate",           "BuildRate",      "ACTIVITY_VOTE"},
            {"com.nangua.quickmenu",           "QuickMenu",      "ACTIVITY"},
            {"com.bekvon.bukkit.residence",    "Residence",      "RESIDENCE"},
            {"com.residence",                  "Residence",      "RESIDENCE"},
            {"com.economyshop",                "EconomyShop",    "SHOP"},
            {"com.auctionhouse",               "AuctionHouse",   "AUCTION"},
            {"net.ess3",                       "EssentialsX",    "OTHER"},
            {"com.earth2me.essentials",        "EssentialsX",    "OTHER"}
    };

    /** 一次调用栈判定结果。 */
    public record Detection(String pkg, String plugin, String frame, Category category) {
        public boolean known() { return plugin != null && !plugin.isEmpty() && !"未知".equals(plugin); }
    }

    private static boolean ignored(String cn) {
        for (String ig : IGNORE) if (cn.startsWith(ig)) return true;
        return false;
    }

    private static Detection frameOf(StackTraceElement el, String[] rule) {
        String cn = el.getClassName();
        int last = cn.lastIndexOf('.');
        String pkg = last > 0 ? cn.substring(0, last) : cn;
        String simple = last > 0 ? cn.substring(last + 1) : cn;
        String frame = simple + "." + el.getMethodName()
                + (el.getLineNumber() > 0 ? "(L" + el.getLineNumber() + ")" : "");
        if (rule != null) {
            return new Detection(pkg, rule[1], frame, Category.fromName(rule[2]));
        }
        return new Detection(pkg, friendlyPlugin(pkg), frame, Category.OTHER);
    }

    /** 包名 → 友好插件名（规则表优先，兜底取末段）。 */
    public static String friendlyPlugin(String pkg) {
        if (pkg == null || pkg.isEmpty()) return "未知";
        for (String[] r : RULES) if (pkg.startsWith(r[0])) return r[1];
        int dot = pkg.lastIndexOf('.');
        return dot >= 0 ? pkg.substring(dot + 1) : pkg;
    }

    /** 扫描整个调用栈：优先返回命中的已知插件，否则返回第一个非框架帧。 */
    public static Detection detect() {
        StackTraceElement[] st = Thread.currentThread().getStackTrace();
        Detection fallback = null;
        for (StackTraceElement el : st) {
            String cn = el.getClassName();
            if (cn == null || ignored(cn)) continue;
            for (String[] r : RULES) {
                if (cn.startsWith(r[0])) return frameOf(el, r);
            }
            if (fallback == null) fallback = frameOf(el, null);
        }
        return fallback != null ? fallback
                : new Detection("", "未知", "", Category.OTHER);
    }

    /** 根据事件 Cause 定类别；API 事件再按真实调用栈细分。 */
    public static Category categorize(UserBalanceUpdateEvent.Cause cause) {
        return categorize(cause, detect());
    }

    /** 复用同一次调用栈判定，避免一笔事件重复扫栈。 */
    public static Category categorize(UserBalanceUpdateEvent.Cause cause, Detection d) {
        if (cause == null) return Category.UNKNOWN;
        return switch (cause) {
            case COMMAND_PAY  -> Category.TRANSFER;
            case COMMAND_ECO  -> Category.ACTIVITY;
            case COMMAND_SELL -> Category.SELL;
            case SPECIAL      -> Category.SPECIAL;
            case API          -> d.category();
            default           -> Category.UNKNOWN;
        };
    }

    /** 兼容旧签名：包名 → 类别。 */
    public static Category fromSource(String pkg) {
        if (pkg == null || pkg.isEmpty()) return Category.OTHER;
        for (String[] r : RULES) if (pkg.startsWith(r[0])) return Category.fromName(r[2]);
        return Category.OTHER;
    }

    /** 兼容旧签名：返回真实调用方包名（不再返回桥/命令框架）。 */
    public static String detectPackage() {
        return detect().pkg();
    }

    /** 生成来源描述：插件名 / 类.方法(行号) | 备注 | 命令上下文。 */
    public static String describe(String reason) {
        return describe(detect(), reason, null);
    }

    /** 兼容旧签名。 */
    public static String describe(String reason, String commandCtx) {
        return describe(detect(), reason, commandCtx);
    }

    /** 复用判定结果的 describe。 */
    public static String describe(Detection d, String reason, String commandCtx) {
        String plugin = d.known() ? d.plugin() : "EssentialsX/外部调用";
        String desc = d.frame().isEmpty() ? plugin : plugin + " / " + d.frame();
        if (commandCtx != null && !commandCtx.isEmpty()) desc += " | 命令: " + commandCtx;
        if (reason != null && !reason.isEmpty()) desc += " | " + reason;
        return desc;
    }
}
