package com.nangua.moneyledger;

import com.nangua.moneyledger.cmd.MoneyLedgerCommand;
import com.nangua.moneyledger.ledger.Category;
import com.nangua.moneyledger.ledger.LedgerEntry;
import com.nangua.moneyledger.ledger.LedgerStore;
import com.nangua.moneyledger.ledger.SourceDetector;
import net.ess3.api.events.UserBalanceUpdateEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.math.BigDecimal;

/**
 * 南瓜账本 MoneyLedger
 * 监听 EssentialsX 余额变动事件，把所有南瓜币变动按类别（转账/买卖/拍卖/收购/税/任务/活动）记入 SQLite。
 */
public final class MoneyLedger extends JavaPlugin implements Listener {

    private static MoneyLedger instance;
    private LedgerStore store;

    public static MoneyLedger get() {
        return instance;
    }

    public LedgerStore getStore() {
        return store;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        store = new LedgerStore(new File(getDataFolder(), "ledger.db"));
        store.open();
        getServer().getPluginManager().registerEvents(this, this);
        MoneyLedgerCommand exec = new MoneyLedgerCommand(this);
        getCommand("moneyledger").setExecutor(exec);
        getCommand("moneyledger").setTabCompleter(exec);
        getLogger().info("南瓜账本 MoneyLedger 已启用，开始按类别记录南瓜币变动");
    }

    @Override
    public void onDisable() {
        if (store != null) store.close();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBalanceUpdate(UserBalanceUpdateEvent e) {
        try {
            BigDecimal oldB = e.getOldBalance();
            BigDecimal newB = e.getNewBalance();
            if (oldB == null || newB == null) return;
            double delta = newB.doubleValue() - oldB.doubleValue();
            if (delta == 0) return;

            UserBalanceUpdateEvent.Cause cause = e.getCause();
            Category cat = SourceDetector.categorize(cause);
            String src = SourceDetector.detectSource();

            store.insert(new LedgerEntry(
                    e.getPlayer().getUniqueId().toString(),
                    e.getPlayer().getName(),
                    System.currentTimeMillis(),
                    oldB.doubleValue(),
                    newB.doubleValue(),
                    delta,
                    cause == null ? "" : cause.name(),
                    cat.name(),
                    src));
        } catch (Throwable t) {
            // 任何异常都不能影响余额变动主流程
        }
    }
}
