package com.nangua.pumpkinmail.storage;

/**
 * 奖品预设（奖品库）。发放时只需引用 presetId。
 * rewardType: money / item / command
 */
public record Preset(String id, String rewardType, String data, String desc) {

    /** 人类可读的奖品摘要，用于菜单与提示。 */
    public String summary() {
        return switch (rewardType) {
            case "money" -> desc + " " + data + " 币";
            case "item" -> {
                String[] p = data.split(":", 2);
                yield desc + " " + p[0] + " x" + (p.length > 1 ? p[1] : "1");
            }
            default -> desc == null || desc.isBlank() ? "命令奖励" : desc;
        };
    }
}
