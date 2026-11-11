package com.nangua.moneyledger;

import com.nangua.moneyledger.cmd.MoneyLedgerCommand;
import com.nangua.moneyledger.ledger.Category;
import com.nangua.moneyledger.ledger.LedgerEntry;
import com.nangua.moneyledger.ledger.LedgerStore;
import com.nangua.moneyledger.ledger.SourceDetector;
import com.nangua.moneyledger.ledger.TradeLogBackfiller;
import net.ess3.api.events.UserBalanceUpdateEvent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;

/**
 * 南瓜账本 MoneyLedger 1.1.0
 *
 * 监听 EssentialsX 余额变动事件记录所有南瓜币变动，并用 EssentialsX 自己的
 * trade.log 做「第二数据源」幂等回填，补上事件链路漏掉的那些笔数。
 *
 * 1.1.0 准确性修复：
 *  - setReason(null) 现在真的会清除备注（旧版不清除，备注会串到下一笔，污染来源）。
 *  - 命令发放记录真实执行者与命令文本，不再显示 Brigadier 的 context/ContextChain。
 *  - 金额用 BigDecimal 精确文本落库，避免 0.18000000000000682 这类浮点脏值。
 *  - 玩家名优先从 Essentials userdata 按 UUID 反查，基岩玩家不再退化成 UUID 前 8 位。
 *  - 写库异常不再被空 catch 吞掉（同步落盘 + 失败转存重放 + 明确日志）。
 */
public final class MoneyLedger extends JavaPlugin implements Listener {

    private static MoneyLedger instance;
    private LedgerStore store;
    private TradeLogBackfiller backfiller;
    private File userdataDir;

    /** 线程级「备注来源」：发奖方在执行 eco give 前设置，账本在同一线程写记录时用作 source。 */
    private static final ThreadLocal<String> REASON = new ThreadLocal<>();
    /** 线程级最近一条命令（带时间戳，超时自动失效，避免串味）。 */
    private static final ThreadLocal<CmdCtx> COMMAND = new ThreadLocal<>();

    private record CmdCtx(String text, long at) { }

    public static MoneyLedger get() { return instance; }

    /** 设置本线程下一笔余额变动记录的来源名；传 null / 空串表示清除（修复旧版无法清除的问题）。 */
    public static void setReason(String reason) {
        if (reason == null || reason.isEmpty()) REASON.remove();
        else REASON.set(reason);
    }

    public LedgerStore getStore() { return store; }
    public TradeLogBackfiller getBackfiller() { return backfiller; }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        File essDir = new File(getDataFolder().getParentFile(), "Essentials");
        userdataDir = new File(essDir, "userdata");
        store = new LedgerStore(new File(getDataFolder(), "ledger.db"), getLogger());
        store.open();
        backfiller = new TradeLogBackfiller(new File(essDir, "trade.log"), userdataDir, store, getLogger(),
                getConfig().getString("backfill.min-date", ""));
        getServer().getPluginManager().registerEvents(this, this);
        MoneyLedgerCommand exec = new MoneyLedgerCommand(this);
        getCommand("moneyledger").setExecutor(exec);
        getCommand("moneyledger").setTabCompleter(exec);
        scheduleBackfill();
        getLogger().info("南瓜账本 MoneyLedger 1.1.0 已启用：事件同步落盘 + trade.log 幂等回填");
    }

    @Override
    public void onDisable() {
        if (store != null) store.close();
    }

    private void scheduleBackfill() {
        if (!getConfig().getBoolean("backfill.enabled", true)) return;
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            TradeLogBackfiller.Result r = backfiller.run(false);
            getLogger().info("[MoneyLedger] trade.log 回填: " + r);
        });
        long minutes = Math.max(1, getConfig().getLong("backfill.interval-minutes", 15));
        long ticks = minutes * 60L * 20L;
        Bukkit.getScheduler().runTaskTimerAsynchronously(this,
                () -> backfiller.run(false), ticks, ticks);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent e) {
        COMMAND.set(new CmdCtx(e.getPlayer().getName() + ": " + e.getMessage(), System.currentTimeMillis()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsoleCommand(ServerCommandEvent e) {
        COMMAND.set(new CmdCtx(e.getSender().getName() + ": " + e.getCommand(), System.currentTimeMillis()));
    }

    private String commandContext() {
        CmdCtx c = COMMAND.get();
        COMMAND.remove();
        if (c == null) return null;
        long ttl = Math.max(1, getConfig().getLong("command-context-ttl-seconds", 5)) * 1000L;
        return System.currentTimeMillis() - c.at() <= ttl ? c.text() : null;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBalanceUpdate(UserBalanceUpdateEvent e) {
        try {
            BigDecimal oldB = e.getOldBalance();
            BigDecimal newB = e.getNewBalance();
            if (oldB == null || newB == null) return;
            BigDecimal deltaB = newB.subtract(oldB);
            if (deltaB.signum() == 0) return;

            UserBalanceUpdateEvent.Cause cause = e.getCause();
            SourceDetector.Detection d = SourceDetector.detect();
            Category cat = SourceDetector.categorize(cause, d);
            String reason = REASON.get();
            REASON.remove();
            String src = SourceDetector.describe(d, reason, commandContext());

            UUID uuid = e.getPlayer().getUniqueId();
            String hint = e.getPlayer().getName();
            String name = resolveName(uuid, hint);

            store.insert(new LedgerEntry(
                    uuid.toString(), name, System.currentTimeMillis(),
                    oldB.doubleValue(), newB.doubleValue(), deltaB.doubleValue(),
                    cause == null ? "" : cause.name(), cat.name(), src,
                    oldB.toPlainString(), newB.toPlainString(), deltaB.toPlainString(),
                    "EVENT"));
        } catch (Throwable t) {
            // 绝不能静默吞掉：账本少一笔就等于钱对不上
            getLogger().severe("[MoneyLedger] 记录余额变动失败（请检查该笔是否已补记）: " + t);
            t.printStackTrace();
        }
    }

    /** 优先用事件里的名字；拿不到就从 Essentials userdata 按 UUID 反查真实账号名。 */
    private String resolveName(UUID uuid, String hint) {
        if (hint != null && !hint.isBlank() && !isUuidPrefix(hint)) return hint;
        File f = new File(userdataDir, uuid + ".yml");
        if (f.isFile()) {
            try {
                for (String line : Files.readAllLines(f.toPath(), StandardCharsets.UTF_8)) {
                    if (line.startsWith("last-account-name:")) {
                        String v = line.substring("last-account-name:".length()).replace("\"", "").trim();
                        if (!v.isEmpty()) return v;
                        break;
                    }
                }
            } catch (Exception ignored) { }
        }
        try {
            OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            if (op.getName() != null && !op.getName().isEmpty()) return op.getName();
        } catch (Throwable ignored) { }
        return uuid.toString().substring(0, 8);
    }

    private static boolean isUuidPrefix(String s) {
        if (s == null || s.length() != 8) return false;
        return s.matches("[0-9a-fA-F]{8}");
    }
}
