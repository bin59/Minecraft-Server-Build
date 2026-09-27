package com.nangua.quickmenu.menu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 一个菜单的完整定义，同时描述两端外观：
 * <ul>
 *   <li>Java 端 — 箱子界面（title / size / filler 物品 / 各 item 的 slot）</li>
 *   <li>基岩端 — 原生 SimpleForm（title / content / 按 items 顺序生成按钮）</li>
 * </ul>
 */
public final class Menu {

    private String id = "";
    private String title = "快捷菜单";
    private String bedrockContent = "";
    private int size = 27;

    /** 填充物品材质（箱子界面中未放置 item 的空位），设为 NONE 表示不填充 */
    private String fillerMaterial = "BLACK_STAINED_GLASS_PANE";
    private String fillerName = " ";

    /** 返回上一级菜单的 id，为空表示无返回按钮 */
    private String backMenu = "";

    private List<MenuItem> items = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBedrockContent() {
        return bedrockContent;
    }

    public void setBedrockContent(String bedrockContent) {
        this.bedrockContent = bedrockContent;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getFillerMaterial() {
        return fillerMaterial;
    }

    public void setFillerMaterial(String fillerMaterial) {
        this.fillerMaterial = fillerMaterial;
    }

    public String getFillerName() {
        return fillerName;
    }

    public void setFillerName(String fillerName) {
        this.fillerName = fillerName;
    }

    public String getBackMenu() {
        return backMenu;
    }

    public void setBackMenu(String backMenu) {
        this.backMenu = backMenu;
    }

    public List<MenuItem> getItems() {
        return items;
    }

    public void setItems(List<MenuItem> items) {
        this.items = items;
    }

    public List<MenuItem> itemsUnmodifiable() {
        return Collections.unmodifiableList(items);
    }
}
