package com.nangua.quickmenu.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * "我的领地"选择器界面的 InventoryHolder。
 *
 * <p>与 {@link PlayerSelectorHolder} 分开：选择器界面不是配置化菜单，
 * 而是运行期动态构建（玩家名下领地列表每次打开都重新生成），
 * 因此不携带 {@code Menu} 对象，点击逻辑也走独立分支。
 *
 * <p>额外保存 {@code backMenu}：玩家点击"返回"后回到的菜单 id。
 */
public final class ResidenceSelectorHolder implements InventoryHolder {

    private Inventory inventory;
    private String backMenu = "";

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public String getBackMenu() {
        return backMenu;
    }

    public void setBackMenu(String backMenu) {
        this.backMenu = backMenu == null ? "" : backMenu;
    }
}
