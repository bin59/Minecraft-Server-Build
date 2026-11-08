package com.nangua.quickmenu.gui;

import com.nangua.quickmenu.menu.Menu;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * 箱子界面的持有者标记。
 *
 * <p>Bukkit 允许为界面绑定一个 {@link InventoryHolder}，点击事件中可通过
 * {@code inventory.getHolder()} 取回。用它来精确识别"这是本插件的菜单界面"，
 * 而不是靠比对界面标题字符串（标题含颜色码，易与其他插件冲突且不可靠）。
 *
 * <p>同时保存当前菜单对象，点击处理时直接读取内存中的动作列表，无需再次解析配置。
 */
public final class MenuHolder implements InventoryHolder {

    private final Menu menu;
    private Inventory inventory;

    public MenuHolder(Menu menu) {
        this.menu = menu;
    }

    public Menu getMenu() {
        return menu;
    }

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
