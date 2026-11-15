package com.pumpkin.cml;

import org.bukkit.Bukkit;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

public class PickupListener implements Listener {

    private final ClickMobsLogger plugin;

    public PickupListener(ClickMobsLogger plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent e) {
        // 只看主手右键
        if (e.getHand() != EquipmentSlot.HAND) return;
        Entity ent = e.getRightClicked();
        // 只关心动物（ClickMobs 变成头颅的源生物）
        if (!(ent instanceof Animals)) return;
        Player p = e.getPlayer();
        // 只记录 shift + 右键（潜行）触发的"生物变成头颅收入背包"
        if (!p.isSneaking()) return;
        String type = ent.getType().name();
        // 按配置的类型白名单过滤（填 ALL 则表示全部）
        if (!plugin.isTracked(type)) return;
        // 手拿黑名单物品时 ClickMobs 不会触发变头颅，跳过
        if (plugin.isHandBlacklisted(p.getInventory().getItemInMainHand().getType())) return;

        org.bukkit.Location loc = ent.getLocation().clone();
        Entity ref = ent;
        // 延迟 1 tick 确认实体是否真的被收走（变成头颅后原实体被移除）
        // 不依赖 clickmobs.pickup 权限判断：没权限时 ClickMobs 不会真正移除实体，
        // 这里靠"实体是否消失"自然过滤掉未发生的点击。
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (ref.isValid() && !ref.isDead()) return; // 实体还在，说明没被收走
            plugin.logPickup(p, type, loc);
        }, 1L);
    }
}
