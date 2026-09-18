package com.nangua.quickmenu.config;

import com.nangua.quickmenu.menu.Menu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 插件的运行时设置与菜单注册表。
 *
 * <p>所有字段均可通过 /qm reload 重新加载，无需重启服务器。
 */
public final class PluginSettings {

    // ===== 触发物品 =====
    private String triggerItemMaterial = "COMPASS";
    private String triggerItemName = "&6&l快捷菜单";
    private List<String> triggerItemLore = new ArrayList<>();
    private boolean triggerItemGlowing = true;

    /**
     * 进服时是否自动发放触发物品。
     * 基岩端建议设为 false —— 手机玩家在物品栏翻找比打中文指令更慢。
     */
    private boolean giveOnJoin = true;

    /** 发放时放入背包的第几个格子，-1 表示自动寻找空位 */
    private int giveSlot = 8;

    /**
     * 触发物品是否绑定 PersistentDataContainer 标记。
     * 开启后只有本插件发放的物品才能触发菜单，
     * 玩家自行合成的同名同材质物品不会误触发。
     */
    private boolean strictItemMatch = true;

    // ===== 界面外观 =====
    private String clickSound = "ui.button.click";
    private float clickSoundVolume = 0.6f;
    private float clickSoundPitch = 1.0f;
    private boolean debug = false;

    /** 默认菜单 id，玩家直接输入 /qm 时打开 */
    private String defaultMenu = "main";

    /** 基岩端是否优先使用原生 Form（false 则与 Java 一致走箱子界面） */
    private boolean bedrockUseNativeForm = true;

    /** Java 端是否禁用（例如只想让基岩玩家用 Form） */
    private boolean javaUseChestGui = true;

    // ===== 玩家选择器（tpa / tpahere 选人界面）=====
    private String playerSelectorTitle = "&6&l选择玩家";
    private String playerSelectorContent = "请选择要传送的玩家";
    private String playerSelectorLore = "&7点击向 TA 发送传送请求";
    private String playerSelectorNoPlayers = "&c当前没有其他在线玩家";

    // ===== 菜单注册表 =====
    private final List<Menu> menus = new ArrayList<>();

    public Menu findMenu(String id) {
        if (id == null) {
            return null;
        }
        for (Menu menu : menus) {
            if (menu.getId().equalsIgnoreCase(id)) {
                return menu;
            }
        }
        return null;
    }

    public List<Menu> getMenus() {
        return Collections.unmodifiableList(menus);
    }

    public void clearMenus() {
        menus.clear();
    }

    public void addMenu(Menu menu) {
        menus.add(menu);
    }

    // ===== getter / setter =====

    public String getTriggerItemMaterial() {
        return triggerItemMaterial;
    }

    public void setTriggerItemMaterial(String triggerItemMaterial) {
        this.triggerItemMaterial = triggerItemMaterial;
    }

    public String getTriggerItemName() {
        return triggerItemName;
    }

    public void setTriggerItemName(String triggerItemName) {
        this.triggerItemName = triggerItemName;
    }

    public List<String> getTriggerItemLore() {
        return triggerItemLore;
    }

    public void setTriggerItemLore(List<String> triggerItemLore) {
        this.triggerItemLore = triggerItemLore;
    }

    public boolean isTriggerItemGlowing() {
        return triggerItemGlowing;
    }

    public void setTriggerItemGlowing(boolean triggerItemGlowing) {
        this.triggerItemGlowing = triggerItemGlowing;
    }

    public boolean isGiveOnJoin() {
        return giveOnJoin;
    }

    public void setGiveOnJoin(boolean giveOnJoin) {
        this.giveOnJoin = giveOnJoin;
    }

    public int getGiveSlot() {
        return giveSlot;
    }

    public void setGiveSlot(int giveSlot) {
        this.giveSlot = giveSlot;
    }

    public boolean isStrictItemMatch() {
        return strictItemMatch;
    }

    public void setStrictItemMatch(boolean strictItemMatch) {
        this.strictItemMatch = strictItemMatch;
    }

    public String getClickSound() {
        return clickSound;
    }

    public void setClickSound(String clickSound) {
        this.clickSound = clickSound;
    }

    public float getClickSoundVolume() {
        return clickSoundVolume;
    }

    public void setClickSoundVolume(float clickSoundVolume) {
        this.clickSoundVolume = clickSoundVolume;
    }

    public float getClickSoundPitch() {
        return clickSoundPitch;
    }

    public void setClickSoundPitch(float clickSoundPitch) {
        this.clickSoundPitch = clickSoundPitch;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public String getDefaultMenu() {
        return defaultMenu;
    }

    public void setDefaultMenu(String defaultMenu) {
        this.defaultMenu = defaultMenu;
    }

    public boolean isBedrockUseNativeForm() {
        return bedrockUseNativeForm;
    }

    public void setBedrockUseNativeForm(boolean bedrockUseNativeForm) {
        this.bedrockUseNativeForm = bedrockUseNativeForm;
    }

    public boolean isJavaUseChestGui() {
        return javaUseChestGui;
    }

    public void setJavaUseChestGui(boolean javaUseChestGui) {
        this.javaUseChestGui = javaUseChestGui;
    }

    public String getPlayerSelectorTitle() {
        return playerSelectorTitle;
    }

    public void setPlayerSelectorTitle(String playerSelectorTitle) {
        this.playerSelectorTitle = playerSelectorTitle;
    }

    public String getPlayerSelectorContent() {
        return playerSelectorContent;
    }

    public void setPlayerSelectorContent(String playerSelectorContent) {
        this.playerSelectorContent = playerSelectorContent;
    }

    public String getPlayerSelectorLore() {
        return playerSelectorLore;
    }

    public void setPlayerSelectorLore(String playerSelectorLore) {
        this.playerSelectorLore = playerSelectorLore;
    }

    public String getPlayerSelectorNoPlayers() {
        return playerSelectorNoPlayers;
    }

    public void setPlayerSelectorNoPlayers(String playerSelectorNoPlayers) {
        this.playerSelectorNoPlayers = playerSelectorNoPlayers;
    }
}
