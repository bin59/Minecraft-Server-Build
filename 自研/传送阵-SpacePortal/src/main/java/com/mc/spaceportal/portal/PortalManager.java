package com.mc.spaceportal.portal;

import com.mc.spaceportal.SpacePortalPlugin;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 传送阵管理器：创建 / 删除 / 连接 / 持久化（portals.yml）。
 */
public class PortalManager {

    private final SpacePortalPlugin plugin;
    private final Map<String, Portal> portals = new LinkedHashMap<>();
    private File file;

    public PortalManager(SpacePortalPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        portals.clear();
        file = new File(plugin.getDataFolder(), "portals.yml");
        if (!file.exists()) {
            return;
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = cfg.getConfigurationSection("portals");
        if (root == null) {
            return;
        }
        for (String name : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(name);
            if (s == null) {
                continue;
            }
            String world = s.getString("world");
            if (world == null) {
                continue;
            }
            double x = s.getDouble("x");
            double y = s.getDouble("y");
            double z = s.getDouble("z");
            String dest = s.getString("dest", null);
            float hue = (float) s.getDouble("hue", 185.0);
            java.util.UUID owner = null;
            String ownerStr = s.getString("owner", null);
            if (ownerStr != null && !ownerStr.isEmpty()) {
                try {
                    owner = java.util.UUID.fromString(ownerStr);
                } catch (IllegalArgumentException ignored) {
                    // 归属者字段损坏时按公共阵法处理
                }
            }
            portals.put(name.toLowerCase(Locale.ROOT), new Portal(name, world, x, y, z, dest, hue, owner));
        }
    }

    public void save() {
        if (file == null) {
            file = new File(plugin.getDataFolder(), "portals.yml");
        }
        YamlConfiguration cfg = new YamlConfiguration();
        for (Portal portal : portals.values()) {
            String path = "portals." + portal.getName() + ".";
            cfg.set(path + "world", portal.getWorldName());
            cfg.set(path + "x", portal.getX());
            cfg.set(path + "y", portal.getY());
            cfg.set(path + "z", portal.getZ());
            cfg.set(path + "dest", portal.getDestName());
            cfg.set(path + "hue", portal.getHue());
            if (portal.getOwner() != null) {
                cfg.set(path + "owner", portal.getOwner().toString());
            }
        }
        try {
            cfg.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("保存 portals.yml 失败: " + ex.getMessage());
        }
    }

    public boolean create(String name, Location location, float hue) {
        return create(name, location, hue, null);
    }

    public boolean create(String name, Location location, float hue, java.util.UUID owner) {
        if (get(name) != null) {
            return false;
        }
        portals.put(name.toLowerCase(Locale.ROOT), new Portal(
                name,
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                null,
                hue,
                owner));
        save();
        return true;
    }

    public boolean remove(String name) {
        Portal removed = portals.remove(name.toLowerCase(Locale.ROOT));
        if (removed == null) {
            return false;
        }
        // 清理所有指向它的连接
        for (Portal portal : portals.values()) {
            if (removed.getName().equalsIgnoreCase(portal.getDestName())) {
                portal.setDestName(null);
            }
        }
        save();
        return true;
    }

    public boolean link(String from, String to) {
        Portal a = get(from);
        Portal b = get(to);
        if (a == null || b == null) {
            return false;
        }
        a.setDestName(b.getName());
        save();
        return true;
    }

    public boolean link2Way(String a, String b) {
        return link(a, b) && link(b, a);
    }

    public Portal get(String name) {
        if (name == null) {
            return null;
        }
        return portals.get(name.toLowerCase(Locale.ROOT));
    }

    public Collection<Portal> all() {
        return Collections.unmodifiableCollection(portals.values());
    }

    public int size() {
        return portals.size();
    }

    /** 统计某个玩家名下的传送阵数量 */
    public int countByOwner(java.util.UUID owner) {
        if (owner == null) {
            return 0;
        }
        int count = 0;
        for (Portal portal : portals.values()) {
            if (owner.equals(portal.getOwner())) {
                count++;
            }
        }
        return count;
    }
}
