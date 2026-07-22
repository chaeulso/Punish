package com.chaeulso.punish;

import com.chaeulso.punish.config.ConfigManager;
import com.chaeulso.punish.database.DatabaseManager;
import com.chaeulso.punish.punishment.PunishmentManager;
import com.chaeulso.punish.commands.CommandManager;
import com.chaeulso.punish.gui.GUIManager;
import com.chaeulso.punish.listeners.PlayerRestrictionListener;
import com.chaeulso.punish.util.AuthScheduler;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for Punish v26.0.1.
 * Created by Chaeulso.
 */
public final class PunishPlugin extends JavaPlugin {
    private static PunishPlugin instance;

    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private PunishmentManager punishmentManager;
    private GUIManager guiManager;
    private AuthScheduler authScheduler;

    @Override
    public void onEnable() {
        instance = this;

        // Print ASCII Banner
        printBanner();

        // 1. Initialize Schedulers
        this.authScheduler = new AuthScheduler(this);

        // 2. Initialize Configurations
        this.configManager = new ConfigManager(this);
        this.configManager.init();

        // 3. Initialize Database Manager (asynchronous repository)
        this.databaseManager = new DatabaseManager(this);
        this.databaseManager.init();

        // 4. Initialize Punishment Logic Controller
        this.punishmentManager = new PunishmentManager(this);
        this.punishmentManager.startExpirationTask();

        // 5. Initialize GUI Manager
        this.guiManager = new GUIManager(this);

        // 6. Register Listeners
        Bukkit.getPluginManager().registerEvents(new PlayerRestrictionListener(this), this);

        // 7. Register Command Executor & Tab Completer
        CommandManager cmdManager = new CommandManager(this);
        String[] commands = {
            "ban", "kick", "mute", "freeze", "jail", "warn",
            "unban", "unmute", "unfreeze", "unjail", "clearwarn", "delwarn",
            "check", "history", "punish", "setjail"
        };
        for (String cmd : commands) {
            var pluginCommand = getCommand(cmd);
            if (pluginCommand != null) {
                pluginCommand.setExecutor(cmdManager);
                pluginCommand.setTabCompleter(cmdManager);
            }
        }

        getLogger().info("Punish v" + getDescription().getVersion() + " has been successfully loaded!");
    }

    @Override
    public void onDisable() {
        if (punishmentManager != null) {
            punishmentManager.shutdown();
        }
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
        getLogger().info("Punish has been successfully disabled!");
        instance = null;
    }

    private void printBanner() {
        String[] banner = {
            " ",
            "  ██████╗ ██╗   ██╗███╗   ██╗██╗███████╗██╗  ██╗",
            "  ██╔══██╗██║   ██║████╗  ██║██║██╔════╝██║  ██║",
            "  ██████╔╝██║   ██║██╔██╗ ██║██║███████╗███████║",
            "  ██╔═══╝ ██║   ██║██║╚██╗██║██║╚════██║██╔══██║",
            "  ██║     ╚██████╔╝██║ ╚████║██║███████║██║  ██║",
            "  ╚═╝      ╚═════╝ ╚═╝  ╚═══╝╚═╝╚══════╝╚═╝  ╚═╝",
            "                                     Version 26.0.1 by Chaeulso",
            " "
        };
        for (String line : banner) {
            Bukkit.getConsoleSender().sendMessage(line);
        }
    }

    public static PunishPlugin getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public PunishmentManager getPunishmentManager() {
        return punishmentManager;
    }

    public GUIManager getGuiManager() {
        return guiManager;
    }

    public AuthScheduler getAuthScheduler() {
        return authScheduler;
    }
}
