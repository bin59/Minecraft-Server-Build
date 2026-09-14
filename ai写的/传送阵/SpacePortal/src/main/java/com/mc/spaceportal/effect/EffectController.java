package com.mc.spaceportal.effect;

import com.mc.spaceportal.portal.Portal;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 粒子特效：地面传送阵法 + 时光穿梭隧道。
 * 全部使用 Bukkit 标准粒子包，Java 与基岩（Geyser）玩家均可正常显示。
 *
 * 性能约定（重要）：
 * - 每个 spawnParticle 调用 = 一个网络包，因此**减少调用次数**比减少单个粒子数更关键。
 *   所以同一位置的多个粒子用 count 参数一次性发出（1 个包渲染多颗粒子），而不是循环单点发送。
 * - 色相 → Color 的转换结果做了缓存，避免每帧反复新建 Color 对象。
 * - 画面走"简洁"路线：单层外环 + 反向内环 + 六芒星 + 稀疏光柱，去掉了原先的双层外环、
 *   符文环与上升微光，每帧包数约为原来的 1/3，配合 particle-interval-ticks 降频进一步省流。
 */
public class EffectController {

    private static final double TWO_PI = Math.PI * 2;

    /** 地面法阵整体亮度（0~1，越大越亮） */
    private static final float GLOW = 0.9f;

    /** 同一位置一次性发出的粒子数：增加视觉密度但不增加网络包数量 */
    private static final int RING_DENSITY = 2;
    /** 单点粒子的随机散布半径 */
    private static final double SPREAD = 0.06;

    private EffectController() {
    }

    /**
     * 渲染一帧地面传送阵法：
     * 单层旋转外环 + 反向旋转内环 + 六芒星 + 稀疏上升光柱（充能时追加聚拢螺旋）。
     *
     * @param center 阵法中心（地面位置，粒子在其上方 0.05~3.0 格绘制）
     * @param radius 阵法半径
     * @param hue    主色调（0~360）
     * @param phase  动画相位
     * @param charge 充能进度（0~1），未充能时传 0；充能时追加聚拢螺旋并在完成时闪光
     */
    public static void drawPortalCircle(Location center, double radius, float hue, double phase, double charge) {
        if (center.getWorld() == null) {
            return;
        }
        Location p = center.clone();
        double yOff = 0.05;

        // ── 外环 + 反向内环，构成法阵轮廓（原为双层外环再叠加一层内环，已合并精简）──
        ring(p, center, radius, 16, hue, phase * 0.5, yOff);
        ring(p, center, radius * 0.55, 10, hue + 25f, -phase * 0.6, yOff);

        // ── 六芒星：两个交错三角形（法阵的核心纹样）──
        double starR = radius * 0.8;
        triangle(p, center, starR, hue + 15f, phase * 0.22);
        triangle(p, center, starR, hue + 15f, phase * 0.22 + Math.PI);

        // ── 中央光柱：稀疏上升光点，仅表示能量向上（原为 14 层×3 点，已大幅削弱）──
        double beamHeight = 2.6 + Math.sin(phase * 1.5) * 0.3;
        for (double h = 0.3; h <= beamHeight; h += 0.7) {
            double a = phase * 1.6 + h * 1.4;
            emit(p, center.getX() + Math.cos(a) * 0.25, center.getY() + h,
                    center.getZ() + Math.sin(a) * 0.25, hue, 1.0f, 1, 0);
        }

        // ── 充能聚拢螺旋（仅蓄力期间追加，能量向内汇聚）──
        if (charge > 0) {
            for (int i = 0; i < 8; i++) {
                double t = i / 8.0;
                double rr = radius * (1.0 - t * 0.85);
                double a = -phase * 2.6 + i * 0.85;
                emit(p, center.getX() + Math.cos(a) * rr, center.getY() + yOff + t * 2.4 * charge,
                        center.getZ() + Math.sin(a) * rr, hue + 60f, (float) (0.8 + charge), 1, 0);
            }
            if (charge >= 1.0) {
                emit(p, center.getX(), center.getY() + 1.2, center.getZ(), hue, 2.0f, 10, 0.15);
            }
        }
    }

    /** 单层圆环 */
    private static void ring(Location p, Location center, double r, int points, float hue, double rotation, double yOff) {
        for (int i = 0; i < points; i++) {
            double a = rotation + i * TWO_PI / points;
            emit(p, center.getX() + Math.cos(a) * r, center.getY() + yOff,
                    center.getZ() + Math.sin(a) * r, hue, GLOW, RING_DENSITY, SPREAD);
        }
    }

    /** 等边三角形（六芒星的一半），绕中心旋转 offset */
    private static void triangle(Location p, Location center, double r, float hue, double offset) {
        for (int side = 0; side < 3; side++) {
            double a1 = offset + side * (TWO_PI / 3);
            double a2 = offset + (side + 1) * (TWO_PI / 3);
            double x1 = center.getX() + Math.cos(a1) * r;
            double z1 = center.getZ() + Math.sin(a1) * r;
            double x2 = center.getX() + Math.cos(a2) * r;
            double z2 = center.getZ() + Math.sin(a2) * r;
            for (int i = 0; i < 4; i++) {
                double t = i / 4.0;
                emit(p, x1 + (x2 - x1) * t, center.getY() + 0.05,
                        z1 + (z2 - z1) * t, hue, GLOW, 1, 0);
            }
        }
    }

    /** 充能完成瞬间的爆发特效 */
    public static void chargeComplete(Player player, Location center, float hue) {
        Location p = center.clone();
        emit(p, center.getX(), center.getY() + 1.0, center.getZ(), hue, 2.2f, 30, 0.15);
        for (int i = 0; i < 12; i++) {
            double a = i * TWO_PI / 12;
            emit(p, center.getX() + Math.cos(a) * 2.2, center.getY() + 0.2,
                    center.getZ() + Math.sin(a) * 2.2, hue + 60f, 1.1f, 1, 0);
        }
        player.getWorld().playSound(center, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.4f);
        player.getWorld().playSound(center, Sound.BLOCK_PORTAL_TRAVEL, 0.8f, 1.2f);
    }

    /** 传送瞬间：起点收缩漩涡 + 终点爆发闪光 */
    public static void teleportBurst(Location from, Location to, float hue) {
        Location p = new Location(from.getWorld(), 0, 0, 0);
        for (int i = 0; i < 12; i++) {
            double a = i * TWO_PI / 12;
            double r = 1.6 - i * 0.08;
            emit(p, from.getX() + Math.cos(a) * r, from.getY() + 1.0 + i * 0.06,
                    from.getZ() + Math.sin(a) * r, hue, 1.0f, 1, 0);
        }
        if (to.getWorld() != null) {
            p.setWorld(to.getWorld());
            emit(p, to.getX(), to.getY() + 1.0, to.getZ(), hue, 2.0f, 24, 0.15);
            to.getWorld().playSound(to, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.8f);
        }
    }

    /**
     * 时光穿梭隧道：双螺旋光带 + 头顶流光。建议由调用方每 2 tick 调用一次。
     *
     * @param player 玩家
     * @param age    特效已持续的调用次数
     * @param total  特效总调用次数
     * @return true 表示特效已结束
     */
    public static boolean drawTimeTunnel(Player player, int age, int total) {
        if (!player.isOnline()) {
            return true;
        }
        if (age >= total) {
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.2f);
            return true;
        }
        Location base = player.getLocation();
        Location p = new Location(base.getWorld(), 0, 0, 0);
        double phase = age * 0.55;

        // 双螺旋隧道：沿玩家身体轴向旋转的光带
        for (int strand = 0; strand < 2; strand++) {
            for (int i = 0; i < 8; i++) {
                double h = 0.15 + (i * 2.6) / 8;
                double a = phase * (strand == 0 ? 1 : -1) + i * 0.8 + strand * Math.PI;
                double r = 1.05 + 0.22 * Math.sin(age * 0.18 + i * 0.55);
                float hue = (float) ((age * 9.0 + i * 30.0 + strand * 180.0) % 360.0);
                emit(p, base.getX() + Math.cos(a) * r, base.getY() + h,
                        base.getZ() + Math.sin(a) * r, hue, 0.85f, 1, 0);
            }
        }

        // 头顶流光圆盘
        for (int i = 0; i < 6; i++) {
            double a = phase * 1.8 + i * TWO_PI / 6;
            double r = 0.5 + 0.3 * Math.sin(age * 0.3 + i);
            emit(p, base.getX() + Math.cos(a) * r, base.getY() + 2.35,
                    base.getZ() + Math.sin(a) * r, (float) ((age * 12.0 + i * 60.0) % 360.0), 0.7f, 1, 0);
        }

        // 收尾闪光
        if (age == total - 1) {
            emit(p, base.getX(), base.getY() + 1.0, base.getZ(), 200f, 1.5f, 16, 0.15);
        }
        return false;
    }

    /** 充能取消的熄灭特效 */
    public static void cancelPuff(Location location, float hue) {
        if (location.getWorld() == null) {
            return;
        }
        Location p = location.clone();
        emit(p, location.getX(), location.getY() + 1.0, location.getZ(), hue + 180f, 1.2f, 12, 0.15);
        location.getWorld().playSound(location, Sound.BLOCK_FIRE_EXTINGUISH, 0.8f, 1.6f);
    }

    // ---------------- 内部工具 ----------------

    private static void emit(Location p, double x, double y, double z, float hue, float size, int count, double spread) {
        p.setX(x);
        p.setY(y);
        p.setZ(z);
        p.getWorld().spawnParticle(Particle.REDSTONE, p, count, spread, spread, spread, 0,
                new Particle.DustOptions(hueToColor(hue), size));
    }

    /** 白色色相哨兵值（hue <= -900 表示低饱和度白色） */
    public static final float WHITE_HUE = -999f;

    /** 色相转换结果缓存：key = -1（白）或 0~359 的整数度数 */
    private static final Map<Integer, Color> COLOR_CACHE = new ConcurrentHashMap<>();

    private static Color hueToColor(float hue) {
        boolean white = hue <= -900f;
        int key = white ? -1 : (int) (((hue % 360f) + 360f) % 360f);
        return COLOR_CACHE.computeIfAbsent(key, EffectController::buildColor);
    }

    /** HSB → Bukkit Color（Bukkit 无 fromHSB，手动转换） */
    private static Color buildColor(int key) {
        boolean white = key < 0;
        float h = white ? 0.6f : key / 360f;
        float sat = white ? 0.10f : 0.75f;
        // 标准 HSB→RGB（v = 1.0）
        int i = (int) (h * 6) % 6;
        float f = h * 6 - (int) (h * 6);
        float q = 1 - f * sat;
        float t = f * sat;
        float r, g, b;
        switch (i) {
            case 0: r = 1; g = t; b = 0; break;
            case 1: r = q; g = 1; b = 0; break;
            case 2: r = 0; g = 1; b = t; break;
            case 3: r = 0; g = q; b = 1; break;
            case 4: r = t; g = 0; b = 1; break;
            default: r = 1; g = 0; b = q; break;
        }
        return Color.fromRGB((int) (r * 255), (int) (g * 255), (int) (b * 255));
    }

    /** 供外部调用的简化接口：绘制一个传送阵的一帧 */
    public static void drawPortalFrame(Portal portal, Location center, double radius, double phase, double charge) {
        drawPortalCircle(center, radius, portal.getHue(), phase, charge);
    }

    /** 会话工具：判断玩家是否仍在阵法内（供监听器使用，避免重复代码） */
    public static boolean inRange(Location a, Location b, double horizontal, double vertical) {
        if (a.getWorld() == null || b.getWorld() == null || !a.getWorld().equals(b.getWorld())) {
            return false;
        }
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        double dy = a.getY() - b.getY();
        return dx * dx + dz * dz <= horizontal * horizontal && Math.abs(dy) <= vertical;
    }
}
