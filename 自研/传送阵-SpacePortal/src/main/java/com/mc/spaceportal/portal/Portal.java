package com.mc.spaceportal.portal;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.UUID;

/**
 * 一个空间传送阵：包含位置、目的地、特效颜色、归属者。
 */
public class Portal {

    private final String name;
    private final String worldName;
    private final double x;
    private final double y;
    private final double z;
    private String destName;
    private float hue;
    /** 归属者 UUID；null 表示管理员创建的公共阵法 */
    private UUID owner;

    public Portal(String name, String worldName, double x, double y, double z, String destName, float hue) {
        this(name, worldName, x, y, z, destName, hue, null);
    }

    public Portal(String name, String worldName, double x, double y, double z,
                  String destName, float hue, UUID owner) {
        this.name = name;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.destName = destName;
        this.hue = hue;
        this.owner = owner;
    }

    public String getName() {
        return name;
    }

    public String getWorldName() {
        return worldName;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public String getDestName() {
        return destName;
    }

    public void setDestName(String destName) {
        this.destName = destName;
    }

    public float getHue() {
        return hue;
    }

    public void setHue(float hue) {
        this.hue = hue;
    }

    /** 归属者 UUID；null 表示管理员创建的公共阵法 */
    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    /** 阵法中心位置（世界未加载时 world 为 null，调用方需先检查） */
    public Location getCenter() {
        return new Location(Bukkit.getWorld(worldName), x, y, z);
    }
}
