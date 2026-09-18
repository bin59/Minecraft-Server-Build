package com.nangua.quickmenu.menu;

import com.nangua.quickmenu.QuickMenuPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

/**
 * 动作执行器。Java 箱子界面与基岩原生表单共用此类，保证两端行为一致。
 *
 * <p><b>线程安全</b>：Cumulus 的表单响应回调在 Netty 网络线程上触发，
 * 而 Bukkit 的所有玩家 / 世界操作必须在主线程执行。因此本类的所有
 * 对外入口都会先调度到主线程，调用方无需自行处理。
 */
public final class ActionExecutor {

    private final QuickMenuPlugin plugin;

    public ActionExecutor(QuickMenuPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 在任意线程安全地执行一组动作。
     *
     * @param player  目标玩家
     * @param actions 动作列表
     */
    public void runAsyncSafe(Player player, List<Action> actions) {
        if (player == null || actions == null || actions.isEmpty()) {
            return;
        }
        if (Bukkit.isPrimaryThread()) {
            runOnMain(player, actions);
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> runOnMain(player, actions));
        }
    }

    private void runOnMain(Player player, List<Action> actions) {
        if (!player.isOnline()) {
            return;
        }
        for (Action action : actions) {
            execute(player, action);
        }
        playClickSound(player);
    }

    private void execute(Player player, Action action) {
        ActionType type = action.getType();
        String value = action.getValue();

        // 平台条件动作：先判断客户端类型，不匹配则整条跳过。
        // 判断只在需要时做一次并缓存到本次调用，避免重复查询 Floodgate。
        if (type.isPlatformSpecific()) {
            boolean bedrock = plugin.getMenuManager().isBedrockPlayer(player);
            if (type.isBedrockOnly() && !bedrock) {
                logSkipped(player, action, "Java");
                return;
            }
            if (type.isJavaOnly() && bedrock) {
                logSkipped(player, action, "基岩");
                return;
            }
        }

        switch (type) {
            case PLAYER_COMMAND:
            case BEDROCK_PLAYER_COMMAND:
            case JAVA_PLAYER_COMMAND:
                if (!value.isEmpty()) {
                    // 以玩家身份执行，受权限系统约束
                    player.performCommand(value);
                }
                break;

            case CONSOLE_COMMAND:
            case BEDROCK_CONSOLE_COMMAND:
            case JAVA_CONSOLE_COMMAND:
                if (!value.isEmpty()) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), value);
                }
                break;

            case OPEN_MENU:
                if (!value.isEmpty()) {
                    plugin.getMenuManager().openMenu(player, value);
                }
                break;

            case PLAYER_SELECTOR:
                if (!value.isEmpty()) {
                    // 打开在线玩家选择器，点击玩家后执行 value 中的命令模板
                    plugin.getMenuManager().openPlayerSelector(player, value);
                }
                break;

            case MESSAGE:
                if (!value.isEmpty()) {
                    player.sendMessage(value);
                }
                break;

            case CLOSE:
                player.closeInventory();
                break;

            default:
                break;
        }
    }

    /**
     * 记录因平台不匹配而跳过的动作（仅调试模式）。
     *
     * <p>这个日志对排查「菜单点了没反应」非常有用：
     * 通常是把基岩专属指令配成了无条件动作，Java 玩家执行时指令不存在。
     */
    private void logSkipped(Player player, Action action, String actualPlatform) {
        if (plugin.getSettings().isDebug()) {
            plugin.getLogger().info("跳过平台条件动作 " + action
                    + " — 玩家 " + player.getName() + " 属于 " + actualPlatform + " 端");
        }
    }

    /**
     * 判断玩家是否有权使用某个菜单项。
     * 菜单项未配置 permission 时视为公开。
     */
    public boolean hasItemPermission(Player player, MenuItem item) {
        String permission = item.getPermission();
        if (permission == null || permission.isEmpty()) {
            return true;
        }
        return player.hasPermission(permission);
    }

    /**
     * 播放点击音效。使用字符串形式的音效名而非 Sound 枚举，
     * 这样跨 Minecraft 版本升级时不会因枚举常量变动而失效。
     */
    private void playClickSound(Player player) {
        String sound = plugin.getSettings().getClickSound();
        if (sound == null || sound.isEmpty()) {
            return;
        }
        float volume = plugin.getSettings().getClickSoundVolume();
        float pitch = plugin.getSettings().getClickSoundPitch();
        try {
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException ex) {
            // 音效名无效时静默忽略，避免刷屏日志
            if (plugin.getSettings().isDebug()) {
                plugin.getLogger().warning("无效的点击音效名: " + sound);
            }
        }
    }

    /**
     * 将带 & 颜色码的字符串转换为 Minecraft 内部使用的 § 形式。
     */
    public static String color(String input) {
        if (input == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    /**
     * 去除所有颜色码，用于基岩端按钮文本
     * （原生 Form 按钮不支持 Minecraft 颜色码，会显示为乱码字符）。
     */
    public static String stripColor(String input) {
        if (input == null) {
            return "";
        }
        String translated = ChatColor.translateAlternateColorCodes('&', input);
        return ChatColor.stripColor(translated);
    }

    /** 保留引用，避免未使用警告（供未来扩展音效分类使用） */
    @SuppressWarnings("unused")
    private Plugin pluginRef() {
        return plugin;
    }
}
