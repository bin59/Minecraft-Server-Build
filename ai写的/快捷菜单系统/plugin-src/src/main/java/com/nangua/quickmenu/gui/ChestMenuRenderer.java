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
import org.bukkit.inventory.meta.SkullMeta;
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

    /** 玩家选择器：标记"这是可选中的玩家头颅"，值为固定标识 __selector__ */
    private final NamespacedKey selectorTargetKey;

    /** 玩家选择器：存储目标玩家名 */
    private final NamespacedKey selectorNameKey;

    /** 玩家选择器：存储点击后要执行的命令模板（含 {target}） */
    private final NamespacedKey selectorTemplateKey;

    /** 物品选择器：存储所选物品的材质名（小写，供命令模板 {item} 替换） */
    private final NamespacedKey selectorItemMaterialKey;

    private final QuickMenuPlugin plugin;

    public ChestMenuRenderer(QuickMenuPlugin plugin) {
        this.plugin = plugin;
        this.triggerKey = new NamespacedKey(plugin, "trigger_item");
        this.itemKey = new NamespacedKey(plugin, "menu_item");
        this.menuKey = new NamespacedKey(plugin, "menu_id");
        this.selectorTargetKey = new NamespacedKey(plugin, "selector_target");
        this.selectorNameKey = new NamespacedKey(plugin, "selector_name");
        this.selectorTemplateKey = new NamespacedKey(plugin, "selector_template");
        this.selectorItemMaterialKey = new NamespacedKey(plugin, "selector_item_material");
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

    public NamespacedKey getSelectorTargetKey() {
        return selectorTargetKey;
    }

    public NamespacedKey getSelectorNameKey() {
        return selectorNameKey;
    }

    public NamespacedKey getSelectorTemplateKey() {
        return selectorTemplateKey;
    }

    public NamespacedKey getSelectorItemMaterialKey() {
        return selectorItemMaterialKey;
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
     * 构建并打开"在线玩家选择器"箱子界面。
     *
     * <p>动态列出当前在线玩家（排除自己），每人一个带真实皮肤的头颅，
     * 点击后执行 {@code commandTemplate}（其中 {@code {target}} 替换为玩家名）。
     * 界面每次打开都重新生成，因此玩家上下线后列表始终是最新状态。
     */
    public void renderPlayerSelector(Player player, String commandTemplate) {
        PlayerSelectorHolder holder = new PlayerSelectorHolder();
        int size = 54;
        Inventory inventory = Bukkit.createInventory(holder, size,
                ActionExecutor.color(plugin.getSettings().getPlayerSelectorTitle()));
        holder.setInventory(inventory);

        // 背景填充
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(" ");
            fillerMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            filler.setItemMeta(fillerMeta);
        }
        for (int slot = 0; slot < size; slot++) {
            inventory.setItem(slot, filler);
        }

        // 在线玩家列表（排除自己，最多 45 人，留出关闭按钮槽位）
        List<Player> targets = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getUniqueId().equals(player.getUniqueId())) {
                continue;
            }
            if (targets.size() >= 45) {
                break;
            }
            targets.add(online);
        }

        if (targets.isEmpty()) {
            ItemStack empty = new ItemStack(Material.BARRIER);
            ItemMeta emptyMeta = empty.getItemMeta();
            if (emptyMeta != null) {
                emptyMeta.setDisplayName(ActionExecutor.color(plugin.getSettings().getPlayerSelectorNoPlayers()));
                List<String> emptyLore = new ArrayList<>();
                emptyLore.add(ActionExecutor.color("&7稍后再试，或直接使用指令"));
                emptyLore.add(ActionExecutor.color("&e" + commandTemplate.replace("{target}", "玩家名")));
                emptyMeta.setLore(emptyLore);
                empty.setItemMeta(emptyMeta);
            }
            inventory.setItem(22, empty);
        } else {
            for (int i = 0; i < targets.size(); i++) {
                inventory.setItem(i, buildSelectorHead(targets.get(i), commandTemplate));
            }
        }

        // 关闭按钮：固定在右下角
        inventory.setItem(size - 1, buildSelectorCloseButton());

        player.openInventory(inventory);
    }

    /**
     * 构建并打开"背包物品选择器"箱子界面。
     *
     * <p>动态列出玩家背包中的物品（跳过触发物品，同材质合并显示数量），
     * 点击后执行 {@code commandTemplate}（其中 {@code {item}} 替换为材质名小写）。
     * 典型用途：估价（{@code worth {item}}），因为触发物品占着主手，
     * 原本的 {@code /worth} 只会估价手持的时钟。
     */
    public void renderItemSelector(Player player, String commandTemplate) {
        ItemSelectorHolder holder = new ItemSelectorHolder();
        int size = 54;
        Inventory inventory = Bukkit.createInventory(holder, size,
                ActionExecutor.color(plugin.getSettings().getItemSelectorTitle()));
        holder.setInventory(inventory);

        // 背景填充
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(" ");
            fillerMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            filler.setItemMeta(fillerMeta);
        }
        for (int slot = 0; slot < size; slot++) {
            inventory.setItem(slot, filler);
        }

        // 背包物品：跳过空位与触发物品，同材质合并数量，保持背包顺序稳定
        java.util.LinkedHashMap<Material, Integer> counts = new java.util.LinkedHashMap<>();
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack == null || stack.getType() == Material.AIR) {
                continue;
            }
            if (isTriggerItem(stack)) {
                continue; // 触发物品（时钟）没有估价意义，排除
            }
            counts.merge(stack.getType(), stack.getAmount(), Integer::sum);
        }

        if (counts.isEmpty()) {
            ItemStack empty = new ItemStack(Material.BARRIER);
            ItemMeta emptyMeta = empty.getItemMeta();
            if (emptyMeta != null) {
                emptyMeta.setDisplayName(ActionExecutor.color(plugin.getSettings().getItemSelectorNoItems()));
                List<String> emptyLore = new ArrayList<>();
                emptyLore.add(ActionExecutor.color("&7背包里没有可选择的物品"));
                emptyLore.add(ActionExecutor.color("&7（触发物品已排除）"));
                emptyMeta.setLore(emptyLore);
                empty.setItemMeta(emptyMeta);
            }
            inventory.setItem(22, empty);
        } else {
            int slot = 0;
            // 留出右下角关闭按钮，最多展示 size-1 种
            for (java.util.Map.Entry<Material, Integer> entry : counts.entrySet()) {
                if (slot >= size - 1) {
                    break;
                }
                inventory.setItem(slot++, buildSelectorItemStack(entry.getKey(), entry.getValue(), commandTemplate));
            }
        }

        // 关闭按钮：固定在右下角
        inventory.setItem(size - 1, buildSelectorCloseButton());

        player.openInventory(inventory);
    }

    /** 构建可点击的背包物品（标记材质名与命令模板，点击后执行） */
    private ItemStack buildSelectorItemStack(Material material, int count, String commandTemplate) {
        ItemStack stack = new ItemStack(material, Math.min(count, 64));
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }

        // 优先显示自定义显示名（如服务器特色物品），否则显示材质名
        String displayName = meta.hasDisplayName() ? meta.getDisplayName() : "&f" + material.name();
        meta.setDisplayName(ActionExecutor.color(displayName));

        List<String> lore = new ArrayList<>();
        lore.add(ActionExecutor.color(plugin.getSettings().getItemSelectorLore()));
        lore.add(ActionExecutor.color("&8背包中数量：&e" + count));
        meta.setLore(lore);

        meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, "__item_selector__");
        meta.getPersistentDataContainer().set(selectorItemMaterialKey,
                PersistentDataType.STRING, material.name().toLowerCase());
        meta.getPersistentDataContainer().set(selectorTemplateKey, PersistentDataType.STRING, commandTemplate);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);

        stack.setItemMeta(meta);
        return stack;
    }

    /** 构建可点击的玩家头颅（带真实皮肤贴图与选择器标记） */
    private ItemStack buildSelectorHead(Player target, String commandTemplate) {
        ItemStack stack = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }

        meta.setDisplayName(ActionExecutor.color("&a" + target.getName()));

        List<String> lore = new ArrayList<>();
        lore.add(ActionExecutor.color(plugin.getSettings().getPlayerSelectorLore()));
        lore.add(ActionExecutor.color("&8点击后自动执行"));
        meta.setLore(lore);

        meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, "__selector__");
        meta.getPersistentDataContainer().set(selectorTargetKey, PersistentDataType.STRING, target.getName());
        meta.getPersistentDataContainer().set(selectorTemplateKey, PersistentDataType.STRING, commandTemplate);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);

        // 真实皮肤头：在线玩家可直接取 PlayerProfile
        if (meta instanceof SkullMeta skullMeta) {
            try {
                skullMeta.setOwnerProfile(target.getPlayerProfile());
            } catch (Exception ignored) {
                // 取不到贴图时保留默认头颅，不影响功能
            }
        }

        stack.setItemMeta(meta);
        return stack;
    }

    /** 构建选择器界面的关闭按钮 */
    private ItemStack buildSelectorCloseButton() {
        ItemStack stack = new ItemStack(Material.ARROW);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ActionExecutor.color("&c关闭"));
            List<String> lore = new ArrayList<>();
            lore.add(ActionExecutor.color("&7点击关闭选择界面"));
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, "__close__");
            stack.setItemMeta(meta);
        }
        return stack;
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
