package com.chaeulso.punish.punishment;

import com.chaeulso.punish.PunishPlugin;
import com.chaeulso.punish.model.Punishment;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controller coordinating active punishments, execution, caching, and auto-expirations.
 */
public class PunishmentManager {
    private final PunishPlugin plugin;

    // Active cache maps
    private final Map<UUID, List<Punishment>> activeCache = new ConcurrentHashMap<>();
    private int taskId = -1;

    public PunishmentManager(@NotNull PunishPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Loads a player's active punishments from the database asynchronously.
     */
    public void loadCache(@NotNull UUID uuid) {
        plugin.getDatabaseManager().getActivePunishments(uuid).thenAccept(list -> {
            activeCache.put(uuid, list);
        });
    }

    public void unloadCache(@NotNull UUID uuid) {
        activeCache.remove(uuid);
    }

    public void clearCache() {
        activeCache.clear();
    }

    /**
     * Gets a cached active punishment of a specific type.
     */
    @Nullable
    public Punishment getActivePunishment(@NotNull UUID uuid, @NotNull Punishment.Type type) {
        List<Punishment> list = activeCache.get(uuid);
        if (list == null) return null;
        for (Punishment p : list) {
            if (p.getType() == type && p.isActive()) {
                return p;
            }
        }
        return null;
    }

    /**
     * Executes a new punishment record.
     */
    public void applyPunishment(@NotNull Punishment p) {
        UUID uuid = p.getUuid();
        
        // 1. Persist to database asynchronously
        plugin.getDatabaseManager().savePunishment(p);

        // 2. Add to local active cache if active
        if (p.isActive()) {
            activeCache.computeIfAbsent(uuid, k -> new ArrayList<>()).add(p);
        }

        // 3. Process punishment effect on the main thread
        plugin.getAuthScheduler().runTask(() -> {
            Player target = Bukkit.getPlayer(uuid);
            
            // Dispatch private notification to target
            if (target != null && target.isOnline()) {
                sendPrivateNotification(target, p);
                
                // Execute active punishment actions
                switch (p.getType()) {
                    case BAN -> {
                        String kickReason = formatBanScreen(p);
                        target.kick(LegacyComponentSerializer.legacyAmpersand().deserialize(kickReason));
                    }
                    case KICK -> {
                        String kickMsg = formatMsg("commands.kick.success", "&cKicked for: &e%reason%").replace("%reason%", p.getReason());
                        target.kick(LegacyComponentSerializer.legacyAmpersand().deserialize(kickMsg));
                    }
                    case JAIL -> teleportToJail(target);
                }
            }

            // Dispatch global broadcast and play audio
            playPunishmentSound(p.getType());
            broadcastPunishment(p);
            logModerationAction(p);
        });
    }

    /**
     * Removes/pardons an active punishment.
     */
    public void removePunishment(@NotNull UUID uuid, @NotNull Punishment.Type type, @NotNull String staffName, @NotNull String reason) {
        List<Punishment> list = activeCache.get(uuid);
        if (list == null) return;

        Punishment target = null;
        for (Punishment p : list) {
            if (p.getType() == type && p.isActive()) {
                target = p;
                break;
            }
        }

        if (target != null) {
            target.setActive(false);
            target.setRemoved(true);
            target.setRemovedBy(staffName);
            target.setRemovedReason(reason);
            target.setRemovedDate(System.currentTimeMillis());

            plugin.getDatabaseManager().updatePunishment(target);
            list.remove(target);

            // Re-teleport jailed player back if online
            if (type == Punishment.Type.JAIL) {
                Player onlineTarget = Bukkit.getPlayer(uuid);
                if (onlineTarget != null && onlineTarget.isOnline()) {
                    teleportOutOfJail(onlineTarget);
                }
            }

            // Log mod unban/unmute/unfreeze action
            plugin.getDatabaseManager().saveLog(staffName, "UN" + type.name(), target.getUsername(), reason);
        }
    }

    /**
     * Starts the asynchronous internal scheduler to automatically expire punishments.
     */
    public void startExpirationTask() {
        this.taskId = plugin.getAuthScheduler().runTaskTimer(() -> {
            long now = System.currentTimeMillis();
            for (Map.Entry<UUID, List<Punishment>> entry : activeCache.entrySet()) {
                UUID uuid = entry.getKey();
                List<Punishment> list = entry.getValue();
                
                list.removeIf(p -> {
                    if (p.getExpires() > 0 && now > p.getExpires()) {
                        p.setActive(false);
                        plugin.getDatabaseManager().updatePunishment(p);

                        // Safe run task on Main thread for releases
                        plugin.getAuthScheduler().runTask(() -> {
                            Player target = Bukkit.getPlayer(uuid);
                            if (target != null && target.isOnline()) {
                                target.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                                        formatMsg("prefix", "&8[&cPunish&8]") + " &aYour active " + p.getType().name() + " has expired."));
                                if (p.getType() == Punishment.Type.JAIL) {
                                    teleportOutOfJail(target);
                                }
                            }
                            plugin.getDatabaseManager().saveLog("System", "EXPIRE_" + p.getType().name(), p.getUsername(), "Auto Expired.");
                        });
                        return true; // remove from cache
                    }
                    return false;
                });
            }
        }, 20L, 20L); // Tick every 1 second
    }

    public void shutdown() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        clearCache();
    }

    // --- Auxiliary Formatting Helpers ---

    private String formatMsg(String path, String def) {
        return plugin.getConfigManager().getMessages().getString(path, def);
    }

    private void playPunishmentSound(Punishment.Type type) {
        if (!plugin.getConfig().getBoolean("effects.sounds", true)) return;
        String path = "sounds." + type.name().toLowerCase();
        String soundName = plugin.getConfigManager().getSounds().getString(path + ".sound", "ENTITY_EXPERIENCE_ORB_PICKUP");
        double vol = plugin.getConfigManager().getSounds().getDouble(path + ".volume", 1.0);
        double pitch = plugin.getConfigManager().getSounds().getDouble(path + ".pitch", 1.0);

        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.playSound(p.getLocation(), sound, (float) vol, (float) pitch);
            }
        } catch (Exception ignored) {}
    }

    private void broadcastPunishment(Punishment p) {
        FileConfiguration msgConfig = plugin.getConfigManager().getMessages();
        String typeKey = p.getType().name().toLowerCase();
        boolean enabled = msgConfig.getBoolean("broadcast." + typeKey + ".enabled", true);
        if (!enabled) return;

        List<String> lines = msgConfig.getStringList("broadcast." + typeKey + ".message");
        if (lines.isEmpty()) return;

        String audience = msgConfig.getString("broadcast." + typeKey + ".audience", "ALL");

        for (Player online : Bukkit.getOnlinePlayers()) {
            if ("STAFF".equalsIgnoreCase(audience) && !online.hasPermission("punish.staffnotify")) {
                continue;
            }
            for (String line : lines) {
                online.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(replacePlaceholders(line, p)));
            }
        }
    }

    private void sendPrivateNotification(Player target, Punishment p) {
        FileConfiguration msgConfig = plugin.getConfigManager().getMessages();
        List<String> lines = msgConfig.getStringList("notifications." + p.getType().name().toLowerCase());
        if (lines.isEmpty()) return;

        for (String line : lines) {
            target.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(replacePlaceholders(line, p)));
        }
    }

    private void logModerationAction(Punishment p) {
        plugin.getDatabaseManager().saveLog(p.getStaffName(), p.getType().name(), p.getUsername(), p.getReason());
    }

    public String formatBanScreen(Punishment p) {
        FileConfiguration msgConfig = plugin.getConfigManager().getMessages();
        List<String> lines = p.isPermanent() 
                ? msgConfig.getStringList("ban-screen") 
                : msgConfig.getStringList("temporary-ban-screen");

        if (lines.isEmpty()) {
            return "§cYou are banned!\nReason: " + p.getReason();
        }

        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(replacePlaceholders(line, p)).append("\n");
        }
        return sb.toString();
    }

    private String replacePlaceholders(String text, Punishment p) {
        long remainingSecs = p.isPermanent() ? 0 : (p.getExpires() - System.currentTimeMillis()) / 1000L;
        String remainingStr = p.isPermanent() ? "Permanent" : formatDurationString(remainingSecs);
        String durationStr = p.isPermanent() ? "Permanent" : formatDurationString((p.getExpires() - p.getIssued()) / 1000L);

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(plugin.getConfig().getString("date-format", "dd/MM/yyyy HH:mm:ss"));
        sdf.setTimeZone(TimeZone.getTimeZone(p.getTimezone()));

        return text
                .replace("%player%", p.getUsername())
                .replace("%uuid%", p.getUuid().toString())
                .replace("%staff%", p.getStaffName())
                .replace("%staff_uuid%", p.getStaffUuid().toString())
                .replace("%reason%", p.getReason())
                .replace("%duration%", durationStr)
                .replace("%remaining%", remainingStr)
                .replace("%issued%", sdf.format(new Date(p.getIssued())))
                .replace("%expired%", p.isPermanent() ? "Never" : sdf.format(new Date(p.getExpires())))
                .replace("%timezone%", p.getTimezone())
                .replace("%server%", p.getServer())
                .replace("%punishment_id%", p.getPunishmentId());
    }

    private String formatDurationString(long totalSeconds) {
        if (totalSeconds <= 0) return "0s";
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0) sb.append(seconds).append("s");
        return sb.toString().trim();
    }

    private void teleportToJail(Player player) {
        String locStr = plugin.getConfigManager().getJail().getString("jail-spawn");
        if (locStr != null) {
            Location loc = deserializeLocation(locStr);
            if (loc != null) {
                player.teleport(loc);
            }
        }
    }

    private void teleportOutOfJail(Player player) {
        // Teleport back to spawn or original position (or spawn coordinate default)
        player.teleport(player.getWorld().getSpawnLocation());
    }

    private Location deserializeLocation(String raw) {
        try {
            String[] parts = raw.split(",");
            return new Location(
                    Bukkit.getWorld(parts[0]),
                    Double.parseDouble(parts[1]),
                    Double.parseDouble(parts[2]),
                    Double.parseDouble(parts[3]),
                    Float.parseFloat(parts[4]),
                    Float.parseFloat(parts[5])
            );
        } catch (Exception e) {
            return null;
        }
    }
}
