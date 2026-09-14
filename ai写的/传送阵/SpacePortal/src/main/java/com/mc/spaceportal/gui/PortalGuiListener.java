package com.mc.spaceportal.gui;

import com.mc.spaceportal.SpacePortalPlugin;
import com.mc.spaceportal.portal.Portal;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 箱子界面事件监听：主界面 / 管理界面按钮点击、聊天栏输入、界面关闭清理。
 */
public class PortalGuiListener implements Listener {

    private final SpacePortalPlugin plugin;
    private final PortalGui gui;
    /** 正在等待聊天栏输入阵法名字的玩家 */
    private final Set<UUID> naming = new HashSet<>();
    /** 正在等待聊天栏输入连接目标的玩家：UUID → 源阵法名 */
    private final Map<UUID, String> linking = new HashMap<>();

    public PortalGuiListener(SpacePortalPlugin plugin) {
        this.plugin = plugin;
        this.gui = new PortalGui(plugin);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof PortalGui.Holder holder)) {
            return;
        }
        // 界面上的物品一律不可拿走/移动
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        int slot = event.getRawSlot();
        // 点到的是玩家自己的背包，不处理
        if (slot < 0 || slot >= top.getSize()) {
            return;
        }
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR
                || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) {
            return;
        }

        if (holder.getType() == PortalGui.Holder.Type.MAIN) {
            handleMainClick(player, slot);
        } else {
            handleManageClick(player, holder, slot);
        }
    }

    // ---------------- 主界面 ----------------

    private void handleMainClick(Player player, int slot) {
        boolean admin = player.hasPermission("spaceportal.admin");
        if (slot <= 44) {
            List<Portal> visible = gui.visiblePortals(player);
            if (slot >= visible.size()) {
                return;
            }
            Portal portal = visible.get(slot);
            if (plugin.getPortals().get(portal.getName()) == null) {
                player.sendMessage(msg("&c该阵法已不存在，界面即将刷新"));
                refreshMain(player);
                return;
            }
            gui.openManage(player, portal);
        } else if (slot == 45) {
            handleCreate(player);
        } else if (admin && slot >= 48 && slot <= 50) {
            handleLimit(player, slot);
        } else if (slot == 53) {
            player.closeInventory();
        }
    }

    /** 点击创建按钮：进入聊天栏命名模式 */
    private void handleCreate(Player player) {
        boolean admin = player.hasPermission("spaceportal.admin");
        if (!admin) {
            if (!player.hasPermission("spaceportal.create")) {
                player.sendMessage(msg("&c你没有权限创建传送阵"));
                return;
            }
            int limit = plugin.getPlayerPortalLimit();
            if (limit == 0) {
                player.sendMessage(msg("&c当前服务器不允许玩家自建传送阵"));
                return;
            }
            int owned = plugin.getPortals().countByOwner(player.getUniqueId());
            if (limit > 0 && owned >= limit) {
                player.sendMessage(msg("&c你名下已有 &f" + owned + " &c个传送阵，达到上限 &f" + limit + " &c个"));
                return;
            }
        }
        // 先关闭界面再进入命名模式：关闭事件会清理 naming，顺序颠倒会导致命名模式失效
        player.closeInventory();
        naming.add(player.getUniqueId());
        player.sendMessage(msg("&7请在聊天栏输入阵法名字（中英文/数字/下划线，最多 16 字），输入 &fcancel &7取消"));
    }

    /** 管理员数量上限 -1 / +1 */
    private void handleLimit(Player player, int slot) {
        int limit = plugin.getPlayerPortalLimit();
        if (slot == 48) {
            if (limit == -1) {
                limit = 9; // 从"不限制"下调到一个常规起点
            }
            limit = Math.max(0, limit - 1);
        } else if (slot == 50) {
            limit = (limit == -1) ? -1 : Math.min(64, limit + 1);
        }
        plugin.setPlayerPortalLimit(limit);
        player.sendMessage(msg("&a✔ 玩家建阵上限已设为 &f" + (limit == -1 ? "不限制" : limit)));
        refreshMain(player);
    }

    // ---------------- 管理界面 ----------------

    private void handleManageClick(Player player, PortalGui.Holder holder, int slot) {
        Portal portal = plugin.getPortals().get(holder.getPortalName());
        if (portal == null) {
            player.sendMessage(msg("&c该阵法已不存在"));
            gui.openMain(player);
            return;
        }
        boolean admin = player.hasPermission("spaceportal.admin");
        boolean canManage = admin || player.getUniqueId().equals(portal.getOwner());
        switch (slot) {
            case 4 -> {
                if (canManage) {
                    startLinking(player, portal);
                }
            }
            case 11 -> handleTeleport(player, portal);
            case 15 -> {
                if (canManage) {
                    handleDelete(player, holder, portal);
                }
            }
            case 22 -> gui.openMain(player);
            default -> {
                // 其他格子（信息 / 玻璃板）不响应
            }
        }
    }

    /** 传送到该阵法：与充能传送同一套费用规则 */
    private void handleTeleport(Player player, Portal portal) {
        Location loc = portal.getCenter();
        if (loc.getWorld() == null) {
            player.sendMessage(msg("&c该阵法所在世界未加载"));
            return;
        }
        // 通用扣费：钻石不足时不关闭界面、不传送，玩家可回去继续看界面
        if (!plugin.getTeleportService().chargeFee(player)) {
            return;
        }
        loc.add(0.5, 1.0, 0.5);
        loc.setYaw(player.getLocation().getYaw());
        loc.setPitch(player.getLocation().getPitch());
        player.closeInventory();
        player.teleport(loc);
        // 设置冷却：落地就在阵法中心，避免移动时被立刻重新触发充能再扣一次费
        plugin.getTeleportService().applyCooldown(player);
        player.sendMessage(msg("&a✔ 已传送到阵法 &f" + portal.getName()));
    }

    /** 删除阵法：第一次点击进入确认态，再点一次才真正删除 */
    private void handleDelete(Player player, PortalGui.Holder holder, Portal portal) {
        if (!holder.isConfirmDelete()) {
            gui.openManage(player, portal, true);
            return;
        }
        String name = portal.getName();
        if (plugin.getPortals().remove(name)) {
            player.sendMessage(msg("&a✔ 传送阵 &f" + name + " &a已删除（指向它的连接已断开）"));
        } else {
            player.sendMessage(msg("&c删除失败：阵法 &f" + name + " &c已不存在"));
        }
        gui.openMain(player);
    }

    /** 点击连接到…：进入聊天栏输入目标阵法模式 */
    private void startLinking(Player player, Portal portal) {
        // 先关闭界面再加入等待集合：关闭事件会清空 linking，顺序反了会失效
        player.closeInventory();
        linking.put(player.getUniqueId(), portal.getName());
        player.sendMessage(msg("&7请在聊天栏输入要与 &f" + portal.getName()
                + " &7双向连接的目标阵法名，输入 &fcancel &7取消"));
    }

    private void finishLinking(Player player, String target) {
        String from = linking.remove(player.getUniqueId());
        if (from == null || !player.isOnline()) {
            return;
        }
        if (target.equalsIgnoreCase("cancel")) {
            player.sendMessage(msg("&7已取消连接"));
            return;
        }
        if (plugin.getPortals().get(from) == null) {
            player.sendMessage(msg("&c源阵法 &f" + from + " &c已不存在"));
            return;
        }
        if (from.equalsIgnoreCase(target)) {
            player.sendMessage(msg("&c不能连接到自身"));
            return;
        }
        if (plugin.getPortals().link2Way(from, target)) {
            player.sendMessage(msg("&a✔ 已双向连接 &f" + from + " &a⇄ &f" + target));
        } else {
            player.sendMessage(msg("&c连接失败：未找到阵法 &f" + target));
        }
    }

    // ---------------- 聊天栏 / 关闭 ----------------

    /** 聊天栏捕获输入（命名模式 / 连接目标模式） */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        boolean isNaming = naming.contains(uuid);
        boolean isLinking = linking.containsKey(uuid);
        if (!isNaming && !isLinking) {
            return;
        }
        event.setCancelled(true);
        String input = event.getMessage().trim();
        // 回到主线程执行，保证线程安全
        if (isNaming) {
            plugin.getServer().getScheduler().runTask(plugin, () -> finishNaming(player, input));
        } else {
            plugin.getServer().getScheduler().runTask(plugin, () -> finishLinking(player, input));
        }
    }

    private void finishNaming(Player player, String name) {
        naming.remove(player.getUniqueId());
        if (!player.isOnline()) {
            return;
        }
        if (name.equalsIgnoreCase("cancel")) {
            player.sendMessage(msg("&7已取消创建"));
            return;
        }
        if (!name.matches("[A-Za-z0-9_\\u4e00-\\u9fa5]{1,16}")) {
            player.sendMessage(msg("&c名字只能包含中英文、数字、下划线，最多 16 个字符"));
            return;
        }
        // 二次校验数量（防止命名期间上限被改）
        boolean admin = player.hasPermission("spaceportal.admin");
        UUID owner = null;
        if (!admin) {
            int limit = plugin.getPlayerPortalLimit();
            if (limit == 0 || (limit > 0
                    && plugin.getPortals().countByOwner(player.getUniqueId()) >= limit)) {
                player.sendMessage(msg("&c无法创建：已达数量上限或服务器禁止自建"));
                return;
            }
            owner = player.getUniqueId();
        }
        Location loc = player.getLocation();
        if (!plugin.getPortals().create(name, loc, 185f, owner)) {
            player.sendMessage(msg("&c已存在名为 &f" + name + " &c的传送阵"));
            return;
        }
        player.sendMessage(msg("&a✔ 传送阵 &f" + name + " &a已创建在脚下"));
        player.sendMessage(msg("&7下一步：在界面里点开该阵法，用 &f连接到… &7建立连接"));
    }

    /** 关闭界面时退出等待输入状态 */
    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof PortalGui.Holder)) {
            return;
        }
        if (event.getPlayer() instanceof Player player) {
            naming.remove(player.getUniqueId());
            linking.remove(player.getUniqueId());
        }
    }

    /** 刷新当前打开的主界面 */
    private void refreshMain(Player player) {
        Inventory open = player.getOpenInventory().getTopInventory();
        if (open.getHolder() instanceof PortalGui.Holder holder
                && holder.getType() == PortalGui.Holder.Type.MAIN) {
            gui.openMain(player);
        }
    }

    private String msg(String text) {
        return ChatColor.translateAlternateColorCodes('&',
                "&8[&b空间传送阵&8] " + text);
    }
}
