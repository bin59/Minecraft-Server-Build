package com.nangua.pumpkinmail.storage;

/**
 * 一条待领取奖励（玩家邮箱中的一项）。
 * playerUuid 可为空（仅按名字匹配）；playerName 为主键查询字段。
 */
public record Reward(
        String id,
        String playerUuid,
        String playerName,
        String presetId,
        String rewardType,
        String data,
        String desc,
        String issuedBy,
        long issuedTime,
        boolean claimed
) {
}
