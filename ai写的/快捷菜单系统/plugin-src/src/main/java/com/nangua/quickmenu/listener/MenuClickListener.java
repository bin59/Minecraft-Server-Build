package com.nangua.quickmenu.listener;

import com.nangua.quickmenu.QuickMenuPlugin;
import com.nangua.quickmenu.gui.MenuHolder;
import com.nangua.quickmenu.menu.ActionExecutor;
import com.nangua.quickmenu.menu.Menu;
import com.nangua.quickmenu.menu.MenuItem;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 箱子菜单点击监听器。
 *
 * <p>核心安全点：
 * <ul>
 *   <li><b>无条件取消事件</b> —— 只要点击发生在本插件的界面内就 setCancelled(true)，
 *       防止玩家把菜单物品拖出界面、复制物品或塞入自己的物品。
 *       这正是 Geyser 箱子界面在基岩端暴露的问题（物品可被拖出掉地），
 *       此处一并堵住。</li>
 *   <li><b>同时处理拖拽事件</b> —— 基岩触屏玩家可能用拖拽方式移动物品，
 *       InventoryClickEvent 不覆盖该行为，需额外拦截。</li>
 *   <li><b>防连点</b> —— 同一玩家在配置间隔内重复点击同一槽位只生效一次，
 *       避免触屏误触或网络抖动导致命令被执行多次。</li>
 * </ul>
 */
public final class MenuClickListener implements Listener {

    private final QuickMenuPlugin plugin;

    /** 记录玩家上次点击：UUID → (槽位, 时间戳) */
    private final Map<UUID, long[]> lastClick = new HashMap<>();

    /** 防连点间隔（毫秒） */
    private static final long CLICK_INTERVAL_MS = 300L;

    /** 返回按钮的内部标识 */
    private static final String BACK_BUTTON_ID = "__back__";

    public MenuClickListener(QuickMenuPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onClick(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        InventoryHolder holder = topInventory.getHolder();

        // 不是本插件的界面，直接放行
        if (!(holder instanceof MenuHolder)) {
            return;
        }

        // 无论点击什么都取消：界面内不允许任何物品移动
        event.setCancelled(true);

        HumanEntity whoClicked = event.getWhoClicked();
        if (!(whoClicked instanceof Player)) {
            return;
        }
        Player player = (Player) whoClicked;

        if (!player.hasPermission("quickmenu.use")) {
            return;
        }

        // 只响应点击界面上半部分（菜单区域），玩家自己背包的点击已被取消但无需处理
        if (event.getClickedInventory() != topInventory) {
            return;
        }

        int slot = event.getSlot();
        MenuHolder menuHolder = (MenuHolder) holder;
        Menu menu = menuHolder.getMenu();

        // 防连点
        if (isSpamming(player.getUniqueId(), slot)) {
            return;
        }

        // 优先通过物品上的 PDC 标记识别，比按槽位反查更可靠
        ItemStack clickedItem = event.getCurrentItem();
        String itemId = readItemId(clickedItem);

        if (BACK_BUTTON_ID.equals(itemId)) {
            String backMenu = menu.getBackMenu();
            if (backMenu != null && !backMenu.isEmpty()) {
                plugin.getMenuManager().openMenu(player, backMenu);
            }
            return;
        }

        if (itemId == null || itemId.isEmpty()) {
            // 点到了填充物品或空格
            return;
        }

        MenuItem item = findItemById(menu, itemId);
        if (item == null) {
            return;
        }

        // 二次校验权限：防止配置热重载后权限发生变化而界面未刷新
        if (!plugin.getActionExecutor().hasItemPermission(player, item)) {
            player.sendMessage(ActionExecutor.color(plugin.getConfig().getString(
                    "messages.no-permission", "&c你没有权限使用快捷菜单")));
            return;
        }

        plugin.getActionExecutor().runAsyncSafe(player, item.getActions());
    }

    /**
     * 拦截拖拽行为。
     *
     * <p>触屏玩家（基岩端箱子界面回退时）与部分 Java 客户端可用拖拽把物品
     * 从菜单界面移到自己背包，InventoryClickEvent 不会触发，必须单独拦截。
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof MenuHolder) {
            event.setCancelled(true);
        }
    }

    private String readItemId(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) {
            return null;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return null;
        }
        NamespacedKey itemKey = plugin.getMenuManager().getChestRenderer().getItemKey();
        return meta.getPersistentDataContainer().get(itemKey, PersistentDataType.STRING);
    }

    private MenuItem findItemById(Menu menu, String itemId) {
        for (MenuItem item : menu.getItems()) {
            if (item.getId().equals(itemId)) {
                return item;
            }
        }
        return null;
    }

    /**
     * 判断是否为短时间内重复点击同一槽位。
     */
    private boolean isSpamming(UUID playerId, int slot) {
        long now = System.currentTimeMillis();
        long[] record = lastClick.get(playerId);
        if (record != null && record[0] == slot && (now - record[1]) < CLICK_INTERVAL_MS) {
            return true;
        }
        lastClick.put(playerId, new long[]{slot, now});
        return false;
    }

    /**
     * 玩家退出时立即移除其防连点记录。
     *
     * <p>这是必要的内存回收：{@code lastClick} 以玩家 UUID 为键，
     * 若不在退出时清理，Map 会随玩家进出持续增长。长期运行的服务器上
     * 这构成内存泄漏——每个曾打开过菜单的离线玩家都留下一条永久记录。
     *
     * <p>相比定时遍历全部在线玩家做清理，事件驱动的即时移除更精确：
     * 无需轮询、无遗漏、单次操作 O(1)。
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        lastClick.remove(event.getPlayer().getUniqueId());
    }
}
