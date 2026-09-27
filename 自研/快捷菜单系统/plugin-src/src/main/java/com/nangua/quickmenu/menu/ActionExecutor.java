package com.nangua.quickmenu.menu;

import com.nangua.quickmenu.QuickMenuPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
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

            case ITEM_SELECTOR:
                if (!value.isEmpty()) {
                    // 打开背包物品选择器，点击物品后执行 value 中的命令模板
                    plugin.getMenuManager().openItemSelector(player, value);
                }
                break;

            case AH_SELL:
                // 格式："价格 材质名"，如 "5000 elytra"
                if (!value.isEmpty()) {
                    String[] parts = value.trim().split("\\s+", 2);
                    if (parts.length >= 2) {
                        String price = parts[0];
                        org.bukkit.Material material = org.bukkit.Material.matchMaterial(parts[1]);
                        if (material != null) {
                            sellItemFromInventory(player, price, material);
                        }
                    }
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
     * 拍卖行上架：从玩家背包（含快捷栏/盔甲槽/副手）查找指定材质物品，
     * 临时换到主手执行 {@code /ah sell 价格}，随后还原主手。
     *
     * <p>背景：菜单触发物品（时钟）占着主手，玩家直接 {@code /ah sell}
     * 会把时钟上架。此方法把目标物品换到主手执行完再换回，规避该问题。
     * 若背包里没有该物品则提示；上架失败（如价格低于下限）时物品归还原位。
     */
    private void sellItemFromInventory(Player player, String price, org.bukkit.Material material) {
        org.bukkit.inventory.PlayerInventory inv = player.getInventory();

        // 定位目标物品：优先存储格，其次盔甲槽（鞘翅常穿在身上），最后副手
        int foundStorage = -1;
        int foundArmor = -1;
        boolean foundOffhand = false;

        ItemStack[] storage = inv.getStorageContents();
        for (int i = 0; i < storage.length; i++) {
            if (storage[i] != null && storage[i].getType() == material) {
                foundStorage = i;
                break;
            }
        }
        if (foundStorage < 0) {
            ItemStack[] armor = inv.getArmorContents();
            for (int i = 0; i < armor.length; i++) {
                if (armor[i] != null && armor[i].getType() == material) {
                    foundArmor = i;
                    break;
                }
            }
            if (foundArmor < 0) {
                ItemStack offhand = inv.getItemInOffHand();
                if (offhand != null && offhand.getType() == material) {
                    foundOffhand = true;
                }
            }
        }
        if (foundStorage < 0 && foundArmor < 0 && !foundOffhand) {
            player.sendMessage(ActionExecutor.color("&c背包里没有 " + material.name() + "，无法上架。"));
            return;
        }

        // 目标物品换到主手，原主手暂存到空出来的位置
        ItemStack current = inv.getItemInMainHand();
        if (foundStorage >= 0) {
            inv.setItemInMainHand(storage[foundStorage]);
            inv.setItem(foundStorage, current);
        } else if (foundArmor >= 0) {
            inv.setItemInMainHand(inv.getArmorContents()[foundArmor]);
            ItemStack[] armor = inv.getArmorContents();
            armor[foundArmor] = current;
            inv.setArmorContents(armor);
        } else {
            inv.setItemInMainHand(inv.getItemInOffHand());
            inv.setItemInOffHand(current);
        }

        // 执行上架命令（AuctionHouse 读取主手物品上架）
        player.performCommand("ah sell " + price);

        // 还原：上架成功后主手被扣走（AIR 或剩余）；失败则物品还在主手，归还原位
        ItemStack after = inv.getItemInMainHand();
        if (after != null && after.getType() == material) {
            if (foundStorage >= 0) {
                inv.setItem(foundStorage, after);
            } else if (foundArmor >= 0) {
                ItemStack[] armor = inv.getArmorContents();
                armor[foundArmor] = after;
                inv.setArmorContents(armor);
            } else {
                inv.setItemInOffHand(after);
            }
        }
        inv.setItemInMainHand(current);
    }

    /**
     * 判断玩家是否有权使用某个菜单项。
     * 菜单项未配置 permission 时视为公开。
     */
    public boolean hasItemPermission(Player player, MenuItem item) {        String permission = item.getPermission();
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
