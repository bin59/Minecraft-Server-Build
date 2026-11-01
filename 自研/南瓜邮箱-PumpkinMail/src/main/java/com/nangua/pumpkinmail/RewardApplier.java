package com.nangua.pumpkinmail;

import com.nangua.pumpkinmail.storage.Reward;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.Map;

/**
 * 奖励发放：兼容 Java 与基岩玩家。
 *  - money   -> eco give <玩家> <金额>            （EssentialsX/Vault 加钱，即南瓜币）
 *  - item    -> 直接把物品放进玩家背包（API 方式，不再拼 give 命令）
 *  - command -> 自定义命令，{player} 占位符替换为玩家名
 *
 * <p><b>为什么 item 不再走 give 命令</b>：服务器 commands.yml 里把 {@code give} 别名强制指向
 * 原版 {@code minecraft:give}，而原版资源名只接受小写（a-z0-9_.-），像 {@code DIAMOND:64}
 * 这种大写写法会被解析成空的物品 path，抛 {@code Unknown item 'minecraft:'}。
 * 直接调 Inventory API 发放可以绕开命令别名、大小写、EssentialsX 覆盖、1.20.5+ 物品组件化
 * 这一整串坑，基岩端（Floodgate 前缀名）同样适用。
 *
 * <p><b>异常约定</b>：本类吞掉一切异常并返回值，绝不向外抛。否则上层的
 * {@code markClaimed()} 不会执行，奖励永远停在"未领取"，玩家可以反复点击重复领取。
 *
 * <p>发放前把奖励描述（desc，如「建筑大赛」第1名：500 南瓜币）写入南瓜账本的"来源"备注，
 * 这样玩家领取后，账本里这笔南瓜币的来源就是这条奖励描述（含活动名）。
 */
public class RewardApplier {

    private final Plugin plugin;

    public RewardApplier(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 发放一条奖励。
     *
     * @return 是否成功；失败时上层会提示玩家"领取失败，请联系管理员"并刷新界面。
     *         任何异常都会被捕获，不会向外抛出。
     */
    public boolean apply(Player player, Reward reward) {
        if (player == null || reward == null) {
            plugin.getLogger().warning("[PumpkinMail] 发放奖励失败：玩家或奖励为空");
            return false;
        }
        String type = reward.rewardType() == null ? "" : reward.rewardType().trim();
        setLedgerReason(reward.desc());
        try {
            return switch (type) {
                case "money" -> dispatch(player, "eco give " + player.getName() + " " + reward.data());
                case "item" -> giveItem(player, reward.data());
                case "command" ->
                        dispatch(player, String.valueOf(reward.data()).replace("{player}", player.getName()));
                default -> {
                    plugin.getLogger().warning("[PumpkinMail] 未知的奖励类型: " + type + " (id=" + reward.id() + ")");
                    yield false;
                }
            };
        } catch (Throwable t) {
            plugin.getLogger().severe("[PumpkinMail] 发放奖励异常: id=" + reward.id()
                    + " type=" + type + " data=" + reward.data() + " player=" + player.getName());
            t.printStackTrace();
            return false;
        } finally {
            clearLedgerReason();
        }
    }

    /** 以控制台身份执行命令；命令本身抛异常时返回 false 而不是冒泡。 */
    private boolean dispatch(Player player, String cmd) {
        if (cmd == null || cmd.isBlank()) return false;
        try {
            return plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), cmd);
        } catch (Throwable t) {
            plugin.getLogger().warning("[PumpkinMail] 奖励命令执行失败: /" + cmd
                    + " (player=" + player.getName() + ")");
            t.printStackTrace();
            return false;
        }
    }

    /** 物品奖励：直接塞背包，塞不下的掉在脚边，避免物品凭空消失。 */
    private boolean giveItem(Player player, String data) {
        ItemStack stack = parseItem(data);
        if (stack == null) {
            plugin.getLogger().warning("[PumpkinMail] 物品奖励解析失败: " + data
                    + " (player=" + player.getName() + ")");
            return false;
        }
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(stack);
        if (!leftover.isEmpty()) {
            int dropped = 0;
            for (ItemStack rest : leftover.values()) {
                if (rest == null || rest.getType().isAir()) continue;
                dropped += rest.getAmount();
                player.getWorld().dropItemNaturally(player.getLocation(), rest);
            }
            if (dropped > 0) {
                player.sendMessage(org.bukkit.ChatColor.YELLOW + "背包已满，多余的 " + dropped + " 个物品已掉落在你脚边");
            }
        }
        return true;
    }

    /**
     * 解析物品奖励数据，支持以下写法（大小写、命名空间均可）：
     * <ul>
     *   <li>{@code DIAMOND} / {@code diamond}</li>
     *   <li>{@code DIAMOND:64} / {@code golden_apple:2}</li>
     *   <li>{@code minecraft:diamond} / {@code minecraft:diamond:64}</li>
     * </ul>
     * 数量从最后一个冒号切分；材质名会自动剥离 {@code minecraft:} 命名空间前缀。
     *
     * @return 解析后的物品；无法识别时返回 null
     */
    public static ItemStack parseItem(String data) {
        String[] spec = splitMaterialAndAmount(data);
        if (spec == null) return null;
        Material m = Material.matchMaterial(spec[0]);
        if (m == null || m.isAir()) return null;
        int amt = Math.max(1, Math.min(Integer.parseInt(spec[1]), m.getMaxStackSize()));
        return new ItemStack(m, amt);
    }

    /**
     * 纯字符串拆分：把奖励数据切成 {@code [材质名, 数量]}，不含任何 Bukkit 依赖，便于单独验证。
     *
     * @return {@code [材质名, 数量字符串]}；数据为空时返回 null
     */
    public static String[] splitMaterialAndAmount(String data) {
        if (data == null || data.isBlank()) return null;
        String s = data.trim();

        // 数量：从最后一个冒号切（这样 minecraft:diamond:64 也能正确切成材质 + 数量）
        String amount = "1";
        int cut = s.lastIndexOf(':');
        if (cut > 0) {
            String tail = s.substring(cut + 1).trim();
            if (tail.matches("\\d+")) {
                amount = tail;
                s = s.substring(0, cut).trim();
            }
        }

        // 剥离命名空间前缀（Material.matchMaterial 会把 : 当成非法字符清掉，必须先去前缀）
        int ns = s.indexOf(':');
        if (ns >= 0) s = s.substring(ns + 1).trim();

        if (s.isEmpty()) return null;
        return new String[]{s, amount};
    }

    /** 反射调用 MoneyLedger.setReason（不存在账本插件则忽略）。 */
    private void setLedgerReason(String reason) {
        try {
            Class<?> c = Class.forName("com.nangua.moneyledger.MoneyLedger");
            c.getMethod("setReason", String.class).invoke(null, reason);
        } catch (Throwable ignored) { }
    }

    private void clearLedgerReason() {
        try {
            Class<?> c = Class.forName("com.nangua.moneyledger.MoneyLedger");
            c.getMethod("setReason", String.class).invoke(null, (String) null);
        } catch (Throwable ignored) { }
    }
}
