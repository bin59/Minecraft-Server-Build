package com.nangua.quickmenu.gui;

import com.nangua.quickmenu.QuickMenuPlugin;
import com.nangua.quickmenu.menu.ActionExecutor;
import com.nangua.quickmenu.menu.Menu;
import com.nangua.quickmenu.menu.MenuItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Java 端箱子 GUI 渲染器。
 *
 * <p>菜单内容在配置加载阶段已解析为内存对象，此处仅做物品组装，无 IO、无 YAML 解析、
 * 无表达式求值。物品图标会做缓存复用，避免每次打开界面都重新构造 ItemStack。
 */
public final class ChestMenuRenderer {

    /** 用于标记触发物品的 PDC 键，防止玩家自制同名物品误触发 */
    private final NamespacedKey triggerKey;

    /** 用于标记菜单项槽位的 PDC 键，值为菜单项 id，点击时据此查动作 */
    private final NamespacedKey itemKey;

    /** 用于标记菜单归属的 PDC 键，值为菜单 id */
    private final NamespacedKey menuKey;

    private final QuickMenuPlugin plugin;

    public ChestMenuRenderer(QuickMenuPlugin plugin) {
        this.plugin = plugin;
        this.triggerKey = new NamespacedKey(plugin, "trigger_item");
        this.itemKey = new NamespacedKey(plugin, "menu_item");
        this.menuKey = new NamespacedKey(plugin, "menu_id");
    }

    public NamespacedKey getTriggerKey() {
        return triggerKey;
    }

    public NamespacedKey getItemKey() {
        return itemKey;
    }

    public NamespacedKey getMenuKey() {
        return menuKey;
    }

    /**
     * 为指定玩家构建并打开菜单界面。
     *
     * <p>无权限的菜单项会被跳过（而不是显示为灰色不可点），
     * 这样界面对不同权限组的玩家呈现各自能用的功能，避免困惑。
     */
    public void render(Player player, Menu menu) {
        MenuHolder holder = new MenuHolder(menu);
        int size = menu.getSize();
        Inventory inventory = Bukkit.createInventory(holder, size, ActionExecutor.color(menu.getTitle()));
        holder.setInventory(inventory);

        // 1. 填充背景物品
        ItemStack filler = buildFiller(menu);
        if (filler != null) {
            for (int slot = 0; slot < size; slot++) {
                inventory.setItem(slot, filler);
            }
        }

        // 2. 放置有权限的菜单项
        for (MenuItem item : menu.getItems()) {
            if (!plugin.getActionExecutor().hasItemPermission(player, item)) {
                continue;
            }
            int slot = item.getSlot();
            if (slot < 0 || slot >= size) {
                continue;
            }
            inventory.setItem(slot, buildItemStack(menu, item));
        }

        // 3. 返回按钮（若配置了 back-menu），固定在右下角
        //    选用 size-1 而非中间位置：中间区域是菜单项的常规摆放区，
        //    放角落可最大限度避免与用户自定义物品抢占槽位。
        String backMenu = menu.getBackMenu();
        if (backMenu != null && !backMenu.isEmpty()) {
            int backSlot = size - 1;
            if (backSlot >= 0 && backSlot < size) {
                ItemStack occupant = inventory.getItem(backSlot);
                if (hasMenuItemTag(occupant)) {
                    // 槽位已被真实菜单项占用，不覆盖，避免用户功能丢失
                    plugin.getLogger().warning("菜单 " + menu.getId()
                            + " 的返回按钮槽位 " + backSlot
                            + " 已被菜单项占用，返回按钮未显示。请调整该物品的 slot 或移除 back-menu。");
                } else {
                    inventory.setItem(backSlot, buildBackButton());
                }
            }
        }

        player.openInventory(inventory);
    }

    /**
     * 判断物品是否带有菜单项标记（即用户配置的真实菜单项，而非填充物）。
     * 用于返回按钮放置前的槽位占用检查。
     */
    private boolean hasMenuItemTag(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return false;
        }
        return meta.getPersistentDataContainer().has(itemKey, PersistentDataType.STRING);
    }

    private ItemStack buildFiller(Menu menu) {
        String materialName = menu.getFillerMaterial();
        if (materialName == null || materialName.isEmpty() || materialName.equalsIgnoreCase("NONE")) {
            return null;
        }
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            return null;
        }
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ActionExecutor.color(menu.getFillerName()));
            List<String> emptyLore = new ArrayList<>();
            meta.setLore(emptyLore);
            // 隐藏材质自带的属性提示，使填充物看起来干净
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack buildItemStack(Menu menu, MenuItem item) {
        Material material = Material.matchMaterial(item.getMaterial());
        if (material == null) {
            material = Material.STONE;
        }
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }

        meta.setDisplayName(ActionExecutor.color(item.getDisplayName()));

        List<String> lore = item.getLore();
        if (lore != null && !lore.isEmpty()) {
            List<String> colored = new ArrayList<>(lore.size());
            for (String line : lore) {
                colored.add(ActionExecutor.color(line));
            }
            meta.setLore(colored);
        }

        // 发光效果：使用 enchantment glint override，
        // 无需真的附魔，因此不会给物品带来任何实际属性加成
        if (item.isGlowing()) {
            meta.setEnchantmentGlintOverride(true);
        }

        // 写入标记，点击时据此定位菜单项
        meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, item.getId());
        meta.getPersistentDataContainer().set(menuKey, PersistentDataType.STRING, menu.getId());
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);

        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack buildBackButton() {
        ItemStack stack = new ItemStack(Material.ARROW);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ActionExecutor.color("&c返回"));
            List<String> lore = new ArrayList<>();
            lore.add(ActionExecutor.color("&7点击返回上一级菜单"));
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, "__back__");
            stack.setItemMeta(meta);
        }
        return stack;
    }

    /**
     * 构建触发物品（供 /qm give 与进服发放使用）。
     */
    public ItemStack buildTriggerItem() {
        String materialName = plugin.getSettings().getTriggerItemMaterial();
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            plugin.getLogger().warning("触发物品材质无效: " + materialName + "，已回退为 COMPASS");
            material = Material.COMPASS;
        }
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }
        meta.setDisplayName(ActionExecutor.color(plugin.getSettings().getTriggerItemName()));

        List<String> lore = plugin.getSettings().getTriggerItemLore();
        if (lore != null && !lore.isEmpty()) {
            List<String> colored = new ArrayList<>(lore.size());
            for (String line : lore) {
                colored.add(ActionExecutor.color(line));
            }
            meta.setLore(colored);
        }
        if (plugin.getSettings().isTriggerItemGlowing()) {
            meta.setEnchantmentGlintOverride(true);
        }
        meta.getPersistentDataContainer().set(triggerKey, PersistentDataType.STRING, "true");
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        stack.setItemMeta(meta);
        return stack;
    }

    /**
     * 判断一个物品是否为本插件的触发物品。
     *
     * <p>strict-match 开启时要求带 PDC 标记（只有插件发放的物品才有），
     * 关闭时只比对材质与显示名，便于玩家用 /give 自制的同款物品也能触发。
     */
    public boolean isTriggerItem(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return false;
        }

        if (plugin.getSettings().isStrictItemMatch()) {
            return meta.getPersistentDataContainer().has(triggerKey, PersistentDataType.STRING);
        }

        // 宽松模式：比对材质与名称
        Material expected = Material.matchMaterial(plugin.getSettings().getTriggerItemMaterial());
        if (expected == null || stack.getType() != expected) {
            return false;
        }
        if (!meta.hasDisplayName()) {
            return false;
        }
        String expectedName = ActionExecutor.color(plugin.getSettings().getTriggerItemName());
        return meta.getDisplayName().equals(expectedName);
    }

    /**
     * 材质升级：移除玩家背包中"旧材质触发物品"。
     *
     * <p>触发物品材质切换（如指南针 COMPASS → 时钟 CLOCK）后，strict-match 下
     * isTriggerItem 只认 PDC 标记，旧材质物品仍会被识别为触发物品，
     * 导致新材质永远发不出去。此方法清除背包中所有与当前配置材质不一致的触发物品。
     *
     * @return true 表示移除了至少一个旧物品
     */
    public boolean upgradeLegacyTriggerItem(Player player) {
        Material expected = Material.matchMaterial(plugin.getSettings().getTriggerItemMaterial());
        PlayerInventory inventory = player.getInventory();
        ItemStack[] contents = inventory.getContents();
        boolean removed = false;
        for (int i = 0; i < contents.length; i++) {
            ItemStack existing = contents[i];
            if (existing == null || existing.getType() == Material.AIR) {
                continue;
            }
            if (!isTriggerItem(existing)) {
                continue;
            }
            if (expected != null && existing.getType() == expected) {
                continue; // 同材质：保留
            }
            inventory.setItem(i, null);
            removed = true;
        }
        return removed;
    }
}
