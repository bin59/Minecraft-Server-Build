package com.nangua.quickmenu;

import com.nangua.quickmenu.command.QuickMenuCommand;
import com.nangua.quickmenu.config.ConfigLoader;
import com.nangua.quickmenu.config.PluginSettings;
import com.nangua.quickmenu.listener.MenuClickListener;
import com.nangua.quickmenu.listener.TriggerItemListener;
import com.nangua.quickmenu.menu.ActionExecutor;
import com.nangua.quickmenu.menu.MenuManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * QuickMenu — 双轨快捷菜单插件。
 *
 * <p>设计目标：玩家手持触发物品右键，按客户端类型自动分发到最合适的界面。
 * <ul>
 *   <li><b>Java 玩家</b> — 箱子 GUI（物品图标，可自由排版，鼠标交互精准）</li>
 *   <li><b>基岩玩家</b> — 原生 SimpleForm（按钮列表，触屏友好，无 Geyser 箱子缺陷）</li>
 * </ul>
 *
 * <p><b>为什么基岩端不用箱子 GUI</b>：Geyser 对箱子界面存在多个未解决缺陷，
 * 包括物品可被拖出掉地（GeyserMC#211）、移动端高亮异常（#5896）、
 * 特定情况打不开界面（#6134）、基岩端只能左键交互。原生 Form 由基岩客户端
 * 自身渲染，点击与滚动行为完全正常，因此体验更优。
 *
 * <p><b>为什么不依赖 Skript / DeluxeMenus</b>：本插件直接用 Java 编写并编译为
 * 原生字节码，无解释执行开销；菜单解析结果缓存在内存中，打开界面时不做 IO。
 * Skript 为解释型，DeluxeMenus 需在运行时解析 YAML 表达式，二者性能均劣于此方案。
 */
public final class QuickMenuPlugin extends JavaPlugin {

    private PluginSettings settings;
    private ConfigLoader configLoader;
    private MenuManager menuManager;
    private ActionExecutor actionExecutor;

    /** Floodgate 是否可用，决定基岩端能否使用原生 Form */
    private boolean floodgateAvailable;

    @Override
    public void onEnable() {
        this.settings = new PluginSettings();
        this.configLoader = new ConfigLoader(this);
        this.actionExecutor = new ActionExecutor(this);
        this.menuManager = new MenuManager(this);

        detectFloodgate();
        loadConfiguration();

        getServer().getPluginManager().registerEvents(new TriggerItemListener(this), this);
        getServer().getPluginManager().registerEvents(new MenuClickListener(this), this);

        PluginCommand command = getCommand("qm");
        if (command != null) {
            command.setExecutor(new QuickMenuCommand(this));
        } else {
            getLogger().warning("未能注册 /qm 命令，请检查 plugin.yml");
        }

        getLogger().info("QuickMenu 已启用 — 已加载 " + settings.getMenus().size()
                + " 个菜单，Floodgate 原生表单：" + (floodgateAvailable ? "可用" : "不可用"));
    }

    @Override
    public void onDisable() {
        // 关闭所有打开中的菜单界面，避免服务器关闭时残留视图
        if (menuManager != null) {
            menuManager.closeAll();
        }
        getLogger().info("QuickMenu 已禁用");
    }

    /**
     * 检测 Floodgate 是否安装并可用。
     *
     * <p>未安装时插件仍正常工作，基岩玩家（实际不会出现）会回退到箱子界面。
     */
    private void detectFloodgate() {
        try {
            if (getServer().getPluginManager().getPlugin("floodgate") != null) {
                // 触发类加载以确认 API 真的存在
                Class.forName("org.geysermc.floodgate.api.FloodgateApi");
                floodgateAvailable = true;
            }
        } catch (ClassNotFoundException | NoClassDefFoundError ex) {
            floodgateAvailable = false;
            getLogger().info("未检测到 Floodgate API，基岩端原生表单功能已禁用");
        }
    }

    /** 重新加载配置文件，由 /qm reload 调用 */
    public void loadConfiguration() {
        saveDefaultConfig();
        reloadConfig();
        configLoader.loadAll(settings);
    }

    public boolean isFloodgateAvailable() {
        return floodgateAvailable;
    }

    public PluginSettings getSettings() {
        return settings;
    }

    public MenuManager getMenuManager() {
        return menuManager;
    }

    public ActionExecutor getActionExecutor() {
        return actionExecutor;
    }
}
