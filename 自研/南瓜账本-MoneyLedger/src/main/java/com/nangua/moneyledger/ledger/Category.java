package com.nangua.moneyledger.ledger;

/**
 * 南瓜币变动类别。
 *
 * 1.1.0：新增 FLIGHT / RESIDENCE / BACKFILL / ADJUST。
 * 枚举顺序只追加不改，数据库中保存的是 name()，历史数据不受影响。
 */
public enum Category {
    TRANSFER("转账"),
    SHOP("买卖"),
    AUCTION("拍卖"),
    BUYOUT("每日收购"),
    TAX("周税"),
    QUEST("任务"),
    ACTIVITY("活动/管理"),
    ACTIVITY_VOTE("活动参与"),
    SELL("出售"),
    SPECIAL("特殊"),
    OTHER("其他"),
    UNKNOWN("未知"),
    FLIGHT("飞行消费"),
    RESIDENCE("领地"),
    BACKFILL("日志回填"),
    ADJUST("余额校正");

    public final String label;

    Category(String label) {
        this.label = label;
    }

    /** 按英文名或中文标签反查，找不到返回 null。 */
    public static Category fromName(String s) {
        if (s == null) return null;
        for (Category c : values()) {
            if (c.name().equalsIgnoreCase(s) || c.label.equals(s)) return c;
        }
        return null;
    }
}
