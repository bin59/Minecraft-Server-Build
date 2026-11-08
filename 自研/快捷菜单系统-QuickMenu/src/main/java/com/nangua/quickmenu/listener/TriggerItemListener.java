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

    /**
     * 右键触发物品 → 打开菜单。
     *
     * <p><b>优先级必须是最早（LOWEST）</b>：一旦识别出玩家右键的是触发物品，
     * 立刻取消右键事件并打开菜单，让其他插件（包括任何把右键绑定为传送、
     * 施法、使用等功能的其他插件）不再有机会处理这个右键事件。
     * 这保证「右键触发物品只打开菜单、绝不会触发别的动作」。
     *
     * <p><b>必须接收已取消事件（不能设置 ignoreCancelled=true）</b>：
     * Spigot 1.21.x 中，玩家「右键空气」时 PlayerInteractEvent 会以
     * <b>已取消状态</b>派发（原版对空气交互无后续行为，事件默认 cancelled）。
     * 若设置 ignoreCancelled=true，右键空气会被直接过滤，导致只有对着方块才能
     * 打开菜单；因此这里不设置 ignoreCancelled，用 LOWEST 保证最早收到并处理。
     */
    @EventHandler(priority = EventPriority.LOWEST)
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
     * <p>已持有同款（当前材质）触发物品时不重复发放（也不触发任何提示）；
     * 持有旧材质触发物品（如材质切换后的指南针）时自动升级移除并换发新材质；
     * 背包已满导致发放失败时，向玩家发送提示，引导使用 /qm 命令打开菜单。
     */
    private void giveTriggerItem(Player player) {
        com.nangua.quickmenu.gui.ChestMenuRenderer renderer =
                plugin.getMenuManager().getChestRenderer();

        // 材质升级：移除旧材质触发物品（如指南针→时钟），避免被误判为"已持有"
        boolean upgraded = renderer.upgradeLegacyTriggerItem(player);

        // 检查是否已持有当前材质的触发物品，已持有则直接返回，不再发放也不提示
        for (ItemStack existing : player.getInventory().getContents()) {
            if (renderer.isTriggerItem(existing)) {
                return;
            }
        }

        ItemStack triggerItem = renderer.buildTriggerItem();
        PlayerInventory inventory = player.getInventory();

        boolean placed = false;
        int slot = plugin.getSettings().getGiveSlot();
        if (slot >= 0 && slot < 36) {
            // 指定格子：仅在格子为空时放入，避免覆盖玩家装备
            ItemStack current = inventory.getItem(slot);
            if (current == null || current.getType() == org.bukkit.Material.AIR) {
                inventory.setItem(slot, triggerItem);
                placed = true;
            }
        }

        if (!placed) {
            // 指定格子被占或未配置：尝试自动找空位
            java.util.HashMap<Integer, ItemStack> overflow = inventory.addItem(triggerItem);
            if (!overflow.isEmpty()) {
                // 背包已满，物品发放失败，提示玩家改用 /qm 打开菜单
                player.sendMessage(ActionExecutor.color(plugin.getConfig().getString(
                        "messages.trigger-item-give-full",
                        "&c背包已满，无法获取\u201C快捷菜单\u201D时钟，请用 /qm 打开快捷菜单")));
                return;
            }
        }

        if (upgraded) {
            player.sendMessage(ActionExecutor.color(plugin.getConfig().getString(
                    "messages.trigger-item-upgraded",
                    "&a检测到旧版触发物品，已自动升级为\u201C快捷菜单\u201D时钟")));
        }
    }
}
