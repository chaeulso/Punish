package com.chaeulso.punish.listeners;

import com.chaeulso.punish.PunishPlugin;
import com.chaeulso.punish.model.Punishment;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Handles all moderation gameplay restrictions (frozen movement, jails, chat silences, and command blocklists).
 */
public class PlayerRestrictionListener implements Listener {
    private final PunishPlugin plugin;

    public PlayerRestrictionListener(@NotNull PunishPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();
        
        // 1. Perform Asynchronous DB query to check ban
        try {
            List<Punishment> list = plugin.getDatabaseManager().getActivePunishments(uuid).get();
            for (Punishment p : list) {
                if (p.getType() == Punishment.Type.BAN && p.isActive()) {
                    String kickReason = plugin.getPunishmentManager().formatBanScreen(p);
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, LegacyComponentSerializer.legacyAmpersand().deserialize(kickReason));
                    return;
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load ban status asynchronously on pre-login: " + e.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Load active punishments cache asynchronously
        plugin.getPunishmentManager().loadCache(player.getUniqueId());

        // Kick again on main thread just in case they slipped through
        plugin.getAuthScheduler().runTaskLater(player, () -> {
            Punishment ban = plugin.getPunishmentManager().getActivePunishment(player.getUniqueId(), Punishment.Type.BAN);
            if (ban != null) {
                String kickReason = plugin.getPunishmentManager().formatBanScreen(ban);
                player.kick(LegacyComponentSerializer.legacyAmpersand().deserialize(kickReason));
            }
        }, 5L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getPunishmentManager().unloadCache(event.getPlayer().getUniqueId());
    }

    // 1. Freeze and Jail Move Restrictions
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Check Freeze
        Punishment freeze = plugin.getPunishmentManager().getActivePunishment(uuid, Punishment.Type.FREEZE);
        if (freeze != null) {
            Location from = event.getFrom();
            Location to = event.getTo();
            if (to == null) return;
            if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
                Location newLoc = from.clone();
                newLoc.setYaw(to.getYaw());
                newLoc.setPitch(to.getPitch());
                event.setTo(newLoc);
                player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&c&l❄ &cYou are currently frozen! You cannot move."));
            }
            return;
        }

        // Check Jail boundaries
        Punishment jail = plugin.getPunishmentManager().getActivePunishment(uuid, Punishment.Type.JAIL);
        if (jail != null) {
            String spawnStr = plugin.getConfigManager().getJail().getString("jail-spawn");
            if (spawnStr != null) {
                Location jailSpawn = deserializeLocation(spawnStr);
                if (jailSpawn != null && player.getLocation().distanceSquared(jailSpawn) > 100.0) { // e.g., max 10 blocks away
                    player.teleport(jailSpawn);
                    player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&c&l🏛 &cYou cannot escape the jail area!"));
                }
            }
        }
    }

    // 2. Chat Silencing
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        Punishment mute = plugin.getPunishmentManager().getActivePunishment(player.getUniqueId(), Punishment.Type.MUTE);
        if (mute != null) {
            event.setCancelled(true);
            
            // Format remaining string dynamically
            long remainSecs = (mute.getExpires() - System.currentTimeMillis()) / 1000L;
            String remainingStr = mute.isPermanent() ? "Permanent" : formatDuration(remainSecs);

            String chatBlockMsg = plugin.getConfigManager().getMessages().getString("commands.mute.chat-blocked", "&cYou are muted!")
                    .replace("%reason%", mute.getReason())
                    .replace("%remaining%", remainingStr);
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(chatBlockMsg));
        }
    }

    // 3. Block Break & Place Restriction
    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (isRestricted(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (isRestricted(player)) {
            event.setCancelled(true);
        }
    }

    // 4. Item Drop & Pick Up Restriction
    @EventHandler(priority = EventPriority.LOWEST)
    public void onDropItem(PlayerDropItemEvent event) {
        if (isRestricted(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPickupItem(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (isRestricted(player)) {
                event.setCancelled(true);
            }
        }
    }

    // 5. Inventory Interaction Restriction
    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player) {
            if (isRestricted(player)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            if (isRestricted(player)) {
                event.setCancelled(true);
            }
        }
    }

    // 6. Combat & Damage Restriction
    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (isRestricted(player)) {
                event.setCancelled(true); // Frozen/Jailed players are invincible
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            if (isRestricted(player)) {
                event.setCancelled(true);
            }
        }
    }

    // 7. Command Restriction for Frozen and Jailed
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage().toLowerCase();
        String[] args = message.split(" ");
        String command = args[0].replace("/", "");

        boolean isFrozen = plugin.getPunishmentManager().getActivePunishment(player.getUniqueId(), Punishment.Type.FREEZE) != null;
        boolean isJailed = plugin.getPunishmentManager().getActivePunishment(player.getUniqueId(), Punishment.Type.JAIL) != null;

        if (isFrozen || isJailed) {
            String mode = isFrozen ? "freeze" : "jail";
            
            // Check whitelist
            List<String> whitelist = plugin.getConfigManager().getWhitelist().getStringList("allowed-commands." + mode);
            if (whitelist != null && whitelist.contains(command)) {
                return; // Whitelisted!
            }

            // Check blacklist
            List<String> blacklist = plugin.getConfigManager().getBlacklist().getStringList("blocked-commands." + mode);
            if (blacklist != null && blacklist.contains(command)) {
                event.setCancelled(true);
                player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&c&l✘ &cYou cannot use this command while " + mode + "d!"));
                return;
            }

            // Default block if not whitelisted
            event.setCancelled(true);
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&c&l✘ &cYou cannot use commands while " + mode + "d!"));
        }
    }

    private boolean isRestricted(Player player) {
        UUID uuid = player.getUniqueId();
        return plugin.getPunishmentManager().getActivePunishment(uuid, Punishment.Type.FREEZE) != null ||
                plugin.getPunishmentManager().getActivePunishment(uuid, Punishment.Type.JAIL) != null;
    }

    private String formatDuration(long totalSeconds) {
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
