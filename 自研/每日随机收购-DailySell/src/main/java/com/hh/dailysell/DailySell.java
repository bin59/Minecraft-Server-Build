package com.hh.dailysell;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import net.milkbowl.vault.economy.Economy;

import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.response.CustomFormResponse;
import org.geysermc.cumulus.response.SimpleFormResponse;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * DailySell - 每日随机收购（8 种选 5 种，27 格 GUI + 命令）
 * 每天零点从物品池随机抽 8 种，作为全服共享的今日候选清单；
 *   - 每种当天额度随机 [min-per-item, max-per-item] 个，单价 = 基础价 × 浮动系数
 *   - 生成时保证 8 种合计（单价×额度）≤ daily-budget（默认 900），超了自动缩减额度
 *   - 每位玩家从中自选 5 种，只有选中的能卖；选择按日期存 data.yml 的 choices 段
 * 玩家：/ds 打开界面（未选满先来自选界面）/ /ds choose <序号> / /ds today / /ds sell / /ds sellall
 * 防刷：每人每天每种额度上限 + 每人每日总收益上限（daily-budget）。
 * 收益按「玩家+日期」独立累计（players.<uuid>.<date>.__earned），与自选清单解耦，
 * 因此 /ds choose clear 重选、/ds reroll 重抽都不会重置已赚金额。
 */
public class DailySell extends JavaPlugin implements CommandExecutor, Listener {

    private Economy econ;
    private FileConfiguration cfg;
    private File dailyFile;
    private YamlConfiguration daily;
    private File dataFile;
    private YamlConfiguration data;

    // 防抖落盘：data 有改动时置 dirty，并延迟 100 ticks 合并写盘（避免每次卖出都同步全量写 yml）
    private boolean dirty = false;
    private BukkitTask saveTask = null;
    private static final long SAVE_DELAY_TICKS = 100L;
    // 旧日期数据保留天数（含当天），rollIfNeeded 检测到跨天时清理更早的键
    private static final int KEEP_DAYS = 7;

    private int itemsPerDay;
    private int choicesPerDay;
    private int minPerItem;
    private int maxPerItem;
    private double priceFloatMin;
    private double priceFloatMax;
    private double dailyBudget;
    private boolean broadcast;
    // Floodgate 是否可用（基岩原生表单）。通过 Class.forName 探测，
    // 未装 Floodgate 时恒为 false，基岩玩家回退箱子 GUI（与 Java 端一致）。
    private boolean floodgateAvailable = false;
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

    // 打开的 GUI（玩家 UUID -> 打开的界面）与页码
    private final Map<UUID, Inventory> guis = new HashMap<>();
    private final Map<UUID, Inventory> chooseGuis = new HashMap<>();
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
        ZH.put("QUARTZ", "下界石英"); ZH.put("AMETHYST_SHARD", "紫水晶碎片");
        ZH.put("BONE_MEAL", "骨粉"); ZH.put("HONEYCOMB", "蜜脾"); ZH.put("GLOWSTONE_DUST", "萤石粉");
        ZH.put("OBSIDIAN", "黑曜石"); ZH.put("SHULKER_SHELL", "潜影壳");
        ZH.put("NETHER_STAR", "凋灵之星"); ZH.put("DRAGON_HEAD", "龙首"); ZH.put("ELYTRA", "鞘翅");
        // ---- 农作物与食物 ----
        ZH.put("BEETROOT", "甜菜根"); ZH.put("WHEAT_SEEDS", "小麦种子"); ZH.put("MELON", "西瓜");
        ZH.put("GOLDEN_CARROT", "金胡萝卜"); ZH.put("BAKED_POTATO", "烤马铃薯"); ZH.put("CAKE", "蛋糕");
        ZH.put("COOKIE", "曲奇"); ZH.put("PUMPKIN_PIE", "南瓜派"); ZH.put("GOLDEN_APPLE", "金苹果");
        ZH.put("GLISTERING_MELON_SLICE", "闪烁的西瓜片"); ZH.put("HONEY_BOTTLE", "蜂蜜瓶"); ZH.put("GLOW_BERRIES", "发光浆果");
        // ---- 肉类与鱼 ----
        ZH.put("BEEF", "生牛肉"); ZH.put("COOKED_BEEF", "牛排"); ZH.put("PORKCHOP", "生猪排");
        ZH.put("COOKED_PORKCHOP", "熟猪排"); ZH.put("COOKED_CHICKEN", "熟鸡肉"); ZH.put("COD", "生鳕鱼");
        ZH.put("SALMON", "生鲑鱼"); ZH.put("COOKED_SALMON", "熟鲑鱼");
        // ---- 矿物与建材 ----
        ZH.put("RAW_COPPER", "粗铜"); ZH.put("IRON_NUGGET", "铁粒"); ZH.put("GOLD_NUGGET", "金粒");
        ZH.put("FLINT", "燧石"); ZH.put("CLAY_BALL", "黏土球"); ZH.put("BRICK", "红砖");
        ZH.put("NETHERITE_SCRAP", "下界合金碎片"); ZH.put("SOUL_SAND", "灵魂沙"); ZH.put("MAGMA_BLOCK", "岩浆块");
        ZH.put("BASALT", "玄武岩"); ZH.put("TUFF", "凝灰岩"); ZH.put("SAND", "沙子");
        ZH.put("GRAVEL", "砂砾");
        // ---- 原木 ----
        ZH.put("ACACIA_LOG", "金合欢原木"); ZH.put("DARK_OAK_LOG", "深色橡木原木"); ZH.put("MANGROVE_LOG", "红树林原木");
        ZH.put("CHERRY_LOG", "樱花原木"); ZH.put("CRIMSON_STEM", "绯红菌柄"); ZH.put("WARPED_STEM", "诡异菌柄");
        // ---- 怪物掉落与海洋 ----
        ZH.put("GHAST_TEAR", "恶魂之泪"); ZH.put("MAGMA_CREAM", "岩浆膏"); ZH.put("PHANTOM_MEMBRANE", "幻翼膜");
        ZH.put("TURTLE_SCUTE", "海龟鳞甲"); ZH.put("ARMADILLO_SCUTE", "犰狳鳞甲"); ZH.put("RABBIT_FOOT", "兔子脚");
        ZH.put("NAUTILUS_SHELL", "鹦鹉螺壳"); ZH.put("PRISMARINE_CRYSTALS", "海晶砂粒"); ZH.put("SPONGE", "海绵");
        // ---- 花草 ----
        ZH.put("DANDELION", "蒲公英"); ZH.put("POPPY", "虞美人"); ZH.put("BLUE_ORCHID", "兰花");
        ZH.put("CORNFLOWER", "矢车菊"); ZH.put("SUNFLOWER", "向日葵"); ZH.put("TORCHFLOWER", "火把花");
        // ---- 羊毛 ----
        ZH.put("WHITE_WOOL", "白色羊毛"); ZH.put("BLACK_WOOL", "黑色羊毛"); ZH.put("RED_WOOL", "红色羊毛");
        ZH.put("BLUE_WOOL", "蓝色羊毛");
        // ---- 杂项 ----
        ZH.put("CHARCOAL", "木炭"); ZH.put("PAPER", "纸"); ZH.put("NAME_TAG", "命名牌");
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

        rollIfNeeded(false);

        new BukkitRunnable() {
            @Override
            public void run() {
                rollIfNeeded(false);
            }
        }.runTaskTimer(this, 1200L, 1200L);

        try {
            Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            floodgateAvailable = true;
        } catch (ClassNotFoundException ex) {
            floodgateAvailable = false;
        }

        getServer().getPluginManager().registerEvents(this, this);
        getCommand("ds").setExecutor(this);
        getLogger().info("DailySell v" + getDescription().getVersion() + " 已启用（GUI + 命令"
                + (floodgateAvailable ? " + 基岩原生表单" : "") + "）");
    }

    @Override
    public void onDisable() {
        // 取消待执行的防抖写任务，强制把未刷盘的改动落盘一次，防止关服丢数据
        if (saveTask != null) {
            saveTask.cancel();
            saveTask = null;
        }
        if (dirty) saveData();
    }

    private void loadParams() {
        itemsPerDay = cfg.getInt("items-per-day", 8);
        choicesPerDay = Math.max(1, Math.min(cfg.getInt("choices-per-day", 5), itemsPerDay));
        minPerItem = cfg.getInt("min-per-item", 10);
        maxPerItem = cfg.getInt("max-per-item", 60);
        priceFloatMin = cfg.getDouble("price-float-min", 0.6);
        priceFloatMax = cfg.getDouble("price-float-max", 1.4);
        dailyBudget = cfg.getDouble("daily-budget", 900);
        broadcast = cfg.getBoolean("broadcast", true);
        pool.clear();
        if (cfg.contains("pool") && cfg.getConfigurationSection("pool") != null) {
            for (String key : cfg.getConfigurationSection("pool").getKeys(false)) {
                pool.put(key.toUpperCase(), cfg.getDouble("pool." + key));
            }
        }
    }

    /**
     * 日期变化时重新生成今日清单；同一天重启时恢复已保存清单（不刷新物品与额度）。
     * 升级兼容：旧版本每天固定 N 种且无需选择，现版本 items-per-day 更大（默认 8）。
     * 若保存的清单数量明显少于当前配置（旧版小清单残留，例如仍是 4 种），且今天尚未
     * 自动升级过，则自动重抽为本日新清单——避免玩家部署新版本后当天仍看到旧的少数量
     * 且“没有选择余地”。autoUpgraded 标记按天写入 daily.yml，防止定时任务反复重抽。
     */
    private void rollIfNeeded(boolean force) {
        String now = LocalDate.now().toString();
        String stored = daily.getString("date", "");
        boolean hasList = daily.isSet("items") && !daily.getStringList("items").isEmpty();

        // 同一天且有已保存清单：先恢复（保持当天稳定，不随意重抽）
        if (!force && now.equals(stored) && hasList) {
            loadTodayFromFile();
        }

        // 是否需要（重新）生成今日清单：
        //   1) 跨天 / 无清单 / 手动 reroll → 强制新生成；
        //   2) 升级兼容：已存清单数量 < 当前 items-per-day（旧版小清单残留），
        //      且今天尚未自动升级过 → 自动重抽，无需管理员手动 /ds reroll。
        String upgraded = daily.getString("autoUpgraded", "");
        boolean needRoll = force
                || !now.equals(stored)
                || !hasList
                || (today.size() > 0 && today.size() < itemsPerDay
                    && !now.equals(upgraded) && itemsPerDay <= pool.size());

        if (!needRoll) return;

        List<Entry> rolled = roll();
        if (rolled == null) {
            getLogger().warning("物品池为空或不足，无法生成今日清单");
            return;
        }
        today.clear();
        today.addAll(rolled);
        todayDate = now;
        daily.set("date", now);
        daily.set("autoUpgraded", now);
        if (force) {
            // 手动重抽会换掉候选物品：清空当天自选记录，并关掉玩家手上过期的界面
            clearChoicesForToday();
            for (Player p : Bukkit.getOnlinePlayers()) {
                Inventory top = p.getOpenInventory().getTopInventory();
                if (top == guis.get(p.getUniqueId()) || top == chooseGuis.get(p.getUniqueId())) {
                    p.closeInventory();
                    p.sendMessage("§e今日收购清单已重新生成，请重新打开 §a/ds §e并自选 "
                            + choiceLimit() + " 种");
                }
            }
        }
        daily.set("items", today.stream()
                .map(e -> e.mat.name() + ":" + e.price + ":" + e.quota)
                .collect(Collectors.toList()));
        try { daily.save(dailyFile); } catch (IOException ex) { getLogger().severe("daily.yml 保存失败"); }
        // 跨天：清理 data.yml 中 7 天前的旧日期键，并立即落盘一次
        cleanOldDates(now);
        if (broadcast) broadcastToday();
    }

    /** 删除 players.<uuid>.<日期>.* 中早于 (今天-KEEP_DAYS+1) 的日期键，只保留最近 KEEP_DAYS 天（含当天） */
    private void cleanOldDates(String todayStr) {
        try {
            LocalDate today = LocalDate.parse(todayStr);
            Set<String> keep = new HashSet<>();
            for (int i = 0; i < KEEP_DAYS; i++) keep.add(today.minusDays(i).toString());
            cleanDateBranch("players", keep);
            cleanDateBranch("choices", keep);
            saveData();
        } catch (Throwable t) {
            getLogger().warning("清理旧日期数据失败: " + t.getMessage());
        }
    }

    /** 清理某一根段（players / choices）下 7 天前的日期键。 */
    private void cleanDateBranch(String root, Set<String> keep) {
        if (!data.isConfigurationSection(root)) return;
        for (String uuid : data.getConfigurationSection(root).getKeys(false)) {
            String uRoot = root + "." + uuid;
            if (!data.isConfigurationSection(uRoot)) continue;
            for (String dateStr : new ArrayList<>(data.getConfigurationSection(uRoot).getKeys(false))) {
                if (!keep.contains(dateStr)) data.set(uRoot + "." + dateStr, null);
            }
        }
    }

    /** 重抽当天清单时清掉所有玩家「今天」的自选记录（保留其它日期以便审计）。 */
    private void clearChoicesForToday() {
        ConfigurationSection root = data.getConfigurationSection("choices");
        if (root == null) return;
        for (String uuid : root.getKeys(false)) {
            data.set("choices." + uuid + "." + todayDate, null);
        }
        markDirty();
    }

    /** 从 daily.yml 恢复当天已生成的清单（重启不刷新物品与额度） */
    private void loadTodayFromFile() {
        today.clear();
        todayDate = daily.getString("date", "");
        for (String s : daily.getStringList("items")) {
            String[] parts = s.split(":");
            if (parts.length != 3) continue;
            Material m = Material.matchMaterial(parts[0]);
            if (m == null) continue;
            try {
                today.add(new Entry(m, Double.parseDouble(parts[1]), Integer.parseInt(parts[2])));
            } catch (NumberFormatException ignored) { }
        }
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
        sb.append("§6✦ 今日收购候选 " + today.size() + " 种（自选 " + choiceLimit() + " 种出售，总价上限 §e")
          .append(fmt(dailyBudget)).append("§6 币）: ");
        for (int i = 0; i < today.size(); i++) {
            Entry e = today.get(i);
            if (i > 0) sb.append("§7, ");
            sb.append("§e").append(zh(e.mat)).append("§7(").append(fmt(e.price)).append("币/个, 限")
              .append(e.quota).append("个)");
        }
        sb.append("  §a/ds §7打开界面自选（基岩端自动弹出表单，无需打字）");
        Bukkit.broadcastMessage(sb.toString());
    }

    private String fmt(double v) {
        if (v == Math.floor(v)) return String.valueOf((long) v);
        return String.valueOf(v);
    }

    // ---------- 数据 ----------

    // ---------- 自选清单（choices.<uuid>.<日期>，与 players 平级，避免被日期清理误删） ----------

    private String choicesKey(Player p) {
        return "choices." + p.getUniqueId() + "." + todayDate;
    }

    /** 玩家当天已选中的 Material 名；只保留仍在今日清单里的项（reroll 后自动失效）。 */
    private List<String> choices(Player p) {
        List<String> out = new ArrayList<>();
        for (String s : data.getStringList(choicesKey(p))) {
            for (Entry e : today) {
                if (e.mat.name().equals(s) && !out.contains(s)) { out.add(s); break; }
            }
        }
        return out;
    }

    private int choiceLimit() {
        return Math.min(choicesPerDay, today.size());
    }

    private boolean hasChosen(Player p) {
        return choices(p).size() >= choiceLimit();
    }

    /** 玩家自己选中的今日条目（保持今日清单顺序）。 */
    private List<Entry> myEntries(Player p) {
        List<String> chosen = choices(p);
        List<Entry> list = new ArrayList<>();
        for (Entry e : today) if (chosen.contains(e.mat.name())) list.add(e);
        return list;
    }

    private void saveChoices(Player p, List<String> list) {
        data.set(choicesKey(p), list);
        markDirty();
    }

    /** 切换第 n 种（1 起）的选中状态；返回给玩家的提示文本。 */
    private String toggleChoice(Player p, int n) {
        if (today.isEmpty()) return "§c今日收购清单未生成";
        if (n < 1 || n > today.size()) return "§c序号需在 1~" + today.size() + " 之间，先看 §a/ds today";
        Entry e = today.get(n - 1);
        List<String> list = choices(p);
        String key = e.mat.name();
        if (list.contains(key)) {
            list.remove(key);
            saveChoices(p, list);
            return "§e已取消选择 §f" + zh(e.mat) + "§7（已选 " + list.size() + "/" + choiceLimit() + "）";
        }
        if (list.size() >= choiceLimit()) {
            return "§c最多只能选 " + choiceLimit() + " 种，先取消一种（§a/ds choose <序号>§c）";
        }
        list.add(key);
        saveChoices(p, list);
        return "§a已选择 §f" + zh(e.mat) + "§7（已选 " + list.size() + "/" + choiceLimit() + "）";
    }

    private String soldKey(Player p, Material m) {
        return "players." + p.getUniqueId() + "." + todayDate + "." + m.name();
    }

    private int soldCount(Player p, Material m) {
        return data.getInt(soldKey(p, m), 0);
    }

    private Entry findEntry(Player p, Material m) {
        for (Entry e : myEntries(p)) if (e.mat == m) return e;
        return null;
    }

    /** 当天收益累计键：与自选清单解耦，重选清单不会重置每日收益上限。 */
    private String earnedKey(Player p) {
        return "players." + p.getUniqueId() + "." + todayDate + ".__earned";
    }

    /**
     * 玩家当天已获总收益。优先读累计字段；旧 data.yml 没有该字段时按当天
     * 「全部已售物品」回放，而不是只看当前选中的 5 种，避免 /ds choose clear
     * 重选后把已赚金额算丢、从而绕过 daily-budget。
     */
    private double earnedToday(Player p) {
        if (data.isSet(earnedKey(p))) return data.getDouble(earnedKey(p), 0D);
        ConfigurationSection sec = data.getConfigurationSection(
                "players." + p.getUniqueId() + "." + todayDate);
        if (sec == null) return 0D;
        double sum = 0D;
        for (String mat : sec.getKeys(false)) {
            if (mat.startsWith("__")) continue;
            int n = sec.getInt(mat, 0);
            if (n <= 0) continue;
            for (Entry e : today) {
                if (e.mat.name().equals(mat)) { sum += n * e.price; break; }
            }
        }
        return sum;
    }

    /** 出售成功后同时写已售数量与累计收益。 */
    private void recordSale(Player p, Material mat, int newSold, double income) {
        // 必须在写新数量之前结算：旧 data.yml 没有 __earned 时 earnedToday() 会回放
        // 全部已售数量，若先写数量会把本次收益重复计入。
        double earned = earnedToday(p) + income;
        data.set(soldKey(p, mat), newSold);
        data.set(earnedKey(p), earned);
        markDirty();
    }

    private void saveData() {
        try {
            data.save(dataFile);
            dirty = false;
        } catch (Throwable t) {
            // 保存失败只记日志，绝不让主线程抛异常
            getLogger().severe("data.yml 保存失败: " + t.getMessage());
        }
    }

    /** data.set(...) 后调用：置脏并延迟合并写盘；100 ticks 内的多次卖出只触发一次写 */
    private void markDirty() {
        dirty = true;
        if (saveTask == null) {
            saveTask = getServer().getScheduler().runTaskLater(this, (Runnable) () -> {
                saveTask = null;
                if (dirty) saveData();
            }, SAVE_DELAY_TICKS);
        }
    }

    // ---------- GUI（27 格美化 + 多页） ----------

    private ItemStack glass(Material m, String name) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(name == null ? " " : name);
        it.setItemMeta(meta);
        return it;
    }

    // ---------- 自选界面（Java 箱子 GUI；基岩端走原生表单，见 openBedrockChoose） ----------

    private void openChooseGui(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, "§6✦ 每日收购 · 自选 " + choiceLimit() + " 种");
        refreshChooseGui(p, inv);
        chooseGuis.put(p.getUniqueId(), inv);
        p.openInventory(inv);
    }

    private void refreshChooseGui(Player p, Inventory inv) {
        inv.clear();
        ItemStack bg = glass(Material.BLACK_STAINED_GLASS_PANE, null);
        ItemStack bar = glass(Material.GRAY_STAINED_GLASS_PANE, null);
        for (int i = 0; i < 27; i++) inv.setItem(i, bg);
        for (int i = 18; i < 27; i++) inv.setItem(i, bar);

        List<String> chosen = choices(p);
        int shown = Math.min(today.size(), 8);
        for (int i = 0; i < shown; i++) {
            Entry e = today.get(i);
            boolean sel = chosen.contains(e.mat.name());
            ItemStack icon = new ItemStack(e.mat, 1);
            ItemMeta meta = icon.getItemMeta();
            meta.setDisplayName((sel ? "§a✔ " : "§7") + zh(e.mat) + " §8[" + (i + 1) + "]");
            List<String> lore = new ArrayList<>();
            lore.add("§7单价 §a" + fmt(e.price) + " §7币/个 · 额度 §e" + e.quota + " §7个");
            lore.add(sel ? "§a✔ 已选择（点击取消）" : "§7✘ 未选择（点击选中）");
            lore.add("§8也可输入 /ds choose " + (i + 1));
            meta.setLore(lore);
            if (sel) {
                meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            }
            icon.setItemMeta(meta);
            inv.setItem(10 + i, icon);
        }

        ItemStack info = new ItemStack(Material.GOLD_NUGGET);
        ItemMeta im = info.getItemMeta();
        im.setDisplayName("§6✦ 今日自选 " + chosen.size() + " / " + choiceLimit());
        im.setLore(Arrays.asList(
                "§7从今日 " + today.size() + " 种候选里选 " + choiceLimit() + " 种出售",
                "§7选择按日期保存，重登 / 重启不变，跨天需重选",
                "§7已卖额度不会因为重选而重置"));
        info.setItemMeta(im);
        inv.setItem(22, info);

        boolean full = chosen.size() >= choiceLimit();
        ItemStack ok = new ItemStack(full ? Material.EMERALD_BLOCK : Material.BARRIER);
        ItemMeta om = ok.getItemMeta();
        om.setDisplayName(full ? "§a✔ 确认选择并打开收购界面"
                : "§c还需选择 " + (choiceLimit() - chosen.size()) + " 种");
        ok.setItemMeta(om);
        inv.setItem(24, ok);

        ItemStack clear = new ItemStack(Material.BUCKET);
        ItemMeta cm = clear.getItemMeta();
        cm.setDisplayName("§e清空重选");
        cm.setLore(Arrays.asList("§7已卖出的额度不会重置"));
        clear.setItemMeta(cm);
        inv.setItem(18, clear);

        ItemStack help = new ItemStack(Material.PAPER);
        ItemMeta hm = help.getItemMeta();
        hm.setDisplayName("§7❓ 玩法说明");
        hm.setLore(Arrays.asList(
                "§7今日候选全服共享 " + today.size() + " 种",
                "§7每人自选 " + choiceLimit() + " 种作为自己的可卖清单",
                "§7基岩端用 §a/ds §7自动弹表单选择（也可 /ds choose <序号>）",
                "§7选好后 §a/ds §7或手持物品 §a/ds sell"));
        help.setItemMeta(hm);
        inv.setItem(26, help);
    }

    private void openGui(Player p) {
        if (today.isEmpty()) { p.sendMessage("§c今日收购清单未生成"); return; }
        // 基岩玩家走原生表单（Geyser 箱子 GUI 不可靠，且触屏点格子体验差）
        if (isBedrock(p)) {
            if (hasChosen(p)) {
                openBedrockSell(p);
            } else {
                openBedrockChoose(p);
            }
            return;
        }
        if (!hasChosen(p)) { openChooseGui(p); return; }
        guiPages.put(p.getUniqueId(), 1);
        Inventory inv = Bukkit.createInventory(null, 27, "§6✦ 每日收购 · 今日");
        refreshGui(p, inv);
        guis.put(p.getUniqueId(), inv);
        p.openInventory(inv);
    }

    private void refreshGui(Player p, Inventory inv) {
        inv.clear();
        List<Entry> my = myEntries(p);
        int page = Math.max(1, guiPages.getOrDefault(p.getUniqueId(), 1));
        int totalPages = Math.max(1, (int) Math.ceil(my.size() / (double) PER_PAGE));
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
            if (idx < my.size()) {
                inv.setItem(i, buildIcon(p, my.get(idx)));
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
                "§7额度用完或达上限即收摊",
                "§7今日自选 " + my.size() + "/" + choiceLimit() + " 种"));
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
            meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        }
        icon.setItemMeta(meta);
        return icon;
    }

    /** 关闭界面后释放 Inventory 引用，避免长期在线的服务器常驻内存。 */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player)) return;
        UUID uuid = e.getPlayer().getUniqueId();
        Inventory top = e.getView().getTopInventory();
        if (top != null && top == chooseGuis.get(uuid)) chooseGuis.remove(uuid);
        if (top != null && top == guis.get(uuid)) {
            guis.remove(uuid);
            guiPages.remove(uuid);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        chooseGuis.remove(uuid);
        guis.remove(uuid);
        guiPages.remove(uuid);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        // 只接管插件 GUI 顶栏：玩家背包/快捷栏点击一律放行，
        // 避免整理背包被取消、或点到背包里的物品误触发选择/出售。
        if (e.getRawSlot() >= e.getInventory().getSize()) return;
        Player p = (Player) e.getWhoClicked();
        Inventory chooser = chooseGuis.get(p.getUniqueId());
        if (chooser != null && e.getInventory() == chooser) {
            e.setCancelled(true);
            ItemStack picked = e.getCurrentItem();
            if (picked == null || picked.getType() == Material.AIR) return;
            int cs = e.getSlot();
            if (cs >= 10 && cs < 10 + Math.min(today.size(), 8)) {
                p.sendMessage(toggleChoice(p, cs - 9));
                refreshChooseGui(p, chooser);
                p.playSound(p.getLocation(), "ui.button.click", 0.6f, 1.2f);
                return;
            }
            if (cs == 18) {
                saveChoices(p, new ArrayList<>());
                refreshChooseGui(p, chooser);
                p.sendMessage("§e已清空今日选择");
                return;
            }
            if (cs == 24) {
                if (!hasChosen(p)) {
                    p.sendMessage("§c还需选择 " + (choiceLimit() - choices(p).size()) + " 种");
                    return;
                }
                chooseGuis.remove(p.getUniqueId());
                Bukkit.getScheduler().runTask(this, () -> { p.closeInventory(); openGui(p); });
            }
            return;
        }
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

        Entry entry = findEntry(p, cur.getType());
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
        recordSale(p, entry.mat, sold + amt, entry.price * amt);
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

    // ---------- 基岩端原生表单（Floodgate / Cumulus） ----------
    // Geyser 的箱子 GUI 不可靠（拖出物品 / 高亮异常 / 触屏只能左键），
    // 基岩玩家 /ds 直接发原生表单：未选满 → CustomForm 开关自选；
    // 选满 → SimpleForm 逐项一键出售（把背包该物品卖到额度/预算上限）。
    // 注意：Cumulus 回调在 Netty 网络线程触发，所有 Bukkit 操作必须
    // 通过 Bukkit.getScheduler().runTask 切回主线程，否则会崩服。

    /** 判断玩家是否基岩版（Floodgate 认证）。未装 Floodgate 恒 false。 */
    private boolean isBedrock(Player p) {
        if (!floodgateAvailable) return false;
        try {
            return FloodgateApi.getInstance().isFloodgatePlayer(p.getUniqueId());
        } catch (NoClassDefFoundError | ExceptionInInitializerError | IllegalStateException ex) {
            return false;
        }
    }

    private boolean sendForm(Player p, SimpleForm.Builder builder) {
        try {
            FloodgateApi api = FloodgateApi.getInstance();
            FloodgatePlayer fp = api.getPlayer(p.getUniqueId());
            if (fp == null) return false;
            return fp.sendForm(builder);
        } catch (NoClassDefFoundError | ExceptionInInitializerError ex) {
            return false;
        }
    }

    private boolean sendForm(Player p, CustomForm.Builder builder) {
        try {
            FloodgateApi api = FloodgateApi.getInstance();
            FloodgatePlayer fp = api.getPlayer(p.getUniqueId());
            if (fp == null) return false;
            return fp.sendForm(builder);
        } catch (NoClassDefFoundError | ExceptionInInitializerError ex) {
            return false;
        }
    }

    /** 基岩端自选表单：每种候选一个开关，提交时必须恰好打开 choiceLimit 个。 */
    private void openBedrockChoose(Player p) {
        if (today.isEmpty()) { p.sendMessage("§c今日收购清单未生成"); return; }
        final int limit = choiceLimit();
        // 快照：回调在 Netty 线程触发，避免零点重抽瞬间读取被修改的 today 列表
        final List<Entry> snapshot = new ArrayList<>(today);
        List<String> chosen = choices(p);
        CustomForm.Builder b = CustomForm.builder()
                .title("每日收购 · 自选 " + limit + " 种");
        b.label("今日候选 " + snapshot.size() + " 种（全服共享）。打开恰好 "
                + limit + " 个开关后点提交。已选 " + chosen.size() + "/" + limit
                + "，选择按日期保存，已卖额度不会因重选重置。");
        final int n = snapshot.size();
        for (int i = 0; i < n; i++) {
            Entry e = snapshot.get(i);
            int sold = soldCount(p, e.mat);
            boolean def = chosen.contains(e.mat.name());
            String text = zh(e.mat) + "：" + fmt(e.price) + " 币/个 · 额度 " + e.quota
                    + (sold > 0 ? "（已卖 " + sold + "）" : "");
            b.toggle(text, def);
        }
        b.validResultHandler((CustomFormResponse resp) -> {
            List<String> picked = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                try {
                    if (resp.getToggle(i)) picked.add(snapshot.get(i).mat.name());
                } catch (RuntimeException ignored) {
                    // 响应结构与表单不一致时跳过该项
                }
            }
            // Netty 线程 → 主线程
            Bukkit.getScheduler().runTask(this, () -> {
                if (!p.isOnline()) return;
                if (picked.size() != limit) {
                    p.sendMessage("§c需恰好选择 " + limit + " 种，当前 "
                            + picked.size() + " 种，已重新打开表单请重选");
                    openBedrockChoose(p);
                    return;
                }
                saveChoices(p, picked);
                p.sendMessage("§a已选好今日 " + limit + " 种收购物品");
                p.playSound(p.getLocation(), "entity.experience_orb.pickup", 0.6f, 1.4f);
                openBedrockSell(p);
            });
        });
        if (!sendForm(p, b)) {
            // 表单发送失败（理论仅在 Floodgate 异常时发生）：兜底回箱子 GUI
            openChooseGui(p);
        }
    }

    /** 基岩端出售表单：每种选中物品一个按钮，点击把背包里该物品卖到额度/预算上限。 */
    private void openBedrockSell(Player p) {
        final List<Entry> my = myEntries(p);
        if (my.isEmpty()) { openBedrockChoose(p); return; }
        double earned = earnedToday(p);
        SimpleForm.Builder b = SimpleForm.builder()
                .title("每日收购 · 出售");
        b.content("今日已赚 " + fmt(earned) + " / " + fmt(dailyBudget) + " 南瓜币\n"
                + "点击物品：把背包里该物品卖到额度/预算上限");
        final int cnt = my.size();
        for (Entry e : my) {
            int sold = soldCount(p, e.mat);
            int remain = Math.max(0, e.quota - sold);
            String label = remain > 0
                    ? zh(e.mat) + " · 剩 " + remain + "/" + e.quota + " · " + fmt(e.price) + " 币/个"
                    : zh(e.mat) + " · 今日额度已用完";
            b.button(label);
        }
        b.button("重新自选（已赚金额与已卖额度不变）");
        b.validResultHandler((SimpleFormResponse resp) -> {
            int idx = resp.clickedButtonId();
            // Netty 线程 → 主线程
            Bukkit.getScheduler().runTask(this, () -> {
                if (!p.isOnline()) return;
                if (idx >= 0 && idx < cnt) {
                    bedrockSell(p, my.get(idx));
                    if (p.isOnline()) openBedrockSell(p); // 刷新表单继续卖
                } else if (idx == cnt) {
                    saveChoices(p, new ArrayList<>());
                    p.sendMessage("§e已清空今日选择（已卖额度与已赚金额不变）");
                    openBedrockChoose(p);
                }
            });
        });
        sendForm(p, b);
    }

    /** 基岩表单出售：整个背包统计该物品，卖到「剩余额度 / 剩余预算 / 持有量」的最小值。 */
    private void bedrockSell(Player p, Entry entry) {
        int sold = soldCount(p, entry.mat);
        int remainQuota = entry.quota - sold;
        if (remainQuota <= 0) {
            p.sendMessage("§c今日「" + zh(entry.mat) + "」额度已用完");
            p.playSound(p.getLocation(), "entity.villager.no", 0.6f, 1.0f);
            return;
        }
        double budgetLeft = dailyBudget - earnedToday(p);
        int budgetAllow = budgetLeft >= entry.price ? (int) Math.floor(budgetLeft / entry.price) : 0;
        if (budgetAllow <= 0) {
            p.sendMessage("§c今日收购总收益已达上限（" + fmt(dailyBudget) + " 币）");
            p.playSound(p.getLocation(), "entity.villager.no", 0.6f, 1.0f);
            return;
        }
        int amt = Math.min(Math.min(remainQuota, budgetAllow), countHeld(p, entry.mat));
        if (amt <= 0) {
            p.sendMessage("§c背包里没有「" + zh(entry.mat) + "」可出售");
            return;
        }
        removeItems(p, entry.mat, amt);
        econ.depositPlayer(p, entry.price * amt);
        recordSale(p, entry.mat, sold + amt, entry.price * amt);
        p.sendMessage("§a已出售 §e" + zh(entry.mat) + "§a ×" + amt + "，获得 §e"
                + fmt(entry.price * amt) + "§a 南瓜币（额度剩 §e" + (entry.quota - sold - amt) + "§a 个）");
        p.playSound(p.getLocation(), "entity.experience_orb.pickup", 0.6f, 1.4f);
    }

    // ---------- 命令 ----------

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String sub = args.length > 0 ? args[0].toLowerCase() : "gui";
        if (!(sender instanceof Player) && !sub.equals("reload") && !sub.equals("reroll")
                && !sub.equals("today")) {
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
            case "choose":
            case "pick":
                chooseCommand(sender, args);
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
                sender.sendMessage("§6用法: §e/ds §7打开界面/自选 §7|§e /ds choose <1-" + today.size() + "|clear> §7|§e /ds sell §7|§e sellall §7|§e today §7|§e reload §7|§e reroll");
                break;
        }
        return true;
    }

    private void chooseCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("此命令需在游戏内执行"); return; }
        Player p = (Player) sender;
        if (today.isEmpty()) { p.sendMessage("§c今日收购清单未生成"); return; }
        if (args.length < 2) {
            if (isBedrock(p)) {
                // 基岩端统一走原生表单（与 /ds 相同入口）
                if (hasChosen(p)) openBedrockSell(p);
                else openBedrockChoose(p);
                return;
            }
            if (hasChosen(p)) {
                p.sendMessage("§a你今天已选好 " + choiceLimit() + " 种，正在打开收购界面");
                openGui(p);
            } else {
                openChooseGui(p);
            }
            return;
        }
        if (args[1].equalsIgnoreCase("clear")) {
            saveChoices(p, new ArrayList<>());
            p.sendMessage("§e已清空今日选择，可重新挑选（已卖额度不会重置）");
            Inventory chooser = chooseGuis.get(p.getUniqueId());
            if (chooser != null) refreshChooseGui(p, chooser);
            return;
        }
        int n;
        try {
            n = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            p.sendMessage("§c用法：§e/ds choose <1-" + today.size() + "§c|§eclear>");
            return;
        }
        p.sendMessage(toggleChoice(p, n));
        Inventory chooser = chooseGuis.get(p.getUniqueId());
        if (chooser != null) refreshChooseGui(p, chooser);
        if (hasChosen(p)) p.sendMessage("§a已选满，输入 §e/ds §a打开收购界面开始出售");
    }

    private void sellHand(Player p) {
        if (today.isEmpty()) { p.sendMessage("§c今日收购清单未生成"); return; }
        if (!hasChosen(p)) {
            p.sendMessage("§c还没选好今天的 " + choiceLimit() + " 种收购物品，先 §a/ds choose §c或 §a/ds choose <序号>");
            return;
        }
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) {
            p.sendMessage("§c请手持要出售的物品（输入 §a/ds §7打开收购界面）");
            return;
        }
        Entry entry = findEntry(p, hand.getType());
        if (entry == null) {
            p.sendMessage("§c「" + zh(hand.getType()) + "」不在你今天选中的 " + choiceLimit() + " 种里，清单见 §a/ds today");
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
        recordSale(p, entry.mat, sold + 1, entry.price);
        p.sendMessage("§a已出售 §e" + zh(entry.mat) + "§a ×1，获得 §e" + fmt(entry.price)
                + "§a 南瓜币（额度剩 §e" + (entry.quota - sold - 1) + "§a 个）");
    }

    private void sellAll(Player p) {
        if (today.isEmpty()) { p.sendMessage("§c今日收购清单未生成"); return; }
        if (!hasChosen(p)) {
            p.sendMessage("§c还没选好今天的 " + choiceLimit() + " 种收购物品，先 §a/ds choose §c或 §a/ds choose <序号>");
            return;
        }
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) {
            p.sendMessage("§c请手持要出售的物品");
            return;
        }
        Entry entry = findEntry(p, hand.getType());
        if (entry == null) {
            p.sendMessage("§c「" + zh(hand.getType()) + "」不在你今天选中的 " + choiceLimit() + " 种里");
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
        recordSale(p, entry.mat, sold + amount, entry.price * amount);
        p.sendMessage("§a已出售 §e" + zh(entry.mat) + "§a ×" + amount + "，获得 §e"
                + fmt(entry.price * amount) + "§a 南瓜币（额度剩 §e" + (entry.quota - sold - amount) + "§a 个）");
    }

    private void showToday(CommandSender sender) {
        if (today.isEmpty()) { sender.sendMessage("§c今日收购清单未生成"); return; }
        List<String> chosen = sender instanceof Player ? choices((Player) sender) : new ArrayList<>();
        sender.sendMessage("§6======== 今日候选 " + today.size() + " 种 · 自选 " + choiceLimit() + " 种 ========");
        double total = 0;
        for (int i = 0; i < today.size(); i++) {
            Entry e = today.get(i);
            boolean sel = chosen.contains(e.mat.name());
            String extra = "";
            if (sender instanceof Player) {
                int sold = soldCount((Player) sender, e.mat);
                extra = " §7(我已卖 " + sold + "/" + e.quota + ")";
            }
            sender.sendMessage("§7[" + (i + 1) + "] " + (sel ? "§a✔ " : "§7  ")
                    + "§e" + zh(e.mat) + " §7→ §a" + fmt(e.price) + "§7 币/个"
                    + " §7额度" + e.quota + "个" + extra);
            total += e.price * e.quota;
        }
        sender.sendMessage("§7清单总价上限: §a" + fmt(dailyBudget) + "§7 币（本次候选合计 " + fmt(total) + " 币）");
        if (sender instanceof Player) {
            sender.sendMessage("§7今日已赚 §a" + fmt(earnedToday((Player) sender))
                    + "§7/§a" + fmt(dailyBudget) + " §7币（重选清单不会重置）");
            sender.sendMessage("§7已选 §a" + chosen.size() + "§7/§a" + choiceLimit()
                    + " §7| §e/ds choose <1-" + today.size() + "> §7选/取消，§e/ds choose clear §7清空");
            if (!hasChosen((Player) sender)) sender.sendMessage("§c还没选满，选好才能出售");
        }
        sender.sendMessage("§7出售: §a/ds §7打开界面，或手持物品 §a/ds sell §7(卖1个) / §a/ds sellall §7(卖满额度)");
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
