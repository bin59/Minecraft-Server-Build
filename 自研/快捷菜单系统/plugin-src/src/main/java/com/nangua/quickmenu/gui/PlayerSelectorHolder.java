package com.nangua.quickmenu.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * 在线玩家选择器界面的 InventoryHolder。
 *
 * <p>与 {@link MenuHolder} 分开：选择器界面不是配置化菜单，
 * 而是运行期动态构建（在线玩家列表每次打开都重新生成），
 * 因此不携带 {@code Menu} 对象，点击逻辑也走独立分支。
 */
public final class PlayerSelectorHolder implements InventoryHolder {

    private Inventory inventory;

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
