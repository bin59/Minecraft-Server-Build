package com.plantop;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * PAPI 扩展：%ptop_<N>_name% / %ptop_<N>_displayname% / %ptop_<N>_time%
 * 附加：%ptop_updated% 最后成功刷新时间；%ptop_error% 最近一次错误（空=ok）
 */
public class PlanTopExpansion extends PlaceholderExpansion {

    private final PlanTopPlugin plugin;

    public PlanTopExpansion(PlanTopPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "ptop";
    }

    @Override
    public String getAuthor() {
        return "hh";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (params == null) {
            return null;
        }
        if (params.equalsIgnoreCase("updated")) {
            long t = plugin.getCache().lastSuccess();
            return t == 0L ? "never" : new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(t));
        }
        if (params.equalsIgnoreCase("error")) {
            String e = plugin.getCache().lastError();
            return e.isEmpty() ? "ok" : e;
        }
        int idx = params.indexOf('_');
        if (idx <= 0) {
            return null;
        }
        int rank;
        try {
            rank = Integer.parseInt(params.substring(0, idx));
        } catch (NumberFormatException ex) {
            return null;
        }
        String field = params.substring(idx + 1);
        if (!field.equals("name") && !field.equals("displayname") && !field.equals("time")) {
            return null;
        }
        return plugin.getCache().get(rank, field);
    }
}
