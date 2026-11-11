package com.nangua.moneyledger.ledger;

import java.math.BigDecimal;

/**
 * 一笔南瓜币变动记录。
 *
 * 1.1.0：新增 oldExact / newExact / deltaExact（BigDecimal 精确文本，避免 double 存出
 * 0.18000000000000682 这类脏小数）与 origin（EVENT=事件实时记录 / BACKFILL=trade.log 回填 /
 * FAIL_REPLAY=失败重放 / ADJUST=余额校正）。9 参数旧构造器保留，供二进制兼容。
 */
public record LedgerEntry(
        String uuid,
        String name,
        long time,
        double oldBalance,
        double newBalance,
        double delta,
        String cause,
        String category,
        String source,
        String oldExact,
        String newExact,
        String deltaExact,
        String origin) {

    /** 旧构造器：金额精确值由 double 兜底推导。 */
    public LedgerEntry(String uuid, String name, long time,
                       double oldBalance, double newBalance, double delta,
                       String cause, String category, String source) {
        this(uuid, name, time, oldBalance, newBalance, delta, cause, category, source,
                BigDecimal.valueOf(oldBalance).toPlainString(),
                BigDecimal.valueOf(newBalance).toPlainString(),
                BigDecimal.valueOf(delta).toPlainString(),
                "EVENT");
    }

    /** 展示用金额：优先精确文本。 */
    public String displayDelta() {
        return deltaExact != null && !deltaExact.isEmpty() ? deltaExact : BigDecimal.valueOf(delta).toPlainString();
    }

    public String displayOld() {
        return oldExact != null && !oldExact.isEmpty() ? oldExact : BigDecimal.valueOf(oldBalance).toPlainString();
    }

    public String displayNew() {
        return newExact != null && !newExact.isEmpty() ? newExact : BigDecimal.valueOf(newBalance).toPlainString();
    }

    public String safeOrigin() {
        return origin == null || origin.isEmpty() ? "EVENT" : origin;
    }
}
