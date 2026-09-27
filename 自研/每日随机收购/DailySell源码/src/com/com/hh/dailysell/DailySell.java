package com.hh.dailysell;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import net.milkbowl.vault.economy.Economy;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * DailySell - 每日随机收购（v1.2.1，27 格美化 GUI + 多页）
 * 每天零点从物品池随机抽 N 种物品：
 *   - 每种当天额度随机 [min-per-item, max-per-item] 个（每人当天最多卖这么多个）
 *   - 每种单价 = 基础价 × 当日随机浮动系数（取整）
 *   - 生成时保证 4 种合计（单价×额度）≤ daily-budget（默认 800），超了自动缩减额度
 * 玩家：
 *   - /ds（或 /ds gui）打开 GUI：左键卖 1 个、Shift+左键卖满额度、支持多页
 *   - /ds sell（卖1个）/ /ds sellall（卖满额度）命令方式
 *   - /ds today 查看清单
 * 防刷：每人每天每种额度上限 + 每人每日总收益上限（daily-budget）。
 */
public class DailySell extends JavaPlugin implements CommandExecutor, Listener {

    private Economy econ;
    private FileConfiguration cfg;
    private File dailyFile;
    private YamlConfiguration daily;
    private File dataFile;
    private YamlConfiguration data;

    private int itemsPerDay;
    private int minPerItem;
    private int maxPerItem;
    private double priceFloatMin;
    private double priceFloatMax;
    private double dailyBudget;
    private boolean broadcast;
    private final LinkedHashMap<String, Double> pool = new LinkedHashMap<>();

    // 今日清单条目
    private static class Entry {
        final Material mat;
        final double price;   // 当日单价
        final int quota;      // 当日额度（每人可卖个数）
        Entry(Material m, double p, int q) { mat = m; price = p; quota = q; }
    }
    private final List<Entry> today = new ArrayList<>();
    private String todayDate = "";

    // 打开的 GUI（玩家 UUID -> 打开的收购界面）与页码
    private final Map<UUID, Inventory> guis = new HashMap<>();
    private final Map<UUID, Integer> guiPages = new HashMap<>();
    private static final int PER_PAGE = 14;

    // 常用物品中文名（展示用，兜底英文名）
    private static final Map<String, String> ZH = new HashMap<>();
    static {
        ZH.put("WHEAT", "小麦"); ZH.put("POTATO", "马铃薯"); ZH.put("CARROT", "胡萝卜");
        ZH.put("SWEET_BERRIES", "甜浆果"); ZH.put("MELON_SLICE", "西瓜片"); ZH.put("PUMPKIN", "南瓜");
        ZH.put("SUGAR_CANE", "甘蔗"); ZH.put("COCOA_BEANS", "可可豆"); ZH.put("BREAD", "面包");
        ZH.put("APPLE", "苹果"); ZH.put("COAL", "煤炭"); ZH.put("RAW_IRON", "粗铁");
        ZH.put("RAW_GOLD", "粗金"); ZH.put("IRON_INGOT", "铁锭"); ZH.put("GOLD_INGOT", "金锭");
        ZH.put("COPPER_INGOT", "铜锭"); ZH.put("LAPIS_LAZULI", "青金石"); ZH.put("REDSTONE", "红石");
        ZH.put("DIAMOND", "钻石"); ZH.put("EMERALD", "绿宝石"); ZH.put("OAK_LOG", "橡木原木");
        ZH.put("BIRCH_LOG", "白桦原木"); ZH.put("SPRUCE_LOG", "云杉原木"); ZH.put("JUNGLE_LOG", "丛林原木");
        ZH.put("STONE", "石头"); ZH.put("COBBLESTONE", "圆石"); ZH.put("DEEPSLATE", "深板岩");
        ZH.put("STRING", "线"); ZH.put("BONE", "骨头"); ZH.put("GUNPOWDER", "火药");
        ZH.put("ROTTEN_FLESH", "腐肉"); ZH.put("SPIDER_EYE", "蜘蛛眼"); ZH.put("SLIME_BALL", "粘液球");
        ZH.put("BLAZE_ROD", "烈焰棒"); ZH.put("ENDER_PEARL", "末影珍珠"); ZH.put("NETHER_WART", "地狱疣");
        ZH.put("PRISMARINE_SHARD", "海晶碎片"); ZH.put("FEATHER", "羽毛"); ZH.put("LEATHER", "皮革");
        ZH.put("EGG", "鸡蛋"); ZH.put("INK_SAC", "墨囊"); ZH.put("GLOW_INK_SAC", "荧光墨囊");
        ZH.put("NETHER_QUARTZ", "下界石英"); ZH.put("AMETHYST_SHARD", "紫水晶碎片");
        ZH.put("BONE_MEAL", "骨粉"); ZH.put("HONEYCOMB", "蜜脾"); ZH.put("GLOWSTONE_DUST", "萤石粉");
        ZH.put("OBSIDIAN", "黑曜石"); ZH.put("SHULKER_SHELL", "潜影壳");
        ZH.put("NETHER_STAR", "凋灵之星"); ZH.put("DRAGON_HEAD", "龙首"); ZH.put("ELYTRA", "鞘翅");
    }

    private static String zh(Material m) {
        return ZH.getOrDefault(m.name(), m.name());
    }

    @Override
    public void onEnable() {
        if (!setupEconomy()) {
            getLogger().severe("未找到 Vault 经济插件，已禁用");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        saveDefaultConfig();
        cfg = getConfig();
        loadParams();

        dailyFile = new File(getDataFolder(), "daily.yml");
        dataFile = new File(getDataFolder(), "data.yml");
        daily = YamlConfiguration.loadConfiguration(dailyFile);
        data = YamlConfiguration.loadConfiguration(dataFile);

        rollIfNeeded(true);

        new BukkitRunnable() {
            @Override
            public void run() {
                rollIfNeeded(false);
            }
        }.runTaskTimer(this, 1200L, 1200L);

        getServer().getPluginManager().registerEvents(this, this);
        getCommand("ds").setExecutor(this);
        getLogger().info("DailySell v" + getDescription().getVersion() + " 已启用（GUI + 命令）");
    }

    @Override
    public void onDisable() {
        saveData();
    }

    private void loadParams() {
        itemsPerDay = cfg.getInt("items-per-day", 4);
        minPerItem = cfg.getInt("min-per-item", 10);
        maxPerItem = cfg.getInt("max-per-item", 100);
        priceFloatMin = cfg.getDouble("price-float-min", 0.6);
        priceFloatMax = cfg.getDouble("price-float-max", 1.4);
        dailyBudget = cfg.getDouble("daily-budget", 800);
        broadcast = cfg.getBoolean("broadcast", true);
        pool.clear();
        if (cfg.contains("pool") && cfg.getConfigurationSection("pool") != null) {
            for (String key : cfg.getConfigurationSection("pool").getKeys(false)) {
                pool.put(key.toUpperCase(), cfg.getDouble("pool." + key));
            }
        }
    }

    /** 日期变化时重新生成今日清单 */
    private void rollIfNeeded(boolean force) {
        String now = LocalDate.now().toString();
        String stored = daily.getString("date", "");
        if (!force && now.equals(stored)) return;
        List<Entry> rolled = roll();
        if (rolled == null) {
            getLogger().warning("物品池为空或不足，无法生成今日清单");
            return;
        }
        today.clear();
        today.addAll(rolled);
        todayDate = now;
        daily.set("date", now);
        daily.set("items", today.stream()
                .map(e -> e.mat.name() + ":" + e.price + ":" + e.quota)
                .collect(Collectors.toList()));
        try { daily.save(dailyFile); } catch (IOException ex) { getLogger().severe("daily.yml 保存失败"); }
        if (broadcast) broadcastToday();
    }

    /** 随机生成今日清单（满足额度范围 + 总价 ≤ daily-budget） */
    private List<Entry> roll() {
        List<String> keys = new ArrayList<>(pool.keySet());
        if (keys.isEmpty()) return null;
        Random rnd = new Random();
        Collections.shuffle(keys, rnd);
        int n = Math.min(itemsPerDay, keys.size());

        for (int attempt = 0; attempt < 10; attempt++) {
            Collections.shuffle(keys, rnd);
            List<Entry> list = new ArrayList<>();
            double total = 0;
            for (int i = 0; i < n; i++) {
                Material m = Material.matchMaterial(keys.get(i));
                if (m == null || !m.isItem()) continue;
                double base = pool.get(keys.get(i));
                double price = Math.max(1, Math.round(base * (priceFloatMin + rnd.nextDouble() * (priceFloatMax - priceFloatMin))));
                int quota = minPerItem + rnd.nextInt(Math.max(1, maxPerItem - minPerItem + 1));
                double cap = dailyBudget - total;
                if (price * quota > cap) {
                    int maxQ = (int) Math.floor(cap / price);
                    if (maxQ < 1) continue;
                    quota = Math.min(quota, maxQ);
                }
                list.add(new Entry(m, price, quota));
                total += price * quota;
            }
            if (list.size() == n) return list;
        }
        List<Entry> fallback = new ArrayList<>();
        double total = 0;
        for (int i = 0; i < n; i++) {
            Material m = Material.matchMaterial(keys.get(i));
            if (m == null || !m.isItem()) continue;
            double price = Math.max(1, Math.round(pool.get(keys.get(i)) * priceFloatMin));
            if (total + price > dailyBudget) break;
            fallback.add(new Entry(m, price, 1));
            total += price;
        }
        return fallback.isEmpty() ? null : fallback;
    }

    private void broadcastToday() {
        StringBuilder sb = new StringBuilder();
        sb.append("§6✦ 今日收购（每人每种额度随机，总价上限 §e")
          .append(fmt(dailyBudget)).append("§6 币）: ");
        for (int i = 0; i < today.size(); i++) {
            Entry e = today.get(i);
            if (i > 0) sb.append("§7, ");
            sb.append("§e").append(zh(e.mat)).append("§7(").append(fmt(e.price)).append("币/个, 限")
              .append(e.quota).append("个)");
        }
        sb.append("  §a/ds §7打开收购界面");
        Bukkit.broadcastMessage(sb.toString());
    }

    private String fmt(double v) {
        if (v == Math.floor(v)) return String.valueOf((long) v);
        return String.valueOf(v);
    }

    // ---------- 数据 ----------

    private String soldKey(Player p, Material m) {
        return "players." + p.getUniqueId() + "." + todayDate + "." + m.name();
    }

    private int soldCount(Player p, Material m) {
        return data.getInt(soldKey(p, m), 0);
    }

    private Entry findEntry(Material m) {
        for (Entry e : today) if (e.mat == m) return e;
        return null;
    }

    /** 玩家当天已获总收益 */
    private double earnedToday(Player p) {
        double sum = 0;
        for (Entry e : today) {
            sum += soldCount(p, e.mat) * e.price;
        }
        return sum;
    }

    private void saveData() {
        try { data.save(dataFile); } catch (IOException ex) { getLogger().severe("data.yml 保存失败"); }
    }

    // ---------- GUI（27 格美化 + 多页） ----------

    private ItemStack glass(Material m, String name) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(name == null ? " " : name);
        it.setItemMeta(meta);
        return it;
    }

    private void openGui(Player p) {
        if (today.isEmpty()) { p.sendMessage("§c今日收购清单未生成"); return; }
        guiPages.put(p.getUniqueId(), 1);
        Inventory inv = Bukkit.createInventory(null, 27, "§6✦ 每日收购 · 今日");
        refreshGui(p, inv);
        guis.put(p.getUniqueId(), inv);
        p.openInventory(inv);
    }

    private void refreshGui(Player p, Inventory inv) {
        inv.clear();
        int page = Math.max(1, guiPages.getOrDefault(p.getUniqueId(), 1));
        int totalPages = Math.max(1, (int) Math.ceil(today.size() / (double) PER_PAGE));
        if (page > totalPages) page = totalPages;
        guiPages.put(p.getUniqueId(), page);

        // 背景装饰：物品区深色玻璃 + 底部控制条墨色玻璃
        ItemStack bg = glass(Material.BLACK_STAINED_GLASS_PANE, null);
        ItemStack bar = glass(Material.GRAY_STAINED_GLASS_PANE, null);
        for (int i = 0; i < 27; i++) inv.setItem(i, bg);
        for (int i = 18; i < 27; i++) inv.setItem(i, bar);

        // 物品槽 0-13（每页 14 个）
        int start = (page - 1) * PER_PAGE;
        for (int i = 0; i < PER_PAGE; i++) {
            int idx = start + i;
            if (idx < today.size()) {
                inv.setItem(i, buildIcon(p, today.get(idx)));
            } else {
                inv.setItem(i, bg.clone());
            }
        }

        // 今日收益（槽 22）
        ItemStack info = new ItemStack(Material.GOLD_NUGGET);
        ItemMeta im = info.getItemMeta();
        im.setDisplayName("§6✦ 今日收益");
        im.setLore(Arrays.asList(
                "§7已获: §a" + fmt(earnedToday(p)) + "§7 / §e" + fmt(dailyBudget) + "§7 南瓜币",
                "§7额度用完或达上限即收摊"));
        info.setItemMeta(im);
        inv.setItem(22, info);

        // 操作说明（槽 24）
        ItemStack foot = new ItemStack(Material.PAPER);
        ItemMeta fom = foot.getItemMeta();
        fom.setDisplayName("§7❓ 操作说明");
        fom.setLore(Arrays.asList(
                "§7左键点击物品 = 卖 1 个",
                "§7Shift+左键 = 卖满今日额度",
                "§7ESC / 关闭界面即退出"));
        foot.setItemMeta(fom);
        inv.setItem(24, foot);

        // 翻页（槽 18 上一页 / 26 下一页）
        if (page > 1) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta pm = prev.getItemMeta();
            pm.setDisplayName("§b◀ 上一页 §7(第 " + page + "/" + totalPages + " 页)");
            prev.setItemMeta(pm);
            inv.setItem(18, prev);
        }
        if (page < totalPages) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nm = next.getItemMeta();
            nm.setDisplayName("§b下一页 ▶ §7(第 " + page + "/" + totalPages + " 页)");
            next.setItemMeta(nm);
            inv.setItem(26, next);
        }
        // 页码（槽 20）
        ItemStack pageIcon = new ItemStack(Material.BOOK);
        ItemMeta pgm = pageIcon.getItemMeta();
        pgm.setDisplayName("§7页码 §e" + page + "§7 / §e" + totalPages);
        pageIcon.setItemMeta(pgm);
        inv.setItem(20, pageIcon);
    }

    /** 单个物品图标：剩余额度用数量显示，未卖完发光 */
    private ItemStack buildIcon(Player p, Entry e) {
        int sold = soldCount(p, e.mat);
        int remaining = Math.max(0, e.quota - sold);
        ItemStack icon = new ItemStack(e.mat, Math.min(Math.max(1, remaining), 64));
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName("§e" + zh(e.mat));
        List<String> lore = new ArrayList<>();
        lore.add("§7单价: §a" + fmt(e.price) + "§7 南瓜币/个");
        lore.add("§7今日额度: §e" + sold + "§7/§e" + e.quota);
        if (remaining <= 0) {
            lore.add("");
            lore.add("§c今日额度已用完");
        } else {
            lore.add("");
            lore.add("§a左键 §7卖 1 个");
            lore.add("§aShift+左键 §7卖满额度");
        }
        meta.setLore(lore);
        if (remaining > 0) {
            meta.addEnchant(org.bukkit.enchantments.Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        }
        icon.setItemMeta(meta);
        return icon;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        Inventory inv = guis.get(p.getUniqueId());
        if (inv == null || e.getInventory() != inv) return;
        e.setCancelled(true);
        ItemStack cur = e.getCurrentItem();
        if (cur == null || cur.getType() == Material.AIR) return;

        int slot = e.getSlot();
        if (slot == 18 && cur.getType() == Material.ARROW) {
            guiPages.put(p.getUniqueId(), Math.max(1, guiPages.getOrDefault(p.getUniqueId(), 1) - 1));
            refreshGui(p, inv);
            p.playSound(p.getLocation(), "ui.button.click", 0.6f, 1.2f);
            return;
        }
        if (slot == 26 && cur.getType() == Material.ARROW) {
            guiPages.put(p.getUniqueId(), guiPages.getOrDefault(p.getUniqueId(), 1) + 1);
            refreshGui(p, inv);
            p.playSound(p.getLocation(), "ui.button.click", 0.6f, 1.2f);
            return;
        }

        Entry entry = findEntry(cur.getType());
        if (entry == null) return;
        if (e.isShiftClick()) {
            sellFromGui(p, entry, inv, 0);
        } else {
            sellFromGui(p, entry, inv, 1);
        }
    }

    /** amount=0 表示卖满额度/预算 */
    private void sellFromGui(Player p, Entry entry, Inventory inv, int amount) {
        int sold = soldCount(p, entry.mat);
        int remainQuota = entry.quota - sold;
        if (remainQuota <= 0) {
            p.sendMessage("§c今日「" + zh(entry.mat) + "」额度已用完");
            p.playSound(p.getLocation(), "entity.villager.no", 0.6f, 1.0f);
            refreshGui(p, inv);
            return;
        }
        double budgetLeft = dailyBudget - earnedToday(p);
        int budgetAllow = budgetLeft >= entry.price ? (int) Math.floor(budgetLeft / entry.price) : 0;
        if (budgetAllow <= 0) {
            p.sendMessage("§c今日收购总收益已达上限（" + fmt(dailyBudget) + " 币）");
            p.playSound(p.getLocation(), "entity.villager.no", 0.6f, 1.0f);
            refreshGui(p, inv);
            return;
        }
        int amt = amount > 0 ? 1 : Math.min(remainQuota, budgetAllow);
        if (amt > 1) {
            int held = countHeld(p, entry.mat);
            amt = Math.min(amt, held);
            if (amt <= 0) {
                p.sendMessage("§c背包里没有「" + zh(entry.mat) + "」可出售");
                return;
            }
        } else {
            if (!hasOne(p, entry.mat)) {
                p.sendMessage("§c背包里没有「" + zh(entry.mat) + "」可出售");
                return;
            }
        }
        removeItems(p, entry.mat, amt);
        econ.depositPlayer(p, entry.price * amt);
        data.set(soldKey(p, entry.mat), sold + amt);
        saveData();
        p.sendMessage("§a已出售 §e" + zh(entry.mat) + "§a ×" + amt + "，获得 §e"
                + fmt(entry.price * amt) + "§a 南瓜币（额度剩 §e" + (entry.quota - sold - amt) + "§a 个）");
        p.playSound(p.getLocation(), "entity.experience_orb.pickup", 0.6f, 1.4f);
        refreshGui(p, inv);
    }

    private int countHeld(Player p, Material m) {
        int n = 0;
        for (ItemStack it : p.getInventory().getContents()) {
            if (it != null && it.getType() == m) n += it.getAmount();
        }
        return n;
    }

    private boolean hasOne(Player p, Material m) {
        return countHeld(p, m) > 0;
    }

    private void removeItems(Player p, Material m, int amount) {
        int left = amount;
        for (ItemStack it : p.getInventory().getContents()) {
            if (left <= 0) break;
            if (it != null && it.getType() == m) {
                int take = Math.min(it.getAmount(), left);
                it.setAmount(it.getAmount() - take);
                left -= take;
            }
        }
    }

    // ---------- 命令 ----------

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String sub = args.length > 0 ? args[0].toLowerCase() : "gui";
        if (!(sender instanceof Player) && !sub.equals("reload") && !sub.equals("reroll")) {
            sender.sendMessage("此命令需在游戏内执行");
            return true;
        }
        switch (sub) {
            case "sell":
                sellHand((Player) sender);
                break;
            case "sellall":
                sellAll((Player) sender);
                break;
            case "gui":
            case "menu":
                openGui((Player) sender);
                break;
            case "today":
                showToday(sender);
                break;
            case "reload":
                if (sender.hasPermission("dailysell.admin")) {
                    reloadConfig();
                    cfg = getConfig();
                    loadParams();
                    sender.sendMessage("§a配置已重载（今日清单不变，需重抽用 /ds reroll）");
                } else sender.sendMessage(ChatColor.RED + "没有权限");
                break;
            case "reroll":
                if (sender.hasPermission("dailysell.admin")) {
                    rollIfNeeded(true);
                    sender.sendMessage("§a已重新生成今日清单");
                } else sender.sendMessage(ChatColor.RED + "没有权限");
                break;
            default:
                sender.sendMessage("§6用法: §e/ds §7打开收购界面 §7|§e /ds sell §7|§e sellall §7|§e today §7|§e reload §7|§e reroll");
                break;
        }
        return true;
    }

    private void sellHand(Player p) {
        if (today.isEmpty()) { p.sendMessage("§c今日收购清单未生成"); return; }
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) {
            p.sendMessage("§c请手持要出售的物品（输入 §a/ds §7打开收购界面）");
            return;
        }
        Entry entry = findEntry(hand.getType());
        if (entry == null) {
            p.sendMessage("§c「" + zh(hand.getType()) + "」不是今日收购物品，清单见 §a/ds §7或 §a/ds today");
            return;
        }
        int sold = soldCount(p, entry.mat);
        if (sold >= entry.quota) {
            p.sendMessage("§c今日「" + zh(entry.mat) + "」额度已用完（限 " + entry.quota + " 个）");
            return;
        }
        if (earnedToday(p) + entry.price > dailyBudget) {
            p.sendMessage("§c今日收购总收益已达上限（" + fmt(dailyBudget) + " 币）");
            return;
        }
        if (!hasOne(p, entry.mat)) {
            p.sendMessage("§c背包里没有「" + zh(entry.mat) + "」可出售");
            return;
        }
        removeItems(p, entry.mat, 1);
        econ.depositPlayer(p, entry.price);
        data.set(soldKey(p, entry.mat), sold + 1);
        saveData();
        p.sendMessage("§a已出售 §e" + zh(entry.mat) + "§a ×1，获得 §e" + fmt(entry.price)
                + "§a 南瓜币（额度剩 §e" + (entry.quota - sold - 1) + "§a 个）");
    }

    private void sellAll(Player p) {
        if (today.isEmpty()) { p.sendMessage("§c今日收购清单未生成"); return; }
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) {
            p.sendMessage("§c请手持要出售的物品");
            return;
        }
        Entry entry = findEntry(hand.getType());
        if (entry == null) {
            p.sendMessage("§c「" + zh(hand.getType()) + "」不是今日收购物品");
            return;
        }
        int sold = soldCount(p, entry.mat);
        int remainQuota = entry.quota - sold;
        if (remainQuota <= 0) {
            p.sendMessage("§c今日「" + zh(entry.mat) + "」额度已用完");
            return;
        }
        double budgetLeft = dailyBudget - earnedToday(p);
        int budgetAllow = budgetLeft >= entry.price ? (int) Math.floor(budgetLeft / entry.price) : 0;
        if (budgetAllow <= 0) {
            p.sendMessage("§c今日收购总收益已达上限（" + fmt(dailyBudget) + " 币）");
            return;
        }
        int amount = Math.min(hand.getAmount(), Math.min(remainQuota, budgetAllow));
        removeItems(p, entry.mat, amount);
        econ.depositPlayer(p, entry.price * amount);
        data.set(soldKey(p, entry.mat), sold + amount);
        saveData();
        p.sendMessage("§a已出售 §e" + zh(entry.mat) + "§a ×" + amount + "，获得 §e"
                + fmt(entry.price * amount) + "§a 南瓜币（额度剩 §e" + (entry.quota - sold - amount) + "§a 个）");
    }

    private void showToday(CommandSender sender) {
        if (today.isEmpty()) { sender.sendMessage("§c今日收购清单未生成"); return; }
        sender.sendMessage("§6======== 今日收购清单 ========");
        double total = 0;
        for (Entry e : today) {
            String extra = "";
            if (sender instanceof Player) {
                int sold = soldCount((Player) sender, e.mat);
                extra = " §7(我已卖 " + sold + "/" + e.quota + ")";
            }
            sender.sendMessage("§e" + zh(e.mat) + " §7×1 → §a" + fmt(e.price) + "§7 币"
                    + "§7 额度" + e.quota + "个" + extra);
            total += e.price * e.quota;
        }
        sender.sendMessage("§7清单总价上限: §a" + fmt(dailyBudget) + "§7 币（本次清单合计 " + fmt(total) + " 币）");
        sender.sendMessage("§7出售: §a/ds §7打开收购界面，或手持物品 §a/ds sell §7(卖1个) / §a/ds sellall §7(卖满额度)");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp =
                getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        econ = rsp.getProvider();
        return econ != null;
    }
}
