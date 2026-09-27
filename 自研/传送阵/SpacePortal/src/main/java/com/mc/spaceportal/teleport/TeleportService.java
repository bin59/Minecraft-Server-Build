package com.mc.spaceportal.teleport;

import com.mc.spaceportal.SpacePortalPlugin;
import com.mc.spaceportal.effect.EffectController;
import com.mc.spaceportal.portal.Portal;
import com.mc.spaceportal.portal.PortalSession;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 传送服务：充能会话管理、钻石费用结算、传送执行、冷却、时光穿梭特效。
 */
public class TeleportService {

    /** 时光穿梭隧道每帧之间的 tick 数（越大越省，总时长由 tunnel-ticks 控制） */
    private static final int TUNNEL_FRAME_TICKS = 2;

    private final SpacePortalPlugin plugin;
    private final Map<UUID, PortalSession> sessions = new HashMap<>();
    private final Map<UUID, BukkitTask> sessionTasks = new HashMap<>();
    private final Map<UUID, Long> cooldownUntil = new HashMap<>();

    public TeleportService(SpacePortalPlugin plugin) {
        this.plugin = plugin;
    }

    /** 玩家进入阵法范围时调用，尝试开始充能 */
    public void tryStartCharge(Player player, Portal portal) {
        UUID id = player.getUniqueId();
        if (sessions.containsKey(id)) {
            return;
        }
        if (!player.hasPermission("spaceportal.use")) {
            player.sendMessage(msg("&c你没有使用传送阵的权限"));
            return;
        }
        Long until = cooldownUntil.get(id);
        if (until != null && System.currentTimeMillis() < until) {
            return; // 冷却中，静默忽略（避免移动事件刷屏）
        }
        Portal dest = plugin.getPortals().get(portal.getDestName());
        if (dest == null) {
            player.sendMessage(msg("&c传送阵 &f" + portal.getName() + " &c尚未连接目的地"));
            return;
        }
        int cost = plugin.getCost();
        boolean free = isFree(player);
        if (!free && countDiamonds(player) < cost) {
            player.sendMessage(msg("&c钻石不足！传送需要 &b" + cost + " 颗钻石"));
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        PortalSession session = new PortalSession(portal.getName(), plugin.getChargeTicks());
        sessions.put(id, session);
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin,
                () -> tickSession(player, portal, session), 1L, 1L);
        sessionTasks.put(id, task);

        player.sendMessage(msg("&b空间阵法开始充能，请站在阵法中央不要移动…"));
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.7f);
    }

    /** 该玩家传送是否免费（免费权限，或创造/旁观且开启 free-in-creative） */
    public boolean isFree(Player player) {
        if (player.hasPermission("spaceportal.free")) {
            return true;
        }
        if (plugin.isFreeInCreative()) {
            GameMode gm = player.getGameMode();
            return gm == GameMode.CREATIVE || gm == GameMode.SPECTATOR;
        }
        return false;
    }

    private void tickSession(Player player, Portal portal, PortalSession session) {
        UUID id = player.getUniqueId();
        // 玩家离线 / 死亡 / 离开范围 → 取消
        if (!player.isOnline() || player.isDead()) {
            cancelCharge(player, false);
            return;
        }
        Location center = portal.getCenter();
        if (center.getWorld() == null
                || !EffectController.inRange(player.getLocation(), center, plugin.getActivateRadius(), 2.5)) {
            cancelCharge(player, true);
            return;
        }

        session.tick++;

        // 每 10 tick 刷新一次动作栏进度 + 充能音效音调渐升
        if (session.tick % 10 == 0) {
            int percent = (int) (session.progress() * 100);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    TextComponent.fromLegacyText(ChatColor.AQUA + "⟡ 空间充能 " + percent + "% ⟡"));
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_AMBIENT,
                    0.6f, 0.8f + (float) session.progress() * 0.8f);
        }

        if (session.tick >= session.total) {
            sessions.remove(id);
            BukkitTask task = sessionTasks.remove(id);
            if (task != null) {
                task.cancel();
            }
            executeTeleport(player, portal);
        }
    }

    /** 取消充能 */
    public void cancelCharge(Player player, boolean notify) {
        UUID id = player.getUniqueId();
        PortalSession session = sessions.remove(id);
        BukkitTask task = sessionTasks.remove(id);
        if (task != null) {
            task.cancel();
        }
        if (session == null) {
            return;
        }
        Portal portal = plugin.getPortals().get(session.portalName);
        if (portal != null) {
            EffectController.cancelPuff(portal.getCenter(), portal.getHue());
        }
        if (notify) {
            player.sendMessage(msg("&7移动离开了阵法，充能已取消"));
        }
    }

    private void executeTeleport(Player player, Portal from) {
        Portal dest = plugin.getPortals().get(from.getDestName());
        if (dest == null) {
            player.sendMessage(msg("&c目的地已被删除，传送失败"));
            return;
        }
        Location fromCenter = from.getCenter();
        Location toCenter = dest.getCenter();
        if (toCenter.getWorld() == null) {
            player.sendMessage(msg("&c目的地所在世界未加载，传送失败"));
            return;
        }

        // 充能完成特效
        EffectController.chargeComplete(player, fromCenter, from.getHue());

        // 结算费用（传送前再次确认，走通用扣费）
        if (!chargeFee(player)) {
            return;
        }

        // 短暂停顿后正式传送（营造蓄力释放感）
        Location destLoc = toCenter.clone().add(0, 1, 0);
        destLoc.setYaw(player.getLocation().getYaw());
        destLoc.setPitch(player.getLocation().getPitch());
        float hue = from.getHue();

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline() || player.isDead()) {
                return;
            }
            EffectController.teleportBurst(fromCenter, destLoc, hue);
            player.teleport(destLoc);
            applyCooldown(player);
            player.sendMessage(msg("&a✦ 空间跳跃完成，已抵达 &f" + dest.getName()));
            startTimeTunnel(player, hue);
        }, 6L);
    }

    /** 传送落地后的时光穿梭特效 */
    private void startTimeTunnel(Player player, float hue) {
        // 隧道特效每 2 tick 渲染一帧（总时长不变，调用次数减半）
        int frames = Math.max(1, plugin.getTunnelTicks() / TUNNEL_FRAME_TICKS);
        final int[] age = {0};
        BukkitTask[] holder = new BukkitTask[1];
        holder[0] = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            boolean done = EffectController.drawTimeTunnel(player, age[0], frames);
            age[0]++;
            if (done) {
                holder[0].cancel();
            }
        }, 0L, (long) TUNNEL_FRAME_TICKS);
    }

    /**
     * 通用扣费：按当前配置费用扣除钻石，供充能传送与界面传送共用。
     * 免费权限或创造/旁观免费时直接放行。
     *
     * @return true 表示扣费成功或本就免费，可以继续传送；false 表示钻石不足，已提示玩家
     */
    public boolean chargeFee(Player player) {
        if (isFree(player)) {
            return true;
        }
        int cost = plugin.getCost();
        if (cost <= 0) {
            return true;
        }
        if (countDiamonds(player) < cost) {
            player.sendMessage(msg("&c钻石不足！传送需要 &b" + cost + " 颗钻石"));
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return false;
        }
        removeDiamonds(player, cost);
        player.sendMessage(msg("&b已支付 &f" + cost + " 颗钻石"));
        return true;
    }

    /** 传送后设置冷却，防止落地在阵法中心时被立刻重新触发充能 */
    public void applyCooldown(Player player) {
        cooldownUntil.put(player.getUniqueId(),
                System.currentTimeMillis() + plugin.getCooldownSeconds() * 1000L);
    }

    /** 统计玩家背包中的钻石数量 */
    public int countDiamonds(Player player) {
        int count = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item != null && item.getType() == Material.DIAMOND) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private void removeDiamonds(Player player, int amount) {
        ItemStack[] contents = player.getInventory().getStorageContents();
        int remaining = amount;
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == Material.DIAMOND) {
                int take = Math.min(remaining, item.getAmount());
                remaining -= take;
                if (take >= item.getAmount()) {
                    contents[i] = null;
                } else {
                    item.setAmount(item.getAmount() - take);
                }
            }
        }
        player.getInventory().setStorageContents(contents);
    }

    /** 供阵法特效任务查询：该阵法当前是否有玩家在充能（返回进度，无则 0） */
    public double getChargeProgress(String portalName) {
        double max = 0;
        for (PortalSession session : sessions.values()) {
            if (session.portalName.equals(portalName)) {
                max = Math.max(max, session.progress());
            }
        }
        return max;
    }

    /** 玩家下线时清理 */
    public void handleQuit(Player player) {
        cancelCharge(player, false);
    }

    private String msg(String text) {
        return ChatColor.translateAlternateColorCodes('&', "&8[&b空间传送阵&8] " + text);
    }
}
