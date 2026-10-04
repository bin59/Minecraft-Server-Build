package com.nangua.moneyledger.ledger;

import net.ess3.api.events.UserBalanceUpdateEvent;

/**
 * 把"余额变动事件 + 调用栈"归类成用户要的 7 大类别。
 */
public final class SourceDetector {

    /** 这些包名不参与来源判定（框架/桥/忽略） */
    private static final String[] IGNORE = {
            "com.nangua.moneyledger",
            "com.nangua.pumpkinmail", // 活动发奖本身在 fromSource 里单独归 ACTIVITY
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

    /** 根据事件 Cause 先定一层类别；API 类再按调用栈来源细分。 */
    public static Category categorize(UserBalanceUpdateEvent.Cause cause) {
        if (cause == null) return Category.UNKNOWN;
        switch (cause) {
            case COMMAND_PAY:   return Category.TRANSFER;
            case COMMAND_ECO:   return Category.ACTIVITY;
            case COMMAND_SELL:  return Category.SELL;
            case SPECIAL:       return Category.SPECIAL;
            case API:
                Category c = fromSource(detectSource());
                return c;
            default:            return Category.UNKNOWN;
        }
    }

    /** 把来源包名映射成类别（各经济插件的基础包）。 */
    public static Category fromSource(String pkg) {
        if (pkg == null) return Category.OTHER;
        if (pkg.startsWith("com.economyshop"))           return Category.SHOP;
        if (pkg.startsWith("com.auctionhouse"))          return Category.AUCTION;
        if (pkg.startsWith("com.skyblockexp.eztax"))     return Category.TAX;
        if (pkg.startsWith("com.leonardobishop.quests")) return Category.QUEST;
        if (pkg.startsWith("com.hh.dailysell"))          return Category.BUYOUT;
        if (pkg.startsWith("com.nangua.buildrate"))      return Category.ACTIVITY_VOTE;
        if (pkg.startsWith("com.nangua.pumpkinmail"))    return Category.ACTIVITY;
        return Category.OTHER;
    }

    /** 从调用栈找出"谁触发了这笔钱变动"，返回其**包名**（去掉类名），找不到返回空串。 */
    public static String detectSource() {
        StackTraceElement[] st = Thread.currentThread().getStackTrace();
        for (StackTraceElement el : st) {
            String cn = el.getClassName();
            if (cn == null || cn.startsWith("com.nangua.moneyledger")) continue;
            boolean ignored = false;
            for (String ig : IGNORE) {
                if (cn.startsWith(ig)) { ignored = true; break; }
            }
            if (!ignored) {
                // 去掉最后一个"."后的类名，返回完整包名，如 com.economyshop / com.nangua.buildrate
                int last = cn.lastIndexOf('.');
                if (last <= 0) return cn;
                return cn.substring(0, last);
            }
        }
        return "";
    }
}
