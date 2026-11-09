package com.nangua.pumpkinfly;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

/** Vault 经济接入；南瓜币余额真源是 Essentials userdata，插件只做扣款，不自己记余额。 */
public final class EconomyHook {

    private final PumpkinFlyPlugin plugin;
    private Economy economy;

    public EconomyHook(PumpkinFlyPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean hook() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("未找到 Vault，购买功能不可用。");
            return false;
        }
        RegisteredServiceProvider<Economy> rsp =
                plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().warning("Vault 已安装但没有 Economy 提供器（EssentialsX 未启用经济？），购买功能不可用。");
            return false;
        }
        economy = rsp.getProvider();
        plugin.getLogger().info("经济提供器：" + economy.getName());
        return true;
    }

    public boolean isReady() {
        return economy != null && economy.isEnabled();
    }

    public double balance(OfflinePlayer p) {
        return isReady() ? economy.getBalance(p) : 0D;
    }

    public boolean has(OfflinePlayer p, double amount) {
        return isReady() && economy.has(p, amount);
    }

    public EconomyResponse withdraw(OfflinePlayer p, double amount) {
        return economy.withdrawPlayer(p, amount);
    }

    public String format(double amount) {
        return isReady() ? economy.format(amount) : String.valueOf((long) amount);
    }
}
