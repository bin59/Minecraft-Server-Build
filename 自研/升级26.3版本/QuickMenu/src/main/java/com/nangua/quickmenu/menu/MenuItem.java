package com.nangua.quickmenu.menu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 单个菜单项。
 *
 * <p>同一个对象同时描述两端外观：
 * <ul>
 *   <li>Java 端 — 渲染为箱子界面中的一个物品（material / slot / lore / glowing）</li>
 *   <li>基岩端 — 渲染为原生 Form 上的一个按钮（buttonLabel）</li>
 * </ul>
 * 点击后执行的动作列表两端完全一致，因此行为不会出现分歧。
 */
public final class MenuItem {

    private final String id;
    private String material = "STONE";
    private String displayName = "";
    private List<String> lore = new ArrayList<>();
    private int slot = 0;
    private boolean glowing = false;
    private String permission = "";
    private List<Action> actions = new ArrayList<>();

    /**
     * 基岩端按钮文本。为空时回退使用 displayName（去除颜色码），
     * 因为原生 Form 按钮不支持 Minecraft 颜色码，只支持 §r 之外的有限样式。
     */
    private String buttonLabel = "";

    public MenuItem(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public List<String> getLore() {
        return lore;
    }

    public void setLore(List<String> lore) {
        this.lore = lore;
    }

    public int getSlot() {
        return slot;
    }

    public void setSlot(int slot) {
        this.slot = slot;
    }

    public boolean isGlowing() {
        return glowing;
    }

    public void setGlowing(boolean glowing) {
        this.glowing = glowing;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public List<Action> getActions() {
        return actions;
    }

    public void setActions(List<Action> actions) {
        this.actions = actions;
    }

    public String getButtonLabel() {
        return buttonLabel;
    }

    public void setButtonLabel(String buttonLabel) {
        this.buttonLabel = buttonLabel;
    }

    public List<Action> actionsUnmodifiable() {
        return Collections.unmodifiableList(actions);
    }
}
