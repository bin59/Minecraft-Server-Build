package com.nangua.buildrate.storage;

/** 数据模型：活动 / 参评建筑 / 评分记录 / 结果汇总 */
public final class Models {
    private Models() {}

    /** 一场评分活动。status 三态：0=待开始（可报名、不可打分） / 1=进行中（可打分） / 2=已截止。
     *  流转：create 写入 0 → start 置 1 → stop 置 2；创建新活动时会把旧的 status=1 一律刷成 2。
     *  isActive() 只认 status==1，所以报名类查询用 getOpenEvent(IN 0,1)、打分/统计用 getActiveEvent(=1)。 */
    public record Event(int id, String name, int status, long createdAt) {
        public boolean isActive() { return status == 1; }
    }

    /** 一座参评建筑（一位参评玩家一场活动一座） */
    public record Build(int id, int eventId, String player, String name,
                        String world, int x, int y, int z) {}

    /** 一条评分（每建筑每评分人唯一，改分覆盖） */
    public record Score(int buildId, String voter, int score, long updatedAt) {}

    /** 单座建筑的统计结果 */
    public record BuildResult(Build build, double avg, int votes,
                              int rawVotes, int min, int max, int dropped) {}
}
