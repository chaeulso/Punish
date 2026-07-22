package com.chaeulso.punish.config;

import com.chaeulso.punish.PunishPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages all configuration files dynamically and provides multi-language capabilities.
 */
public class ConfigManager {
    private final PunishPlugin plugin;
    private final Map<String, FileConfiguration> configs = new HashMap<>();
    private final Map<String, File> files = new HashMap<>();

    public ConfigManager(@NotNull PunishPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        // Load config.yml first
        loadConfig("config.yml");

        // Load other base configurations
        loadConfig("permissions.yml");
        loadConfig("database.yml");
        loadConfig("jail.yml");
        loadConfig("gui.yml");
        loadConfig("reasons.yml");
        loadConfig("blacklist.yml");
        loadConfig("whitelist.yml");
        loadConfig("timezone.yml");
        loadConfig("sounds.yml");
        loadConfig("webhook.yml");

        // Load dynamic language configuration
        loadLanguageConfig();
    }

    private void loadConfig(String name) {
        File file = new File(plugin.getDataFolder(), name);
        if (!file.exists()) {
            plugin.saveResource(name, false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        
        InputStream defStream = plugin.getResource(name);
        if (defStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defStream));
            config.setDefaults(defConfig);
        }

        configs.put(name, config);
        files.put(name, file);
    }

    private void loadLanguageConfig() {
        String lang = getConfig().getString("language", "en").toLowerCase();
        String fileName = "messages_" + lang + ".yml";
        File file = new File(plugin.getDataFolder(), fileName);

        if (!file.exists()) {
            InputStream resourceStream = plugin.getResource(fileName);
            if (resourceStream != null) {
                plugin.saveResource(fileName, false);
            } else {
                // Default fallback to messages_en.yml
                File enFile = new File(plugin.getDataFolder(), "messages_en.yml");
                if (!enFile.exists()) {
                    plugin.saveResource("messages_en.yml", false);
                }
                file = enFile;
                fileName = "messages_en.yml";
            }
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        
        InputStream defStream = plugin.getResource(fileName);
        if (defStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defStream));
            config.setDefaults(defConfig);
        } else {
            InputStream enStream = plugin.getResource("messages_en.yml");
            if (enStream != null) {
                YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(enStream));
                config.setDefaults(defConfig);
            }
        }

        configs.put("messages.yml", config);
        files.put("messages.yml", file);
    }

    public void reload() {
        configs.clear();
        files.clear();
        plugin.reloadConfig();
        init();
    }

    public FileConfiguration getConfig() {
        return configs.get("config.yml");
    }

    public FileConfiguration getMessages() {
        return configs.get("messages.yml");
    }

    public FileConfiguration getPermissions() {
        return configs.get("permissions.yml");
    }

    public FileConfiguration getDatabase() {
        return configs.get("database.yml");
    }

    public FileConfiguration getJail() {
        return configs.get("jail.yml");
    }

    public FileConfiguration getGui() {
        return configs.get("gui.yml");
    }

    public FileConfiguration getReasons() {
        return configs.get("reasons.yml");
    }

    public FileConfiguration getBlacklist() {
        return configs.get("blacklist.yml");
    }

    public FileConfiguration getWhitelist() {
        return configs.get("whitelist.yml");
    }

    public FileConfiguration getTimezone() {
        return configs.get("timezone.yml");
    }

    public FileConfiguration getSounds() {
        return configs.get("sounds.yml");
    }

    public FileConfiguration getWebhook() {
        return configs.get("webhook.yml");
    }

    public void saveJail() {
        try {
            configs.get("jail.yml").save(files.get("jail.yml"));
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save jail.yml coordinates file: " + e.getMessage());
        }
    }
}
