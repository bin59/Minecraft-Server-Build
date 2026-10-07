package com.nangua.moneyledger.ledger;

import net.ess3.api.events.UserBalanceUpdateEvent;

/**
 * 把"余额变动事件 + 调用栈"归类成精准的类别 + 来源描述。
 * source 字段格式：插件友好名 / 类名.方法名（行号），若有活动备注则拼在末尾。
 */
public final class SourceDetector {

    /** 这些包名不参与来源判定（框架/桥/忽略） */
    private static final String[] IGNORE = {
            "com.nangua.moneyledger",
            "org.bukkit",
            "net.ess3",
            "com.earth2me.essentials",
            "net.milkbowl.vault",
            "java.", "javax.", "sun.", "jdk.",
            "io.papermc",
            "net.minecraft",
            "com.google", "org.yaml", "org.sqlite",
            "io.netty", "it.unimi", "org.slf4j", "org.apache", "org.jetbrains", "kotlin."
    };

    /** 包名前缀 → 友好插件名（用于 source 展示）。 */
    private static String friendlyPlugin(String pkg) {
        if (pkg == null || pkg.isEmpty()) return "未知";
        if (pkg.startsWith("com.economyshop"))              return "EconomyShop";
        if (pkg.startsWith("com.auctionhouse"))             return "AuctionHouse";
        if (pkg.startsWith("com.skyblockexp.eztax"))        return "EzTax";
        if (pkg.startsWith("com.leonardobishop.quests"))    return "Quests";
        if (pkg.startsWith("com.hh.dailysell"))            return "DailySell";
        if (pkg.startsWith("com.nangua.buildrate"))        return "BuildRate";
        if (pkg.startsWith("com.nangua.pumpkinmail"))       return "PumpkinMail";
        if (pkg.startsWith("com.nangua.quickmenu"))         return "QuickMenu";
        if (pkg.startsWith("com.nangua.moneyledger"))       return "MoneyLedger";
        if (pkg.startsWith("net.ess3") || pkg.startsWith("com.earth2me.essentials")) return "EssentialsX";
        // 兜底：取包名最后一段
        int dot = pkg.lastIndexOf('.');
        return dot >= 0 ? pkg.substring(dot + 1) : pkg;
    }

    /** 根据事件 Cause 先定一层类别；API 类再按调用栈来源细分。 */
    public static Category categorize(UserBalanceUpdateEvent.Cause cause) {
        if (cause == null) return Category.UNKNOWN;
        switch (cause) {
            case COMMAND_PAY:   return Category.TRANSFER;
            case COMMAND_ECO:   return Category.ACTIVITY;
            case COMMAND_SELL:  return Category.SELL;
            case SPECIAL:       return Category.SPECIAL;
            case API:           return fromSource(detectPackage());
            default:            return Category.UNKNOWN;
        }
    }

    /** 把来源包名映射成类别。 */
    public static Category fromSource(String pkg) {
        if (pkg == null || pkg.isEmpty()) return Category.OTHER;
        if (pkg.startsWith("com.economyshop"))              return Category.SHOP;
        if (pkg.startsWith("com.auctionhouse"))            return Category.AUCTION;
        if (pkg.startsWith("com.skyblockexp.eztax"))        return Category.TAX;
        if (pkg.startsWith("com.leonardobishop.quests"))   return Category.QUEST;
        if (pkg.startsWith("com.hh.dailysell"))            return Category.BUYOUT;
        if (pkg.startsWith("com.nangua.buildrate"))        return Category.ACTIVITY_VOTE;
        if (pkg.startsWith("com.nangua.pumpkinmail"))       return Category.ACTIVITY;
        return Category.OTHER;
    }

    /** 从调用栈找出"谁触发了这笔钱变动"，返回其**包名**。 */
    public static String detectPackage() {
        StackTraceElement[] st = Thread.currentThread().getStackTrace();
        for (StackTraceElement el : st) {
            String cn = el.getClassName();
            if (cn == null || cn.startsWith("com.nangua.moneyledger")) continue;
            boolean ignored = false;
            for (String ig : IGNORE) {
                if (cn.startsWith(ig)) { ignored = true; break; }
            }
            if (!ignored) {
                int last = cn.lastIndexOf('.');
                if (last <= 0) return cn;
                return cn.substring(0, last);
            }
        }
        return "";
    }

    /**
     * 生成精准的来源描述：友好插件名 / 类名.方法名(行号)。
     * 若传入 reason（活动备注），拼在末尾。
     */
    public static String describe(String reason) {
        StackTraceElement[] st = Thread.currentThread().getStackTrace();
        String pkg = "";
        String frame = "";
        for (StackTraceElement el : st) {
            String cn = el.getClassName();
            if (cn == null || cn.startsWith("com.nangua.moneyledger")) continue;
            boolean ignored = false;
            for (String ig : IGNORE) {
                if (cn.startsWith(ig)) { ignored = true; break; }
            }
            if (!ignored) {
                int last = cn.lastIndexOf('.');
                pkg = last > 0 ? cn.substring(0, last) : cn;
                String simple = last > 0 ? cn.substring(last + 1) : cn;
                frame = simple + "." + el.getMethodName() + (el.getLineNumber() > 0 ? "(L" + el.getLineNumber() + ")" : "");
                break;
            }
        }
        String plugin = friendlyPlugin(pkg);
        String desc = frame.isEmpty() ? plugin : plugin + " / " + frame;
        if (reason != null && !reason.isEmpty()) desc += " | " + reason;
        return desc;
    }
}
