package com.nangua.pumpkinfly;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 时长文本解析与格式化。
 *
 * <p>奖励/发放支持「几天、几小时、几分钟」：{@code 3d}、{@code 6h}、{@code 30m}、
 * {@code 45s}、组合 {@code 1d2h30m}，也支持中文 {@code 1天2小时30分}。
 * 纯数字（如 {@code 30}）沿用旧口径，按<b>分钟</b>处理，保证老命令与老菜单不失效。
 */
public final class TimeUtil {

    /** 单次发放上限，防止误输入把到期时间顶到几十年后。 */
    public static final long MAX_SECONDS = 366L * 24L * 3600L;

    private static final Pattern PART = Pattern.compile("(\\d+)\\s*([dhms])");
    private static final SimpleDateFormat CLOCK = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private TimeUtil() { }

    /** 解析为秒数；非法/为 0/超上限返回 -1。 */
    public static long parseSeconds(String raw) {
        if (raw == null) return -1L;
        String s = raw.trim().toLowerCase(Locale.ROOT).replace(" ", "");
        if (s.isEmpty()) return -1L;
        // 中文单位先按词、再按单字归一到 d/h/m/s
        s = s.replace("小时", "h").replace("分钟", "m").replace("秒钟", "s")
                .replace("天", "d").replace("时", "h").replace("分", "m").replace("秒", "s");
        if (s.matches("\\d+")) {
            long minutes;
            try { minutes = Long.parseLong(s); } catch (NumberFormatException e) { return -1L; }
            long seconds = minutes * 60L;
            return (seconds > 0L && seconds <= MAX_SECONDS) ? seconds : -1L;
        }
        Matcher m = PART.matcher(s);
        long total = 0L;
        int pos = 0;
        boolean found = false;
        while (m.find()) {
            if (!s.substring(pos, m.start()).isEmpty()) return -1L;
            long n;
            try { n = Long.parseLong(m.group(1)); } catch (NumberFormatException e) { return -1L; }
            switch (m.group(2)) {
                case "d": total += n * 86400L; break;
                case "h": total += n * 3600L; break;
                case "m": total += n * 60L; break;
                default: total += n; break;
            }
            pos = m.end();
            found = true;
            if (total > MAX_SECONDS) return -1L;
        }
        if (!found || pos != s.length() || total <= 0L) return -1L;
        return total;
    }

    /** 秒数 → 「3 天 2 小时 5 分 10 秒」，供消息、status、HUD 使用。 */
    public static String format(long seconds) {
        if (seconds <= 0L) return "0 秒";
        long d = seconds / 86400L;
        long h = (seconds % 86400L) / 3600L;
        long m = (seconds % 3600L) / 60L;
        long s = seconds % 60L;
        StringBuilder sb = new StringBuilder();
        if (d > 0L) sb.append(d).append(" 天 ");
        if (h > 0L) sb.append(h).append(" 小时 ");
        if (m > 0L) sb.append(m).append(" 分 ");
        if (s > 0L || sb.length() == 0) sb.append(s).append(" 秒");
        return sb.toString().trim();
    }

    /** 毫秒时间戳 → 可读的到期时间。 */
    public static String clock(long millis) {
        if (millis <= 0L) return "无";
        synchronized (CLOCK) {
            return CLOCK.format(new Date(millis));
        }
    }

    /** 今天（yyyy-MM-dd），用于 DAILY 领取判定。 */
    public static String today() {
        synchronized (CLOCK) {
            return new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        }
    }
}
