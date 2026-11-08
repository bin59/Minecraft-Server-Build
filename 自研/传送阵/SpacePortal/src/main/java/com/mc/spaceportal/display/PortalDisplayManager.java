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
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * 传送阵的「特殊物品」展示管理。
 *
 * <p>关闭粒子特效后，地面法阵不再可见，本类在每个阵法中心上方放置一个
 * {@link ItemDisplay} 展示实体，用来标记阵法位置。展示实体具有以下特性：</p>
 * <ul>
 *   <li>无碰撞、不可拾取、不可交互，不影响玩家走动与建阵；</li>
 *   <li>{@code setPersistent(false)}，不写入存档，服务器重启后不会残留；</li>
 *   <li>带 PDC 标记，重载/删除阵法时可精确清理，不会误伤其他实体；</li>
 *   <li>静态（无动画、无粒子、无音效），完全不产生周期性网络包。</li>
 * </ul>
 *
 * <p>展示物品可由管理员手持任意物品（含带数据组件的自定义物品）执行
 * {@code /csz item set} 保存，序列化写入 override.yml，重启后仍生效。</p>
 */
public final class PortalDisplayManager {

    /** 每 100 tick（5 秒）做一次幂等补齐，不发送任何粒子或音效 */
    private static final long ENSURE_PERIOD = 100L;

    private final SpacePortalPlugin plugin;
    private final NamespacedKey marker;
    /** 传送阵名（小写） -> 展示实体 UUID */
    private final Map<String, UUID> spawned = new HashMap<>();
    private BukkitTask task;

    public PortalDisplayManager(SpacePortalPlugin plugin) {
        this.plugin = plugin;
        this.marker = new NamespacedKey(plugin, "portal_display");
    }

    /** 启动或重启：全量重建一次，之后低频幂等补齐 */
    public void start() {
        restart();
    }

    /** 配置 / 展示物品变更后调用，立即全量重建 */
    public void refresh() {
        restart();
    }

    /** 关闭展示：取消任务并移除全部实体 */
    public void stop() {
        cancelTask();
        removeAll();
    }

    private void restart() {
        cancelTask();
        removeAll();
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

    /** 只为缺失的阵法补建展示实体，已有实体不动，避免反复闪烁 */
    private void ensure() {
        if (!plugin.isDisplayEnabled()) {
            return;
        }
        ItemStack item = getDisplayItem();
        if (item == null || item.getType().isAir()) {
            return;
        }
        for (Portal portal : plugin.getPortals().all()) {
            String key = portal.getName().toLowerCase(Locale.ROOT);
            UUID uuid = spawned.get(key);
            Entity entity = uuid == null ? null : Bukkit.getEntity(uuid);
            if (entity != null && entity.isValid() && !entity.isDead()) {
                continue;
            }
            spawned.remove(key);
            spawn(portal, item);
        }
    }

    private void spawn(Portal portal, ItemStack item) {
        Location center = portal.getCenter();
        World world = center.getWorld();
        if (world == null) {
            return; // 世界未加载，等下一次 ensure
        }
        Location loc = center.clone().add(0, plugin.getDisplayHeight(), 0);
        String key = portal.getName().toLowerCase(Locale.ROOT);
        boolean armorStand = "ARMOR_STAND".equals(plugin.getDisplayMode());
        try {
            if (armorStand) {
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
                });
                spawned.put(key, stand.getUniqueId());
            } else {
                // 默认：Item Display（Java 端最干净，无碰撞、不写存档、无周期发包）
                ItemDisplay display = world.spawn(loc, ItemDisplay.class, d -> {
                    d.setItemStack(item.clone());
                    d.setBillboard(plugin.getDisplayBillboard());
                    d.setPersistent(false);
                    d.setCustomNameVisible(false);
                    d.getPersistentDataContainer().set(marker, PersistentDataType.STRING, portal.getName());
                });
                spawned.put(key, display.getUniqueId());
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("无法为传送阵 " + portal.getName()
                    + " 创建物品展示：" + ex.getMessage());
        }
    }

    /** 移除本插件创建的所有展示实体，并顺带清理带标记的历史遗留实体 */
    public void removeAll() {
        for (UUID uuid : spawned.values()) {
            Entity entity = Bukkit.getEntity(uuid);
            if (entity != null) {
                entity.remove();
            }
        }
        spawned.clear();
        for (Portal portal : plugin.getPortals().all()) {
            Location center = portal.getCenter();
            World world = center.getWorld();
            if (world == null) {
                continue;
            }
            for (Entity entity : world.getNearbyEntities(center, 3, 4, 3)) {
                if (entity.getPersistentDataContainer().has(marker, PersistentDataType.STRING)) {
                    entity.remove();
                }
            }
        }
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
