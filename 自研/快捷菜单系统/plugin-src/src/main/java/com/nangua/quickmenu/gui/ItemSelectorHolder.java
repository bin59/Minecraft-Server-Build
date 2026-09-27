package com.nangua.quickmenu.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * 背包物品选择器界面的 InventoryHolder。
 *
 * <p>与 {@link MenuHolder} / {@link PlayerSelectorHolder} 分开：
 * 该界面不是配置化菜单，而是运行期根据玩家背包动态构建，
 * 因此不携带 {@code Menu} 对象，点击逻辑走独立分支。
 */
public final class ItemSelectorHolder implements InventoryHolder {

    private Inventory inventory;

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
