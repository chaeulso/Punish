package com.chaeulso.punish.util;

import com.chaeulso.punish.PunishPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.concurrent.TimeUnit;

/**
 * Cross-platform Folia-ready task scheduler wrapper for the Punish plugin.
 */
public class AuthScheduler {
    private final PunishPlugin plugin;
    private static Boolean folia = null;

    public AuthScheduler(PunishPlugin plugin) {
        this.plugin = plugin;
    }

    public static boolean isFolia() {
        if (folia == null) {
            try {
                Class.forName("io.papermc.paper.threadedregions.RegionScheduler");
                folia = true;
            } catch (ClassNotFoundException e) {
                folia = false;
            }
        }
        return folia;
    }

    public void runTaskLater(Runnable runnable, long delayTicks) {
        if (isFolia()) {
            Bukkit.getAsyncScheduler().runDelayed(plugin, task -> runnable.run(), delayTicks * 50, TimeUnit.MILLISECONDS);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, runnable, delayTicks);
        }
    }

    public void runTaskLater(Player player, Runnable runnable, long delayTicks) {
        if (isFolia()) {
            player.getScheduler().runDelayed(plugin, task -> runnable.run(), runnable, delayTicks);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, runnable, delayTicks);
        }
    }

    public void runTask(Runnable runnable) {
        if (isFolia()) {
            Bukkit.getGlobalRegionScheduler().run(plugin, task -> runnable.run());
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    public void runTask(Player player, Runnable runnable) {
        if (isFolia()) {
            player.getScheduler().run(plugin, task -> runnable.run(), null);
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    public void runTaskAsynchronously(Runnable runnable) {
        if (isFolia()) {
            Bukkit.getAsyncScheduler().runNow(plugin, task -> runnable.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
        }
    }

    public int runTaskTimer(Runnable runnable, long delayTicks, long periodTicks) {
        if (isFolia()) {
            Bukkit.getAsyncScheduler().runAtFixedRate(plugin, task -> runnable.run(), delayTicks * 50, periodTicks * 50, TimeUnit.MILLISECONDS);
            return 99999;
        } else {
            return Bukkit.getScheduler().runTaskTimer(plugin, runnable, delayTicks, periodTicks).getTaskId();
        }
    }
}
