package com.nangua.moneyledger.ledger;

/**
 * 一笔南瓜币变动记录。
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
        String source) {
}
