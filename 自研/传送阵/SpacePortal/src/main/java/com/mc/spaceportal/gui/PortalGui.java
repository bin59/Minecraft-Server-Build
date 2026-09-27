package com.mc.spaceportal.gui;

import com.mc.spaceportal.SpacePortalPlugin;
import com.mc.spaceportal.portal.Portal;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 传送阵箱子界面。
 * Java 版直接显示为箱子 GUI；基岩版经 Geyser 自动转换为原生箱子界面，双端通用。
 *
 * 主界面布局（54 格）：
 * - 0~44：传送阵列表（自己创建的 + 公共阵法，管理员可见全部），点击打开该阵法的管理界面
 * - 45：创建传送阵（点击后在聊天栏输入名字）
 * - 48~50：管理员数量上限控制（-1 / 当前上限 / +1）
 * - 52：费用信息展示
 * - 53：关闭界面
 *
 * 管理界面布局（27 格）：
 * - 4：连接到…（聊天栏输入目标阵法，建立双向连接）
 * - 11：传送到该阵法
 * - 13：阵法信息
 * - 15：删除该阵法（需二次确认；管理员可删任意，玩家仅能删自己的）
 * - 22：返回列表
 */
public class PortalGui {

    /** 主界面标题 */
    public static final String TITLE_MAIN = color("&b&l空间传送阵");
    /** 管理界面标题 */
    public static final String TITLE_MANAGE = color("&b&l阵法管理");

    private final SpacePortalPlugin plugin;

    public PortalGui(SpacePortalPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 界面持有者：用来区分主界面 / 管理界面，并记录当前管理的阵法名。
     * 比按标题字符串判断更可靠，不受基岩端标题转码影响。
     */
    public static final class Holder implements InventoryHolder {

        public enum Type { MAIN, MANAGE }

        private final Type type;
        private final String portalName;
        private boolean confirmDelete;
        private Inventory inventory;

        public Holder(Type type, String portalName) {
            this.type = type;
            this.portalName = portalName;
        }

        public Type getType() {
            return type;
        }

        public String getPortalName() {
            return portalName;
        }

        public boolean isConfirmDelete() {
            return confirmDelete;
        }

        public void setConfirmDelete(boolean confirmDelete) {
            this.confirmDelete = confirmDelete;
        }

        public void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    /** 为玩家打开主界面 */
    public void openMain(Player player) {
        Holder holder = new Holder(Holder.Type.MAIN, null);
        Inventory inv = Bukkit.createInventory(holder, 54, TITLE_MAIN);
        holder.setInventory(inv);
        fillPortalList(inv, player);

        boolean admin = player.hasPermission("spaceportal.admin");

        // 45：创建传送阵
        int owned = plugin.getPortals().countByOwner(player.getUniqueId());
        int limit = plugin.getPlayerPortalLimit();
        List<String> createLore = new ArrayList<>();
        createLore.add(color("&7点击后在聊天栏输入阵法名字"));
        if (!admin) {
            createLore.add(color("&7我的阵法：&f" + owned + " / " + (limit < 0 ? "∞" : limit)));
        } else {
            createLore.add(color("&7管理员建阵不占额度"));
        }
        inv.setItem(45, item(Material.EMERALD_BLOCK, "&a创建传送阵", createLore));

        // 48~50：管理员数量上限控制
        if (admin) {
            inv.setItem(48, item(Material.RED_STAINED_GLASS_PANE, "&c上限 -1",
                    Arrays.asList(color("&7点击减少玩家可建阵法数量"))));
            inv.setItem(49, item(Material.CLOCK, "&e当前上限：" + limitText(limit),
                    Arrays.asList(color("&7玩家每人最多可创建的阵法数量"),
                            color("&80 = 禁止自建，-1 = 不限制"))));
            inv.setItem(50, item(Material.LIME_STAINED_GLASS_PANE, "&a上限 +1",
                    Arrays.asList(color("&7点击增加玩家可建阵法数量"))));
        }

        // 52：费用信息
        inv.setItem(52, item(Material.DIAMOND, "&b传送费用：" + plugin.getCost() + " 钻石",
                Arrays.asList(color("&7走进阵法自动充能，完成后扣费传送"),
                        admin ? color("&7管理员可用 /csz setcost 修改") : color("&8管理员可调"))));

        // 53：关闭
        inv.setItem(53, item(Material.REDSTONE_BLOCK, "&c关闭", Arrays.asList()));

        // 未占用格子铺玻璃板，界面更整齐
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ", Arrays.asList());
        for (int i = 0; i < 54; i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, filler);
            }
        }

        player.openInventory(inv);
    }

    /** 打开某个传送阵的管理界面（confirmDelete=false 为初始状态） */
    public void openManage(Player player, Portal portal) {
        openManage(player, portal, false);
    }

    /** 打开某个传送阵的管理界面 */
    public void openManage(Player player, Portal portal, boolean confirmDelete) {
        Holder holder = new Holder(Holder.Type.MANAGE, portal.getName());
        holder.setConfirmDelete(confirmDelete);
        Inventory inv = Bukkit.createInventory(holder, 27, TITLE_MANAGE);
        holder.setInventory(inv);

        boolean admin = player.hasPermission("spaceportal.admin");
        boolean mine = player.getUniqueId().equals(portal.getOwner());
        boolean canManage = admin || mine;

        // 4：连接到…（管理员可连任意，玩家仅能连自己的）
        if (canManage) {
            inv.setItem(4, item(Material.COMPASS, "&b连接到…", Arrays.asList(
                    color("&7点击后在聊天栏输入目标阵法名字"),
                    color("&7建立 &f双向 &7连接（⇄），输入 &fcancel &7取消"))));
        }

        // 11：传送
        inv.setItem(11, item(Material.ENDER_PEARL, "&a传送到该阵法",
                Arrays.asList(color(costText(player)))));

        // 13：阵法信息
        List<String> info = new ArrayList<>();
        info.add(color("&7世界：&f" + portal.getWorldName()));
        info.add(color("&7坐标：&f" + (int) portal.getX() + "," + (int) portal.getY()
                + "," + (int) portal.getZ()));
        info.add(color("&7目的地："
                + (portal.getDestName() == null ? "&8未连接" : "&a→ " + portal.getDestName())));
        info.add(color("&7归属：" + (portal.getOwner() == null ? "&7公共阵法"
                : (mine ? "&e我的阵法" : "&7他人阵法"))));
        if (!canManage) {
            info.add(color("&8你没有管理此阵法的权限"));
        }
        inv.setItem(13, item(Material.ENDER_EYE, "&f✦ " + portal.getName(), info));

        // 15：删除（二次确认）
        if (canManage) {
            if (confirmDelete) {
                inv.setItem(15, item(Material.REDSTONE_BLOCK, "&c&l⚠ 再次点击确认删除",
                        Arrays.asList(
                                color("&7将删除 &f" + portal.getName() + " &7并断开指向它的连接"),
                                color("&7点击其他按钮或关闭界面可取消"))));
            } else {
                inv.setItem(15, item(Material.BARRIER, "&c删除该阵法",
                        Arrays.asList(color("&7删除后不可恢复，需二次确认"))));
            }
        }

        // 22：返回
        inv.setItem(22, item(Material.ARROW, "&e返回列表",
                Arrays.asList(color("&7返回传送阵列表"))));

        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ", Arrays.asList());
        for (int i = 0; i < 27; i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, filler);
            }
        }

        player.openInventory(inv);
    }

    /** 玩家可见的阵法列表（自己的 + 公共的；管理员可见全部），最多 45 个 */
    public List<Portal> visiblePortals(Player player) {
        boolean admin = player.hasPermission("spaceportal.admin");
        List<Portal> out = new ArrayList<>();
        for (Portal portal : plugin.getPortals().all()) {
            if (out.size() >= 45) {
                break;
            }
            boolean mine = player.getUniqueId().equals(portal.getOwner());
            boolean pub = portal.getOwner() == null;
            if (!admin && !mine && !pub) {
                continue;
            }
            out.add(portal);
        }
        return out;
    }

    /** 0~44：列出玩家可用阵法（自己的 + 公共的；管理员可见全部） */
    private void fillPortalList(Inventory inv, Player player) {
        int slot = 0;
        for (Portal portal : visiblePortals(player)) {
            boolean mine = player.getUniqueId().equals(portal.getOwner());
            boolean pub = portal.getOwner() == null;
            List<String> lore = new ArrayList<>();
            lore.add(color("&7世界：&f" + portal.getWorldName()
                    + " &7坐标：&f" + (int) portal.getX() + "," + (int) portal.getY() + "," + (int) portal.getZ()));
            String dest = portal.getDestName() == null ? "&8未连接" : "&a→ " + portal.getDestName();
            lore.add(color("&7目的地：" + dest));
            if (mine) {
                lore.add(color("&e★ 我的阵法"));
            } else if (pub) {
                lore.add(color("&7公共阵法"));
            } else {
                lore.add(color("&7他人阵法"));
            }
            lore.add(color("&b左键点击管理此阵法（传送 / 删除 / 连接）"));
            inv.setItem(slot++, item(Material.ENDER_EYE, "&f✦ " + portal.getName(), lore));
        }
    }

    /** 按玩家身份显示点击传送的实际费用 */
    private String costText(Player player) {
        if (plugin.getTeleportService().isFree(player)) {
            return "&b点击传送到该阵法（免费）";
        }
        return "&b点击传送到该阵法（消耗 &f" + plugin.getCost() + " &b钻石）";
    }

    private String limitText(int limit) {
        return limit < 0 ? "不限制" : String.valueOf(limit);
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(name));
            if (!lore.isEmpty()) {
                meta.setLore(lore);
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
