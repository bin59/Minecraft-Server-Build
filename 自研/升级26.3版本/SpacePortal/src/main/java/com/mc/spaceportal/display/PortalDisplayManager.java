package com.mc.spaceportal.display;

import com.mc.spaceportal.SpacePortalPlugin;
import com.mc.spaceportal.portal.Portal;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 传送阵的「特殊物品」展示管理。
 *
 * <p>关闭粒子特效后，地面法阵不再可见，本类在每个阵法中心上方放置一个展示实体，
 * 用来标记阵法位置。支持三种实体类型：</p>
 * <ul>
 *   <li>{@code ITEM}：悬浮掉落物，Geyser 有原生 ItemEntity 映射，Java / 基岩双端可见（互通服默认）；</li>
 *   <li>{@code ITEM_DISPLAY}：Java 端展示实体，Geyser 无对应映射，基岩端不可见（纯 Java 服默认）；</li>
 *   <li>{@code ARMOR_STAND}：隐形 marker 盔甲架头顶戴物品，兼容模式。</li>
 * </ul>
 *
 * <p>展示实体具有以下特性：</p>
 * <ul>
 *   <li>无碰撞（掉落物模式除外）、不可拾取、不可交互；</li>
 *   <li>{@code setPersistent(false)}，不写入存档，服务器重启后不会残留；</li>
 *   <li>带 PDC 标记，重载/删除阵法时可精确清理，不会误伤其他实体；</li>
 *   <li>静态（无动画、无粒子、无音效），不产生周期性网络包。</li>
 * </ul>
 *
 * <p><b>幂等策略</b>：每个阵法严格只保留一个标记实体。补齐任务不再只依赖内存中的
 * UUID 表，而是每次扫描全服带 PDC 标记的实体，按阵法名分组，删除多余实体、孤儿实体
 * 与展示模式不匹配的旧实体，再为缺失的阵法补建。这样即便实体被合并、被清理插件
 * 移除、被推出范围或插件热重载，也不会出现「同一阵法堆出很多个物品」。</p>
 *
 * <p>展示物品可由管理员手持任意物品（含带数据组件的自定义物品）执行
 * {@code /csz item set} 保存，序列化写入 override.yml，重启后仍生效。</p>
 */
public final class PortalDisplayManager implements Listener {

    /** 每 100 tick（5 秒）做一次幂等去重与补齐，不发送任何粒子或音效 */
    private static final long ENSURE_PERIOD = 100L;
    /** 标记实体偏离中心超过此距离才纠正位置，避免产生周期性发包 */
    private static final double MAX_DRIFT = 0.75;

    private final SpacePortalPlugin plugin;
    /** PDC：标记实体归属的阵法名 */
    private final NamespacedKey marker;
    /** PDC：标记实体创建时的展示模式，用于模式切换后清理旧实体 */
    private final NamespacedKey modeKey;
    /** 传送阵名（小写） -> 展示实体 UUID，仅作记录，真实状态以 PDC 扫描为准 */
    private final Map<String, UUID> spawned = new HashMap<>();
    private BukkitTask task;

    public PortalDisplayManager(SpacePortalPlugin plugin) {
        this.plugin = plugin;
        this.marker = new NamespacedKey(plugin, "portal_display");
        this.modeKey = new NamespacedKey(plugin, "portal_display_mode");
        // ITEM 模式下防止掉落物被合并 / 消失 / 拾取 / 漏斗吸走（只注册一次）
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // ------------------------------------------------------------------
    // ITEM 模式的实体存活保护：任何一个标记物品失效，都会触发补建
    // ------------------------------------------------------------------

    /** 标记掉落物不参与原版物品合并，避免 UUID 失效或堆叠数量被改写 */
    @EventHandler(ignoreCancelled = true)
    public void onItemMerge(ItemMergeEvent event) {
        if (isMarker(event.getEntity()) || isMarker(event.getTarget())) {
            event.setCancelled(true);
        }
    }

    /** 标记掉落物不允许自然消失 */
    @EventHandler(ignoreCancelled = true)
    public void onItemDespawn(ItemDespawnEvent event) {
        if (isMarker(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    /** 生物 / 玩家拾取兜底（setCanPlayerPickup / setCanMobPickup 之外再挡一层） */
    @EventHandler(ignoreCancelled = true)
    public void onEntityPickup(EntityPickupItemEvent event) {
        if (isMarker(event.getItem())) {
            event.setCancelled(true);
        }
    }

    /** 带 PDC 标记的掉落物禁止被漏斗 / 漏斗矿车吸走 */
    @EventHandler(ignoreCancelled = true)
    public void onInventoryPickup(InventoryPickupItemEvent event) {
        if (isMarker(event.getItem())) {
            event.setCancelled(true);
        }
    }

    private boolean isMarker(Entity entity) {
        return entity != null
                && entity.getPersistentDataContainer().has(marker, PersistentDataType.STRING);
    }

    // ------------------------------------------------------------------
    // 生命周期
    // ------------------------------------------------------------------

    /** 启动或重启：立即去重补齐一次，之后低频幂等维护 */
    public void start() {
        restart();
    }

    /** 配置 / 展示物品变更后调用，立即去重补齐 */
    public void refresh() {
        restart();
    }

    /** 关闭展示：取消任务并移除全部标记实体 */
    public void stop() {
        cancelTask();
        removeAll();
    }

    private void restart() {
        cancelTask();
        if (!plugin.isDisplayEnabled()) {
            return;
        }
        ensure();
        task = plugin.getServer().getScheduler()
                .runTaskTimer(plugin, this::ensure, ENSURE_PERIOD, ENSURE_PERIOD);
    }

    private void cancelTask() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    /**
     * 全量幂等维护：每个阵法恰好保留一个有效且模式匹配的标记实体。
     * 以 PDC 标记为唯一真相，内存 UUID 表只用于 /csz item 的状态展示。
     */
    private void ensure() {
        if (!plugin.isDisplayEnabled()) {
            return;
        }
        ItemStack item = getDisplayItem();
        if (item == null || item.getType().isAir()) {
            return;
        }
        String mode = plugin.getDisplayMode();

        Collection<Portal> portals = plugin.getPortals().all();
        Set<String> validKeys = new HashSet<>();
        for (Portal portal : portals) {
            validKeys.add(keyOf(portal.getName()));
        }

        // 1. 扫描全服已加载实体，按阵法名分组；不属于任何现存阵法的先清掉
        Map<String, List<Entity>> found = new HashMap<>();
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : new ArrayList<>(world.getEntities())) {
                String name = entity.getPersistentDataContainer()
                        .get(marker, PersistentDataType.STRING);
                if (name == null) {
                    continue;
                }
                String key = keyOf(name);
                if (!validKeys.contains(key)) {
                    entity.remove();
                } else {
                    found.computeIfAbsent(key, k -> new ArrayList<>()).add(entity);
                }
            }
        }

        // 2. 每个阵法只留一个：优先有效、模式匹配、且是内存中记录过的实体
        for (Portal portal : portals) {
            String key = keyOf(portal.getName());
            List<Entity> entities = found.getOrDefault(key, Collections.emptyList());
            Entity keep = pickKeeper(entities, mode, spawned.get(key));
            for (Entity entity : entities) {
                if (entity != keep) {
                    entity.remove();
                }
            }
            if (keep != null) {
                spawned.put(key, keep.getUniqueId());
                // 掉落物被玩家挤走时纠正位置；仅在明显偏离时发包
                Location expected = expectedLocation(portal);
                if (expected != null && keep.getWorld().equals(expected.getWorld())
                        && keep.getLocation().distanceSquared(expected) > MAX_DRIFT * MAX_DRIFT) {
                    keep.teleport(expected);
                }
                continue;
            }
            spawned.remove(key);
            spawn(portal, item, mode);
        }
    }

    /** 从候选中挑一个保留：必须在场、未死、模式一致；优先内存里记录的那个 */
    private Entity pickKeeper(List<Entity> entities, String mode, UUID preferred) {
        Entity fallback = null;
        for (Entity entity : entities) {
            if (!entity.isValid() || entity.isDead()) {
                continue;
            }
            String entityMode = entity.getPersistentDataContainer()
                    .get(modeKey, PersistentDataType.STRING);
            if (!mode.equals(entityMode)) {
                continue; // 旧模式 / 无模式标记的遗留实体，交给调用方删掉
            }
            if (entity.getUniqueId().equals(preferred)) {
                return entity;
            }
            if (fallback == null) {
                fallback = entity;
            }
        }
        return fallback;
    }

    private Location expectedLocation(Portal portal) {
        Location center = portal.getCenter();
        if (center.getWorld() == null) {
            return null;
        }
        return center.clone().add(0, plugin.getDisplayHeight(), 0);
    }

    private String keyOf(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT);
    }

    private void spawn(Portal portal, ItemStack item, String mode) {
        Location loc = expectedLocation(portal);
        if (loc == null) {
            return; // 世界未加载，等下一次 ensure
        }
        World world = loc.getWorld();
        String key = keyOf(portal.getName());
        try {
            if ("ITEM".equals(mode)) {
                // 掉落物模式：Geyser 有原生 ItemEntity 映射，Java / 基岩双端都可见。
                // 不可拾取、不消失、无重力、不写存档，静态悬浮，不发任何粒子或音效包。
                Item dropped = world.dropItem(loc, item.clone(), i -> {
                    i.setPickupDelay(Integer.MAX_VALUE);
                    i.setCanPlayerPickup(false);
                    i.setCanMobPickup(false);
                    i.setUnlimitedLifetime(true);
                    i.setWillAge(false);
                    i.setGravity(false);
                    i.setInvulnerable(true);
                    i.setSilent(true);
                    i.setPersistent(false);
                    i.setCustomNameVisible(false);
                    i.getPersistentDataContainer().set(marker, PersistentDataType.STRING, portal.getName());
                    i.getPersistentDataContainer().set(modeKey, PersistentDataType.STRING, mode);
                });
                // spawn 后再钉死速度与重力，消除 dropItem 默认给的随机抛掷速度
                dropped.setGravity(false);
                dropped.setVelocity(new Vector(0, 0, 0));
                spawned.put(key, dropped.getUniqueId());
            } else if ("ARMOR_STAND".equals(mode)) {
                // 盔甲架模式：Geyser 全版本支持，基岩玩家也能看到头顶的特殊物品
                ArmorStand stand = world.spawn(loc, ArmorStand.class, a -> {
                    a.setInvisible(true);
                    a.setMarker(true);
                    a.setGravity(false);
                    a.setSmall(true);
                    a.setArms(false);
                    a.setBasePlate(false);
                    a.setCanPickupItems(false);
                    a.setPersistent(false);
                    a.setCustomNameVisible(false);
                    if (a.getEquipment() != null) {
                        a.getEquipment().setItem(EquipmentSlot.HEAD, item.clone());
                    }
                    a.getPersistentDataContainer().set(marker, PersistentDataType.STRING, portal.getName());
                    a.getPersistentDataContainer().set(modeKey, PersistentDataType.STRING, mode);
                });
                spawned.put(key, stand.getUniqueId());
            } else {
                // ITEM_DISPLAY（纯 Java 服）：最干净，无碰撞、不写存档、无周期发包。
                // Geyser 无此实体映射，互通服已由 SpacePortalPlugin#getDisplayMode() 自动降级为 ITEM
                ItemDisplay display = world.spawn(loc, ItemDisplay.class, d -> {
                    d.setItemStack(item.clone());
                    d.setBillboard(plugin.getDisplayBillboard());
                    d.setPersistent(false);
                    d.setCustomNameVisible(false);
                    d.getPersistentDataContainer().set(marker, PersistentDataType.STRING, portal.getName());
                    d.getPersistentDataContainer().set(modeKey, PersistentDataType.STRING, mode);
                });
                spawned.put(key, display.getUniqueId());
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("无法为传送阵 " + portal.getName()
                    + " 创建物品展示：" + ex.getMessage());
        }
    }

    /** 移除本插件创建的所有展示实体（含被推离阵法中心、超出搜索半径的历史遗留） */
    public void removeAll() {
        spawned.clear();
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : new ArrayList<>(world.getEntities())) {
                if (isMarker(entity)) {
                    entity.remove();
                }
            }
        }
    }

    /** 当前实际存在的标记实体数量，用于 /csz item 排错 */
    public int countMarkers() {
        int count = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (isMarker(entity)) {
                    count++;
                }
            }
        }
        return count;
    }

    /** 当前展示物品：优先管理员用 /csz item set 保存的自定义物品，否则用 config.yml 默认物品 */
    public ItemStack getDisplayItem() {
        ItemStack custom = plugin.getCustomDisplayItem();
        if (custom != null && !custom.getType().isAir()) {
            return custom.clone();
        }
        Material material = Material.matchMaterial(plugin.getDisplayMaterial());
        if (material == null || material.isAir()) {
            material = Material.ECHO_SHARD;
        }
        ItemStack item = new ItemStack(material);
        String name = plugin.getDisplayItemName();
        if (name != null && !name.isEmpty()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
                item.setItemMeta(meta);
            }
        }
        return item;
    }

    /**
     * 判断玩家主手是否持有「特殊物品」（use-as-trigger 开启时作为传送凭证）。
     * 先用 ItemStack#isSimilar 精确匹配（忽略数量），再用「材质 + 显示名」宽松兜底，
     * 兼容部分版本对数据组件序列化后精确比较失配的情况。
     */
    public boolean matchesHeld(Player player) {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || held.getType().isAir()) {
            return false;
        }
        ItemStack display = getDisplayItem();
        if (held.isSimilar(display)) {
            return true;
        }
        if (held.getType() != display.getType()) {
            return false;
        }
        String a = displayNameOf(held);
        String b = displayNameOf(display);
        return a != null && a.equals(b);
    }

    private String displayNameOf(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return null;
        }
        return meta.getDisplayName();
    }
}
