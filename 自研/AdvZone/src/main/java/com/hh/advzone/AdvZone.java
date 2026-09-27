package com.hh.advzone;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * AdvZone —— 独立版：不规则多边形区域强制冒险模式
 * 不依赖 Residence / WorldGuard，区域在游戏内用"箭"选点创建，自动写回 config.yml。
 *
 * 选点：手持箭 左键=在脚下位置加一个顶点，右键=撤销上一个顶点（带粒子可视化）
 * 命令：/az（/advzone 别名）
 *   wand / add / undo / clear / points          —— 选点管理
 *   create <名> [yMin] [yMax]                   —— 用选好的点创建/覆盖区域
 *   edit <名> / delete <名> / list / info <名>   —— 编辑、删除、查看
 *   reload / check                              —— 重载 / 查看所在区域
 * 玩家进入区域 → 强制冒险；离开 → 恢复进区前模式；advzone.bypass 或 OP 豁免。
 */
public final class AdvZone extends JavaPlugin implements Listener {

    /** 一个多边形区域：顶点(x,z) + 垂直范围 + 包围盒缓存 */
    private static final class Zone {
        final String name;
        final double[][] points;
        final double yMin, yMax;
        final double minX, maxX, minZ, maxZ;

        Zone(String name, double[][] points, double yMin, double yMax) {
            this.name = name;
            this.points = points;
            this.yMin = yMin;
            this.yMax = yMax;
            double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
            double minZ = Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
            for (double[] p : points) {
                if (p[0] < minX) minX = p[0];
                if (p[0] > maxX) maxX = p[0];
                if (p[1] < minZ) minZ = p[1];
                if (p[1] > maxZ) maxZ = p[1];
            }
            this.minX = minX;
            this.maxX = maxX;
            this.minZ = minZ;
            this.maxZ = maxZ;
        }

        /** 射线法判点在多边形内 + 包围盒快筛（框外零循环） */
        boolean contains(Location loc) {
            double x = loc.getX(), y = loc.getY(), z = loc.getZ();
            if (y < yMin || y > yMax) return false;
            if (x < minX || x > maxX || z < minZ || z > maxZ) return false;
            boolean inside = false;
            int n = points.length;
            for (int i = 0, j = n - 1; i < n; j = i++) {
                double xi = points[i][0], zi = points[i][1];
                double xj = points[j][0], zj = points[j][1];
                if (((zi > z) != (zj > z)) && (x < (xj - xi) * (z - zi) / (zj - zi) + xi)) {
                    inside = !inside;
                }
            }
            return inside;
        }
    }

    private final List<Zone> zones = new ArrayList<>();
    /** 被本插件强制改成冒险的玩家 -> 进区前的模式 */
    private final Map<UUID, GameMode> forced = new HashMap<>();
    /** 每个玩家的选点缓冲（x,z），箭左键加 / 右键删 */
    private final Map<UUID, Deque<double[]>> selection = new HashMap<>();
    private Material wandMaterial = Material.ARROW;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        String mat = getConfig().getString("wand-material", "ARROW");
        Material m = Material.matchMaterial(mat == null ? "ARROW" : mat);
        wandMaterial = m != null ? m : Material.ARROW;
        loadZones();
        getServer().getPluginManager().registerEvents(this, this);
        startParticlePreview();
    }

    @Override
    public void onDisable() {
        // 关服/卸载时把被强制的玩家还原，避免卡在冒险模式
        for (Map.Entry<UUID, GameMode> e : forced.entrySet()) {
            Player p = getServer().getPlayer(e.getKey());
            if (p != null && p.isOnline()) p.setGameMode(e.getValue());
        }
        forced.clear();
    }

    /** 选点可视化：给有选点的玩家在每个顶点画一根粒子柱，方便对位 */
    private void startParticlePreview() {
        getServer().getScheduler().runTaskTimer(this, () -> {
            for (Player p : getServer().getOnlinePlayers()) {
                Deque<double[]> pts = selection.get(p.getUniqueId());
                if (pts == null || pts.isEmpty()) continue;
                World w = p.getWorld();
                double baseY = p.getLocation().getY() - 1.0;
                for (double[] pt : pts) {
                    for (double dy = 0; dy <= 6.0; dy += 0.5) {
                        w.spawnParticle(Particle.END_ROD, pt[0] + 0.5, baseY + dy, pt[1] + 0.5, 1);
                    }
                }
            }
        }, 20L, 10L);
    }

    /* ---------------- 配置加载 ---------------- */

    private void loadZones() {
        zones.clear();
        ConfigurationSection root = getConfig().getConfigurationSection("regions");
        if (root == null) {
            getLogger().warning("config.yml 中缺少 regions 配置，插件不会强制任何区域。");
            return;
        }
        for (String name : root.getKeys(false)) {
            ConfigurationSection sec = root.getConfigurationSection(name);
            if (sec == null) continue;
            List<String> raw = sec.getStringList("points");
            if (raw.size() < 3) {
                getLogger().warning("区域 [" + name + "] 的 points 少于 3 个顶点，已跳过。");
                continue;
            }
            try {
                double[][] pts = new double[raw.size()][2];
                for (int i = 0; i < raw.size(); i++) {
                    String[] parts = raw.get(i).split(",");
                    pts[i][0] = Double.parseDouble(parts[0].trim());
                    pts[i][1] = Double.parseDouble(parts[1].trim());
                }
                double yMin = sec.getDouble("y-min", -64);
                double yMax = sec.getDouble("y-max", 320);
                zones.add(new Zone(name, pts, yMin, yMax));
                getLogger().info("已加载区域 [" + name + "] " + pts.length
                        + " 个顶点，Y " + (int) yMin + "~" + (int) yMax);
            } catch (Exception ex) {
                getLogger().warning("区域 [" + name + "] 配置解析失败：" + ex.getMessage());
            }
        }
    }

    private void saveRegion(String name, Deque<double[]> pts, double yMin, double yMax) {
        List<String> list = new ArrayList<>();
        for (double[] pt : pts) {
            list.add(String.format(Locale.ROOT, "%.2f,%.2f", pt[0], pt[1]));
        }
        getConfig().set("regions." + name + ".points", list);
        getConfig().set("regions." + name + ".y-min", yMin);
        getConfig().set("regions." + name + ".y-max", yMax);
        saveConfig();
        loadZones();
    }

    private void deleteRegion(String name) {
        getConfig().set("regions." + name, null);
        saveConfig();
        loadZones();
    }

    /* ---------------- 选点（箭魔棒 + 命令） ---------------- */

    private Deque<double[]> sel(Player p) {
        return selection.computeIfAbsent(p.getUniqueId(), k -> new ArrayDeque<>());
    }

    private void addPoint(Player p, double x, double z) {
        Deque<double[]> pts = sel(p);
        pts.addLast(new double[]{x, z});
        p.sendMessage("§a已添加顶点 #" + pts.size() + " §7(" + fmt(x) + ", " + fmt(z) + ")"
                + " §8| 左键继续加点，右键撤销，/az create <名> 保存");
    }

    private void undoPoint(Player p) {
        Deque<double[]> pts = sel(p);
        if (pts.isEmpty()) {
            p.sendMessage("§c没有可撤销的顶点。");
            return;
        }
        double[] last = pts.removeLast();
        p.sendMessage("§e已撤销顶点 §7(" + fmt(last[0]) + ", " + fmt(last[1]) + ") §8| 剩余 " + pts.size() + " 个");
    }

    private static String fmt(double v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }

    /* ---------------- 事件 ---------------- */

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getItem() == null || e.getItem().getType() != wandMaterial) return;
        Action a = e.getAction();
        if (a == Action.LEFT_CLICK_AIR || a == Action.LEFT_CLICK_BLOCK) {
            e.setCancelled(true); // 防止误破坏方块
            Location l = e.getPlayer().getLocation();
            addPoint(e.getPlayer(), l.getX(), l.getZ());
        } else if (a == Action.RIGHT_CLICK_AIR || a == Action.RIGHT_CLICK_BLOCK) {
            e.setCancelled(true);
            undoPoint(e.getPlayer());
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        check(e.getPlayer());
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent e) {
        check(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        forced.remove(e.getPlayer().getUniqueId());
        selection.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Location from = e.getFrom(), to = e.getTo();
        if (to == null) return;
        // 性能关键：只在跨方块时才做真判定
        if (from.getBlockX() != to.getBlockX()
                || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ()) {
            check(e.getPlayer());
        }
    }

    /* ---------------- 核心逻辑 ---------------- */

    private void check(Player p) {
        if (p.hasPermission("advzone.bypass") || p.isOp()) return; // 管理豁免

        String zone = zoneAt(p.getLocation());
        if (zone != null) {
            if (p.getGameMode() != GameMode.ADVENTURE) {
                forced.put(p.getUniqueId(), p.getGameMode()); // 记住进区前模式
                p.setGameMode(GameMode.ADVENTURE);
            }
        } else if (forced.containsKey(p.getUniqueId())) {
            p.setGameMode(forced.remove(p.getUniqueId()));     // 离开还原
        }
    }

    /** 返回玩家所在区域名；不在任何区域内返回 null */
    private String zoneAt(Location loc) {
        for (Zone z : zones) {
            if (z.contains(loc)) return z.name;
        }
        return null;
    }

    private List<String> zoneNames() {
        List<String> names = new ArrayList<>();
        for (Zone z : zones) names.add(z.name);
        return names;
    }

    /* ---------------- 命令 ---------------- */

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("§c该命令只能由玩家执行。");
            return true;
        }
        if (args.length == 0) {
            sendHelp(p);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "wand" -> {
                p.getInventory().addItem(new ItemStack(wandMaterial));
                p.sendMessage("§a已给你选点魔棒（" + wandMaterial.name() + "）：§7站在拐角左键加点，右键撤销");
            }
            case "add" -> {
                Location l = p.getLocation();
                addPoint(p, l.getX(), l.getZ());
            }
            case "undo" -> undoPoint(p);
            case "clear" -> {
                sel(p).clear();
                p.sendMessage("§e选点已全部清空。");
            }
            case "points" -> {
                Deque<double[]> pts = sel(p);
                if (pts.isEmpty()) {
                    p.sendMessage("§7当前没有任何选点。用 /az wand 拿魔棒，或 /az add 加点。");
                    return true;
                }
                StringBuilder sb = new StringBuilder("§e当前选点 " + pts.size() + " 个：");
                int i = 1;
                for (double[] pt : pts) {
                    sb.append("\n§7#").append(i++).append(" (").append(fmt(pt[0]))
                      .append(", ").append(fmt(pt[1])).append(")");
                }
                p.sendMessage(sb.toString());
            }
            case "create" -> {
                if (args.length < 2) {
                    p.sendMessage("§c用法: /az create <区域名> [yMin] [yMax]");
                    return true;
                }
                Deque<double[]> pts = sel(p);
                if (pts.size() < 3) {
                    p.sendMessage("§c至少需要 3 个顶点，当前 " + pts.size() + " 个。");
                    return true;
                }
                double yMin = args.length >= 3 ? parseD(args[2], -64) : -64;
                double yMax = args.length >= 4 ? parseD(args[3], 320) : 320;
                String name = args[1];
                boolean overwrite = getConfig().getConfigurationSection("regions." + name) != null;
                saveRegion(name, pts, yMin, yMax);
                p.sendMessage("§a区域 [" + name + "] 已保存（" + (overwrite ? "覆盖旧区域" : "新建")
                        + "），" + pts.size() + " 个顶点，Y " + (int) yMin + "~" + (int) yMax
                        + "。§7选点缓冲已清空。");
                sel(p).clear();
            }
            case "edit" -> {
                if (args.length < 2) {
                    p.sendMessage("§c用法: /az edit <区域名>");
                    return true;
                }
                ConfigurationSection sec = getConfig().getConfigurationSection("regions." + args[1]);
                if (sec == null) {
                    p.sendMessage("§c区域 [" + args[1] + "] 不存在。用 /az list 查看现有区域。");
                    return true;
                }
                Deque<double[]> pts = sel(p);
                pts.clear();
                for (String s : sec.getStringList("points")) {
                    String[] parts = s.split(",");
                    pts.addLast(new double[]{Double.parseDouble(parts[0].trim()),
                            Double.parseDouble(parts[1].trim())});
                }
                p.sendMessage("§a已载入区域 [" + args[1] + "] 的 " + pts.size()
                        + " 个顶点。§7右键撤销不要的点、走到新位置左键加新点，改完 /az create " + args[1] + " 覆盖保存。");
            }
            case "delete" -> {
                if (args.length < 2) {
                    p.sendMessage("§c用法: /az delete <区域名>");
                    return true;
                }
                if (getConfig().getConfigurationSection("regions." + args[1]) == null) {
                    p.sendMessage("§c区域 [" + args[1] + "] 不存在。");
                    return true;
                }
                deleteRegion(args[1]);
                p.sendMessage("§a区域 [" + args[1] + "] 已删除。");
            }
            case "list" -> {
                if (zones.isEmpty()) {
                    p.sendMessage("§7当前没有任何区域。");
                    return true;
                }
                StringBuilder sb = new StringBuilder("§e现有区域 " + zones.size() + " 个：");
                for (Zone z : zones) {
                    sb.append("\n§7- ").append(z.name).append(" §8(").append(z.points.length)
                      .append(" 顶点, Y ").append((int) z.yMin).append("~").append((int) z.yMax).append(")");
                }
                p.sendMessage(sb.toString());
            }
            case "info" -> {
                if (args.length < 2) {
                    p.sendMessage("§c用法: /az info <区域名>");
                    return true;
                }
                Zone found = null;
                for (Zone z : zones) {
                    if (z.name.equals(args[1])) found = z;
                }
                if (found == null) {
                    p.sendMessage("§c区域 [" + args[1] + "] 不存在。");
                    return true;
                }
                StringBuilder sb = new StringBuilder("§e区域 [" + found.name + "]§7 Y "
                        + (int) found.yMin + "~" + (int) found.yMax + "，顶点：");
                int i = 1;
                for (double[] pt : found.points) {
                    sb.append("\n§7#").append(i++).append(" (").append(fmt(pt[0]))
                      .append(", ").append(fmt(pt[1])).append(")");
                }
                p.sendMessage(sb.toString());
            }
            case "reload" -> {
                reloadConfig();
                loadZones();
                // 重载后对所有在线玩家重跑判定（被删掉区域的玩家会被自动还原）
                for (Player pl : getServer().getOnlinePlayers()) check(pl);
                p.sendMessage("§aAdvZone 已重载，当前 " + zones.size() + " 个区域。");
            }
            case "check" -> {
                String z = zoneAt(p.getLocation());
                p.sendMessage(z != null
                        ? "§a你当前在区域 [" + z + "] §7模式: " + p.getGameMode()
                        : "§7你当前不在任何 AdvZone 区域内。");
            }
            default -> sendHelp(p);
        }
        return true;
    }

    private double parseD(String s, double def) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException ex) {
            return def;
        }
    }

    private void sendHelp(Player p) {
        p.sendMessage("§e===== AdvZone 用法 =====");
        p.sendMessage("§e/az wand§7 - 拿选点魔棒（" + wandMaterial.name() + "）");
        p.sendMessage("§e/az add§7 / §eundo§7 / §eclear§7 / §epoints§7 - 加点/撤销/清空/查看");
        p.sendMessage("§e/az create <名> [yMin] [yMax]§7 - 用选点创建/覆盖区域");
        p.sendMessage("§e/az edit <名>§7 - 载入区域顶点进行修改");
        p.sendMessage("§e/az delete <名>§7 / §elist§7 / §einfo <名>§7 - 删除/列表/详情");
        p.sendMessage("§e/az reload§7 / §echeck§7 - 重载 / 查看所在区域");
    }

    /* ---------------- Tab 补全 ---------------- */

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("wand", "add", "undo", "clear", "points",
                    "create", "edit", "delete", "list", "info", "reload", "check"), args[0]);
        }
        if (args.length == 2 && List.of("edit", "delete", "info").contains(args[0].toLowerCase())) {
            return filter(zoneNames(), args[1]);
        }
        return List.of();
    }

    private List<String> filter(List<String> options, String token) {
        String t = token.toLowerCase();
        List<String> out = new ArrayList<>();
        for (String o : options) {
            if (o.toLowerCase().startsWith(t)) out.add(o);
        }
        return out;
    }
}
