package com.nangua.buildrate.storage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** 统计计算：均分（去最高最低）、票数、排名 */
public final class Stats {
    private Stats() {}

    /**
     * 计算某活动全部建筑的结果，按均分降序、票数降序排名。
     * 去最高最低规则：票数 >= 3 且开启 dropHiLo 时，去掉 1 个最高分与 1 个最低分后求平均；
     * 票数 <= 2 时按全部票平均（否则被扣光）。
     */
    public static List<Models.BuildResult> compute(List<Models.Build> builds,
                                                   Map<Integer, List<Models.Score>> scoresByBuild,
                                                   boolean dropHiLo) {
        List<Models.BuildResult> out = new ArrayList<>();
        for (Models.Build b : builds) {
            List<Models.Score> scores = scoresByBuild.getOrDefault(b.id(), new ArrayList<>());
            int raw = scores.size();
            int dropped = 0;
            int min = raw > 0 ? Integer.MAX_VALUE : 0;
            int max = raw > 0 ? Integer.MIN_VALUE : 0;
            long sum = 0;
            for (Models.Score sc : scores) {
                min = Math.min(min, sc.score());
                max = Math.max(max, sc.score());
                sum += sc.score();
            }
            int denom = raw;
            if (dropHiLo && raw >= 3) {
                sum -= (min + max);
                denom = raw - 2;
                dropped = 2;
            }
            double avg = denom > 0 ? (double) sum / denom : 0.0;
            out.add(new Models.BuildResult(b, avg, denom, raw, min, max, dropped));
        }
        out.sort(Comparator
                .comparingDouble(Models.BuildResult::avg).reversed()
                .thenComparing(Models.BuildResult::votes, Comparator.reverseOrder()));
        return out;
    }
}
