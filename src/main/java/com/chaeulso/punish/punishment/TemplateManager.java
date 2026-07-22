package com.chaeulso.punish.punishment;

import com.chaeulso.punish.PunishPlugin;
import com.chaeulso.punish.model.Punishment;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Manages punishment templates and infraction scaling ladders.
 */
public class TemplateManager {
    private final PunishPlugin plugin;
    private FileConfiguration config;

    public TemplateManager(@NotNull PunishPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "templates.yml");
        if (!file.exists()) {
            plugin.saveResource("templates.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);

        InputStream defStream = plugin.getResource("templates.yml");
        if (defStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defStream));
            config.setDefaults(defConfig);
        }
    }

    /**
     * Resolves the next duration for a player based on their infraction count for a specific template.
     */
    public CompletableFuture<String> getNextDuration(@NotNull UUID uuid, @NotNull String templateKey) {
        return plugin.getDatabaseManager().getPunishmentHistory(uuid).thenApply(history -> {
            String path = "templates." + templateKey;
            if (!config.contains(path)) return "30m"; // fallback

            List<String> ladder = config.getStringList(path + ".ladder");
            if (ladder == null || ladder.isEmpty()) return "1d";

            String typeStr = config.getString(path + ".type", "ban");
            String reason = config.getString(path + ".reason", "Moderation action");

            // Count previous matches of this type and reason in history
            int count = 0;
            for (Punishment p : history) {
                if (p.getType().name().equalsIgnoreCase(typeStr) && p.getReason().equalsIgnoreCase(reason)) {
                    count++;
                }
            }

            // Get duration from ladder based on previous infraction count
            int index = Math.min(count, ladder.size() - 1);
            return ladder.get(index);
        });
    }

    @Nullable
    public String getTemplateReason(@NotNull String templateKey) {
        return config.getString("templates." + templateKey + ".reason");
    }

    @Nullable
    public Punishment.Type getTemplateType(@NotNull String templateKey) {
        String type = config.getString("templates." + templateKey + ".type");
        if (type == null) return null;
        try {
            return Punishment.Type.valueOf(type.toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }
}
