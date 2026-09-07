package com.nangua.quickmenu.listener;

import com.nangua.quickmenu.QuickMenuPlugin;
import com.nangua.quickmenu.menu.ActionExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * 触发物品监听器。
 *
 * <p>处理三件事：
 * <ol>
 *   <li>右键触发物品 → 打开菜单</li>
 *   <li>进服自动发放触发物品（可配置）</li>
 *   <li>阻止触发物品被丢弃（可配置，避免玩家误丢后无法打开菜单）</li>
 * </ol>
 *
 * <p><b>双手重复触发问题</b>：玩家主手与副手各持一个触发物品时，
 * Bukkit 会为同一次右键派发两次 PlayerInteractEvent。这里只处理
 * {@link EquipmentSlot#HAND}，避免菜单被打开两次。
 */
public final class TriggerItemListener implements Listener {

    private final QuickMenuPlugin plugin;

    public TriggerItemListener(QuickMenuPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();

        // 只响应右键（对方块或对空气），左键用于攻击/破坏不应触发菜单
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.RIGHT_CLICK_AIR) {
            return;
        }

        // 只处理主手，规避双手各持物品时的重复触发
        EquipmentSlot hand = event.getHand();
        if (hand != null && hand != EquipmentSlot.HAND) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null) {
            return;
        }
        if (!plugin.getMenuManager().getChestRenderer().isTriggerItem(item)) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.hasPermission("quickmenu.use")) {
            player.sendMessage(ActionExecutor.color(plugin.getConfig().getString(
                    "messages.no-permission", "&c你没有权限使用快捷菜单")));
            event.setCancelled(true);
            return;
        }

        // 取消默认行为：防止物品被"使用"（如食物被吃掉、方块被放置），
        // 也避免右键方块时同时触发方块交互
        event.setCancelled(true);

        plugin.getMenuManager().openDefaultMenu(player);

        if (plugin.getSettings().isDebug()) {
            plugin.getLogger().info("玩家 " + player.getName() + " 使用触发物品打开菜单");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.getSettings().isGiveOnJoin()) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.hasPermission("quickmenu.use")) {
            return;
        }

        // 延迟一拍发放：部分插件在 JOIN 事件中修改背包，
        // 延迟可避免物品被后续逻辑覆盖
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            giveTriggerItem(player);
        }, 5L);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (!plugin.getSettings().isStrictItemMatch()) {
            return;
        }
        ItemStack dropped = event.getItemDrop().getItemStack();
        if (plugin.getMenuManager().getChestRenderer().isTriggerItem(dropped)) {
            // 阻止丢弃，玩家仍可通过 /qm give 重新获取
            event.setCancelled(true);
            event.getPlayer().sendMessage(ActionExecutor.color(plugin.getConfig().getString(
                    "messages.item-drop-denied", "&c快捷菜单物品不能丢弃，使用 /qm 即可打开菜单")));
        }
    }

    /**
     * 发放触发物品到玩家背包。
     *
     * <p>已持有同款物品时不重复发放，避免背包塞满。
     */
    private void giveTriggerItem(Player player) {
        ItemStack triggerItem = plugin.getMenuManager().getChestRenderer().buildTriggerItem();
        PlayerInventory inventory = player.getInventory();

        // 检查是否已持有
        for (ItemStack existing : inventory.getContents()) {
            if (plugin.getMenuManager().getChestRenderer().isTriggerItem(existing)) {
                return;
            }
        }

        int slot = plugin.getSettings().getGiveSlot();
        if (slot >= 0 && slot < 36) {
            // 指定格子：仅在格子为空或已是触发物品时放入，避免覆盖玩家装备
            ItemStack current = inventory.getItem(slot);
            if (current == null || current.getType() == org.bukkit.Material.AIR) {
                inventory.setItem(slot, triggerItem);
            } else {
                inventory.addItem(triggerItem);
            }
        } else {
            inventory.addItem(triggerItem);
        }
    }
}
