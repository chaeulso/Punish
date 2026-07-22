package com.chaeulso.punish.commands;

import com.chaeulso.punish.PunishPlugin;
import com.chaeulso.punish.model.Punishment;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Handles, processes, validates, and auto-completes all Punish plugin moderation commands.
 */
public class CommandManager implements CommandExecutor, TabCompleter {
    private final PunishPlugin plugin;

    public CommandManager(@NotNull PunishPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String name = command.getName().toLowerCase();

        // 1. Get dynamic permission from permissions.yml
        String permNode = plugin.getConfigManager().getPermissions().getString("permissions." + name, "punish." + name);
        if (!sender.hasPermission(permNode)) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(formatMsg("no-permission", "&cNo permission!")));
            return true;
        }

        // Get executing staff credentials
        UUID staffUuid = (sender instanceof Player p) ? p.getUniqueId() : UUID.fromString("00000000-0000-0000-0000-000000000000");
        String staffName = sender.getName();

        switch (name) {
            case "ban" -> handleBanCommand(sender, staffUuid, staffName, args);
            case "kick" -> handleKickCommand(sender, staffUuid, staffName, args);
            case "mute" -> handleMuteCommand(sender, staffUuid, staffName, args);
            case "freeze" -> handleFreezeCommand(sender, staffUuid, staffName, args);
            case "jail" -> handleJailCommand(sender, staffUuid, staffName, args);
            case "warn" -> handleWarnCommand(sender, staffUuid, staffName, args);
            case "unban" -> handleUnbanCommand(sender, staffName, args);
            case "unmute" -> handleUnmuteCommand(sender, staffName, args);
            case "unfreeze" -> handleUnfreezeCommand(sender, staffName, args);
            case "unjail" -> handleUnjailCommand(sender, staffName, args);
            case "clearwarn" -> handleClearWarnCommand(sender, args);
            case "delwarn" -> handleDeleteWarnCommand(sender, args);
            case "check" -> handleCheckCommand(sender, args);
            case "history" -> handleHistoryCommand(sender, args);
            case "punish" -> handlePunishCommand(sender, args);
            case "setjail" -> handleSetJailCommand(sender);
        }

        return true;
    }

    // --- Command Handling Implementations ---

    private void handleBanCommand(CommandSender sender, UUID staffUuid, String staffName, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(formatMsg("commands.ban.usage", "&cUsage: /ban <player> <reason> <duration>")));
            return;
        }

        String targetName = args[0];
        String reason = args[1];
        String durInput = args[2];

        long parsed = parseDuration(durInput);
        if (parsed == -2) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(formatMsg("invalid-duration", "&cInvalid duration!")));
            return;
        }

        resolveOfflinePlayer(targetName).thenAccept(target -> {
            if (target == null) {
                sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(formatMsg("player-not-found", "&cPlayer not found!").replace("%player%", targetName)));
                return;
            }

            if (checkAbuseExemption(sender, target, "ban")) return;

            long expires = (parsed == -1) ? 0 : System.currentTimeMillis() + parsed;
            String punId = "BAN-" + String.format("%06d", new Random().nextInt(100000));

            Punishment p = new Punishment(punId, target.getUniqueId(), targetName, staffUuid, staffName,
                    reason, Punishment.Type.BAN, System.currentTimeMillis(), expires, "UTC", "Global");

            plugin.getPunishmentManager().applyPunishment(p);
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(formatMsg("commands.ban.success", "&aSuccessfully banned player &e%player%")
                    .replace("%player%", targetName)
                    .replace("%reason%", reason)
                    .replace("%duration%", durInput)));
        });
    }

    private void handleKickCommand(CommandSender sender, UUID staffUuid, String staffName, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(formatMsg("commands.kick.usage", "&cUsage: /kick <player> <reason>")));
            return;
        }

        String targetName = args[0];
        String reason = args[1];

        Player target = Bukkit.getPlayer(targetName);
        if (target == null || !target.isOnline()) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(formatMsg("player-not-found", "&cPlayer not found!").replace("%player%", targetName)));
            return;
        }

        if (checkAbuseExemption(sender, target, "kick")) return;

        String punId = "KICK-" + String.format("%06d", new Random().nextInt(100000));
        Punishment p = new Punishment(punId, target.getUniqueId(), targetName, staffUuid, staffName,
                reason, Punishment.Type.KICK, System.currentTimeMillis(), 0, "UTC", "Global");

        plugin.getPunishmentManager().applyPunishment(p);
        sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(formatMsg("commands.kick.success", "&aSuccessfully kicked player &e%player%")
                .replace("%player%", targetName)
                .replace("%reason%", reason)));
    }

    private void handleMuteCommand(CommandSender sender, UUID staffUuid, String staffName, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(formatMsg("commands.mute.usage", "&cUsage: /mute <player> <reason> <duration>")));
            return;
        }

        String targetName = args[0];
        String reason = args[1];
        String durInput = args[2];

        long parsed = parseDuration(durInput);
        if (parsed == -2) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(formatMsg("invalid-duration", "&cInvalid duration!")));
            return;
        }

        resolveOfflinePlayer(targetName).thenAccept(target -> {
            if (target == null) {
                sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(formatMsg("player-not-found", "&cPlayer not found!").replace("%player%", targetName)));
                return;
            }

            if (checkAbuseExemption(sender, target, "mute")) return;

            long expires = (parsed == -1) ? 0 : System.currentTimeMillis() + parsed;
            String punId = "MUTE-" + String.format("%06d", new Random().nextInt(100000));

            Punishment p = new Punishment(punId, target.getUniqueId(), targetName, staffUuid, staffName,
                    reason, Punishment.Type.MUTE, System.currentTimeMillis(), expires, "UTC", "Global");

            plugin.getPunishmentManager().applyPunishment(p);
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(formatMsg("commands.mute.success", "&aSuccessfully muted player &e%player%")
                    .replace("%player%", targetName)
                    .replace("%reason%", reason)
                    .replace("%duration%", durInput)));
        });
    }

    private void handleFreezeCommand(CommandSender sender, UUID staffUuid, String staffName, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(formatMsg("commands.freeze.usage", "&cUsage: /freeze <player> <reason> <duration>")));
            return;
        }

        String targetName = args[0];
        String reason = args[1];
        String durInput = args[2];

        long parsed = parseDuration(durInput);
        if (parsed == -2) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(formatMsg("invalid-duration", "&cInvalid duration!")));
            return;
        }

        resolveOfflinePlayer(targetName).thenAccept(target -> {
            if (target == null) {
                sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(formatMsg("player-not-found", "&cPlayer not found!").replace("%player%", targetName)));
                return;
            }

            if (checkAbuseExemption(sender, target, "freeze")) return;

            long expires = (parsed == -1) ? 0 : System.currentTimeMillis() + parsed;
            String punId = "FREEZE-" + String.format("%06d", new Random().nextInt(100000));

            Punishment p = new Punishment(punId, target.getUniqueId(), targetName, staffUuid, staffName,
                    reason, Punishment.Type.FREEZE, System.currentTimeMillis(), expires, "UTC", "Global");

            plugin.getPunishmentManager().applyPunishment(p);
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(formatMsg("commands.freeze.success", "&aSuccessfully frozen player &e%player%")
                    .replace("%player%", targetName)
                    .replace("%reason%", reason)
                    .replace("%duration%", durInput)));
        });
    }

    private void handleJailCommand(CommandSender sender, UUID staffUuid, String staffName, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(formatMsg("commands.jail.usage", "&cUsage: /jail <player> <reason> <duration>")));
            return;
        }

        // Verify jail coordinate is configured
        String spawnStr = plugin.getConfigManager().getJail().getString("jail-spawn");
        if (spawnStr == null) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(formatMsg("commands.jail.spawn-not-set", "&cJail spawn not set!")));
            return;
        }

        String targetName = args[0];
        String reason = args[1];
        String durInput = args[2];

        long parsed = parseDuration(durInput);
        if (parsed == -2) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(formatMsg("invalid-duration", "&cInvalid duration!")));
            return;
        }

        resolveOfflinePlayer(targetName).thenAccept(target -> {
            if (target == null) {
                sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(formatMsg("player-not-found", "&cPlayer not found!").replace("%player%", targetName)));
                return;
            }

            if (checkAbuseExemption(sender, target, "jail")) return;

            long expires = (parsed == -1) ? 0 : System.currentTimeMillis() + parsed;
            String punId = "JAIL-" + String.format("%06d", new Random().nextInt(100000));

            Punishment p = new Punishment(punId, target.getUniqueId(), targetName, staffUuid, staffName,
                    reason, Punishment.Type.JAIL, System.currentTimeMillis(), expires, "UTC", "Global");

            plugin.getPunishmentManager().applyPunishment(p);
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(formatMsg("commands.jail.success", "&aSuccessfully jailed player &e%player%")
                    .replace("%player%", targetName)
                    .replace("%reason%", reason)
                    .replace("%duration%", durInput)));
        });
    }

    private void handleWarnCommand(CommandSender sender, UUID staffUuid, String staffName, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(formatMsg("commands.warn.usage", "&cUsage: /warn <player> <reason>")));
            return;
        }

        String targetName = args[0];
        String reason = args[1];

        resolveOfflinePlayer(targetName).thenAccept(target -> {
            if (target == null) {
                sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(formatMsg("player-not-found", "&cPlayer not found!").replace("%player%", targetName)));
                return;
            }

            if (checkAbuseExemption(sender, target, "warn")) return;

            String warnId = "WARN-" + String.format("%06d", new Random().nextInt(100000));
            
            plugin.getDatabaseManager().addWarn(warnId, target.getUniqueId(), staffName, reason).thenRun(() -> {
                plugin.getDatabaseManager().saveLog(staffName, "WARN", targetName, reason);
                sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(formatMsg("commands.warn.success", "&aSuccessfully warned player &e%player%")
                        .replace("%player%", targetName)
                        .replace("%reason%", reason)));

                // Try to send private warning notification
                Player online = Bukkit.getPlayer(target.getUniqueId());
                if (online != null && online.isOnline()) {
                    List<String> warnLines = plugin.getConfigManager().getMessages().getStringList("notifications.warn");
                    for (String line : warnLines) {
                        online.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(line
                                .replace("%reason%", reason)
                                .replace("%warn_id%", warnId)
                                .replace("%staff%", staffName)));
                    }
                }
            });
        });
    }

    private void handleUnbanCommand(CommandSender sender, String staffName, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(formatMsg("commands.unban.usage", "&cUsage: /unban <player>")));
            return;
        }
        String targetName = args[0];
        resolveOfflinePlayer(targetName).thenAccept(target -> {
            if (target == null) {
                sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(formatMsg("player-not-found", "&cPlayer not found!").replace("%player%", targetName)));
                return;
            }
            plugin.getPunishmentManager().removePunishment(target.getUniqueId(), Punishment.Type.BAN, staffName, "Pardoned via unban command.");
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(formatMsg("commands.unban.success", "&aSuccessfully unbanned &e%player%&a.").replace("%player%", targetName)));
        });
    }

    private void handleUnmuteCommand(CommandSender sender, String staffName, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(formatMsg("commands.unmute.usage", "&cUsage: /unmute <player>")));
            return;
        }
        String targetName = args[0];
        resolveOfflinePlayer(targetName).thenAccept(target -> {
            if (target == null) {
                sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(formatMsg("player-not-found", "&cPlayer not found!").replace("%player%", targetName)));
                return;
            }
            plugin.getPunishmentManager().removePunishment(target.getUniqueId(), Punishment.Type.MUTE, staffName, "Pardoned via unmute command.");
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(formatMsg("commands.unmute.success", "&aSuccessfully unmuted &e%player%&a.").replace("%player%", targetName)));
        });
    }

    private void handleUnfreezeCommand(CommandSender sender, String staffName, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(formatMsg("commands.unfreeze.usage", "&cUsage: /unfreeze <player>")));
            return;
        }
        String targetName = args[0];
        resolveOfflinePlayer(targetName).thenAccept(target -> {
            if (target == null) {
                sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(formatMsg("player-not-found", "&cPlayer not found!").replace("%player%", targetName)));
                return;
            }
            plugin.getPunishmentManager().removePunishment(target.getUniqueId(), Punishment.Type.FREEZE, staffName, "Pardoned via unfreeze command.");
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(formatMsg("commands.unfreeze.success", "&aSuccessfully unfrozen &e%player%&a.").replace("%player%", targetName)));
        });
    }

    private void handleUnjailCommand(CommandSender sender, String staffName, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(formatMsg("commands.unjail.usage", "&cUsage: /unjail <player>")));
            return;
        }
        String targetName = args[0];
        resolveOfflinePlayer(targetName).thenAccept(target -> {
            if (target == null) {
                sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(formatMsg("player-not-found", "&cPlayer not found!").replace("%player%", targetName)));
                return;
            }
            plugin.getPunishmentManager().removePunishment(target.getUniqueId(), Punishment.Type.JAIL, staffName, "Pardoned via unjail command.");
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(formatMsg("commands.unjail.success", "&aSuccessfully unjailed &e%player%&a.").replace("%player%", targetName)));
        });
    }

    private void handleClearWarnCommand(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(formatMsg("commands.clearwarn.usage", "&cUsage: /clearwarn <player>")));
            return;
        }
        String targetName = args[0];
        resolveOfflinePlayer(targetName).thenAccept(target -> {
            if (target == null) {
                sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(formatMsg("player-not-found", "&cPlayer not found!").replace("%player%", targetName)));
                return;
            }
            plugin.getDatabaseManager().clearWarns(target.getUniqueId()).thenRun(() -> {
                plugin.getDatabaseManager().saveLog(sender.getName(), "CLEARWARN", targetName, "Cleared all warnings.");
                sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(formatMsg("commands.clearwarn.success", "&aSuccessfully cleared warnings for &e%player%&a.").replace("%player%", targetName)));
            });
        });
    }

    private void handleDeleteWarnCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(formatMsg("commands.delwarn.usage", "&cUsage: /delwarn <player> <warn-id>")));
            return;
        }
        String targetName = args[0];
        String warnId = args[1];
        resolveOfflinePlayer(targetName).thenAccept(target -> {
            if (target == null) {
                sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(formatMsg("player-not-found", "&cPlayer not found!").replace("%player%", targetName)));
                return;
            }
            plugin.getDatabaseManager().deleteWarn(target.getUniqueId(), warnId).thenAccept(success -> {
                if (success) {
                    plugin.getDatabaseManager().saveLog(sender.getName(), "DELWARN", targetName, "Deleted warn ID: " + warnId);
                    sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(formatMsg("commands.delwarn.success", "&aSuccessfully deleted warning &e%warn_id%&a.").replace("%warn_id%", warnId).replace("%player%", targetName)));
                } else {
                    sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(formatMsg("commands.delwarn.not-found", "&cWarning ID not found!").replace("%warn_id%", warnId).replace("%player%", targetName)));
                }
            });
        });
    }

    private void handleCheckCommand(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&cUsage: /check <player>"));
            return;
        }
        String targetName = args[0];
        resolveOfflinePlayer(targetName).thenAccept(target -> {
            if (target == null) {
                sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(formatMsg("player-not-found", "&cPlayer not found!").replace("%player%", targetName)));
                return;
            }
            UUID uuid = target.getUniqueId();
            
            // Fetch warning count & active punishments asynchronously
            plugin.getDatabaseManager().getWarnCount(uuid).thenAccept(warnCount -> {
                plugin.getDatabaseManager().getActivePunishments(uuid).thenAccept(active -> {
                    boolean isBanned = false, isMuted = false, isFrozen = false, isJailed = false;
                    for (Punishment p : active) {
                        if (p.getType() == Punishment.Type.BAN && p.isActive()) isBanned = true;
                        if (p.getType() == Punishment.Type.MUTE && p.isActive()) isMuted = true;
                        if (p.getType() == Punishment.Type.FREEZE && p.isActive()) isFrozen = true;
                        if (p.getType() == Punishment.Type.JAIL && p.isActive()) isJailed = true;
                    }

                    sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                            "&6&lModeration Check: &e" + targetName + "\n" +
                            "&8» &fBan: " + (isBanned ? "&aActive" : "&cNone") + "\n" +
                            "&8» &fMute: " + (isMuted ? "&aActive" : "&cNone") + "\n" +
                            "&8» &fFreeze: " + (isFrozen ? "&aActive" : "&cNone") + "\n" +
                            "&8» &fJail: " + (isJailed ? "&aActive" : "&cNone") + "\n" +
                            "&8» &fActive Warnings: &e" + warnCount
                    ));
                });
            });
        });
    }

    private void handleHistoryCommand(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&cUsage: /history <player>"));
            return;
        }
        String targetName = args[0];
        resolveOfflinePlayer(targetName).thenAccept(target -> {
            if (target == null) {
                sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(formatMsg("player-not-found", "&cPlayer not found!").replace("%player%", targetName)));
                return;
            }
            plugin.getDatabaseManager().getPunishmentHistory(target.getUniqueId()).thenAccept(history -> {
                if (history.isEmpty()) {
                    sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&cNo punishment history found for &e" + targetName));
                    return;
                }
                sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&6========== Punishment History: &e" + targetName + " &6=========="));
                for (Punishment p : history) {
                    sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                            "&8» &6&l" + p.getType().name() + " &7| Reason: &f" + p.getReason() + " &7| By: &e" + p.getStaffName() + "\n" +
                            "   &7ID: &b" + p.getPunishmentId() + " &7| Date: &7" + new Date(p.getIssued()) + " &7| Status: " + (p.isActive() ? "&aActive" : "&cExpired")
                    ));
                }
            });
        });
    }

    private void handlePunishCommand(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&cUsage: /punish <player> [reload/rollback]"));
            return;
        }

        if ("reload".equalsIgnoreCase(args[0])) {
            String reloadPerm = plugin.getConfigManager().getPermissions().getString("permissions.reload", "punish.reload");
            if (!sender.hasPermission(reloadPerm)) {
                sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(formatMsg("no-permission", "&cNo permission!")));
                return;
            }
            plugin.getConfigManager().reload();
            plugin.getTemplateManager().reload();
            plugin.getPunishmentManager().clearCache();
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&a&l✔ &aAll configuration files successfully reloaded!"));
            return;
        }

        if ("rollback".equalsIgnoreCase(args[0])) {
            String rollbackPerm = plugin.getConfigManager().getPermissions().getString("permissions.rollback", "punish.admin");
            if (!sender.hasPermission(rollbackPerm)) {
                sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(formatMsg("no-permission", "&cNo permission!")));
                return;
            }
            if (args.length < 2) {
                sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&cUsage: /punish rollback <staff_name> [reason]"));
                return;
            }
            String staff = args[1];
            String reason = args.length > 2 ? args[2] : "Staff punishments rolled back by admin.";

            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&8[&cPunish&8] &aRolling back all punishments issued by &e" + staff + "&a..."));
            
            plugin.getDatabaseManager().getPunishmentHistoryByStaff(staff).thenAccept(list -> {
                if (list.isEmpty()) {
                    sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&c&l✘ &cNo punishments found issued by staff: &e" + staff));
                    return;
                }
                int count = 0;
                for (Punishment p : list) {
                    if (p.isActive()) {
                        p.setActive(false);
                        p.setRemoved(true);
                        p.setRemovedBy(sender.getName());
                        p.setRemovedReason(reason);
                        p.setRemovedDate(System.currentTimeMillis());
                        plugin.getDatabaseManager().updatePunishment(p);
                        
                        plugin.getPunishmentManager().unloadCache(p.getUuid());
                        count++;
                    }
                }
                sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                        "&a&l✔ &aSuccessfully rolled back &e" + count + " &aactive punishments issued by &e" + staff + "&a!"));
            });
            return;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(formatMsg("only-player-error", "&cPlayer-only command!")));
            return;
        }

        String targetName = args[0];
        resolveOfflinePlayer(targetName).thenAccept(target -> {
            if (target == null) {
                player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(formatMsg("player-not-found", "&cPlayer not found!").replace("%player%", targetName)));
                return;
            }
            // Open GUI asynchronously or on main thread
            plugin.getAuthScheduler().runTask(player, () -> {
                plugin.getGuiManager().openMainGUI(player, target);
            });
        });
    }

    private void handleSetJailCommand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(formatMsg("only-player-error", "&cPlayer-only command!")));
            return;
        }
        Location loc = player.getLocation();
        String serialized = loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ() + "," + loc.getYaw() + "," + loc.getPitch();
        plugin.getConfigManager().getJail().set("jail-spawn", serialized);
        plugin.getConfigManager().saveJail();
        player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&a&l✔ &aJail spawn location successfully saved at your current position!"));
    }

    // --- Core Helper Functions ---

    private String formatMsg(String path, String def) {
        return plugin.getConfigManager().getMessages().getString(path, def);
    }

    /**
     * Resolves an offline player UUID safely and asynchronously from server registries.
     */
    private CompletableFuture<OfflinePlayer> resolveOfflinePlayer(String name) {
        return CompletableFuture.supplyAsync(() -> {
            Player online = Bukkit.getPlayer(name);
            if (online != null) return online;
            return Bukkit.getOfflinePlayer(name);
        }, task -> plugin.getAuthScheduler().runTaskAsynchronously(task));
    }

    /**
     * Checks permission weight restrictions to prevent lower staff from punishing higher staff.
     */
    private boolean checkAbuseExemption(CommandSender sender, OfflinePlayer target, String punishmentType) {
        if (!(sender instanceof Player staff)) {
            return false; // Console is omnipotent and bypasses weight checks
        }

        // Check self-punish block
        if (staff.getUniqueId().equals(target.getUniqueId())) {
            boolean allowSelf = plugin.getConfig().getBoolean("anti-abuse.allow-self-" + punishmentType, false);
            if (!allowSelf) {
                sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(formatMsg("self-punish-error", "&cYou cannot punish yourself!")));
                return true;
            }
        }

        // Check if target is online and exempt
        Player onlineTarget = target.getPlayer();
        if (onlineTarget != null && onlineTarget.isOnline()) {
            boolean isOp = onlineTarget.isOp();
            boolean isOwner = onlineTarget.hasPermission("punish.bypass.owner") || isOp;
            boolean isAdmin = onlineTarget.hasPermission("punish.bypass.admin") || isOp;

            boolean protectOwner = plugin.getConfig().getBoolean("anti-abuse.protect-owner", true);
            boolean protectAdmin = plugin.getConfig().getBoolean("anti-abuse.protect-admin", true);

            if ((isOwner && protectOwner) || (isAdmin && protectAdmin)) {
                sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(formatMsg("staff-exempt-error", "&cThis player is exempt!")));
                return true;
            }
        }

        return false;
    }

    /**
     * Parses duration input strings (s, m, h, d, w, mo, y, perm) into milliseconds.
     */
    public long parseDuration(String input) {
        if (input == null || input.isEmpty()) return -1;
        String clean = input.toLowerCase().trim();
        if (clean.equals("perm") || clean.equals("permanent")) {
            return -1; // Permanent
        }

        long seconds = 0;
        try {
            char suffix = clean.charAt(clean.length() - 1);
            String numPart = clean.substring(0, clean.length() - 1);
            
            if (clean.endsWith("mo")) {
                suffix = 'M';
                numPart = clean.substring(0, clean.length() - 2);
            }

            long num = Long.parseLong(numPart);
            switch (suffix) {
                case 's' -> seconds = num;
                case 'm' -> seconds = num * 60;
                case 'h' -> seconds = num * 3600;
                case 'd' -> seconds = num * 86400;
                case 'w' -> seconds = num * 604800;
                case 'M' -> seconds = num * 2592000; // 30 days
                case 'y' -> seconds = num * 31536000; // 365 days
                default -> seconds = Long.parseLong(clean);
            }
        } catch (Exception e) {
            return -2; // Parsing Error
        }
        return seconds * 1000L;
    }

    // --- Tab Completion ---

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        String name = command.getName().toLowerCase();

        if (args.length == 1) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(p.getName());
                }
            }
            if ("reload".startsWith(args[0].toLowerCase())) {
                completions.add("reload");
            }
            if ("rollback".startsWith(args[0].toLowerCase())) {
                completions.add("rollback");
            }
        } else if (args.length == 2 && List.of("ban", "kick", "mute", "freeze", "jail", "warn").contains(name)) {
            // Autocomplete preset reasons
            Set<String> keys = plugin.getConfigManager().getReasons().getConfigurationSection("reasons." + name).getKeys(false);
            for (String key : keys) {
                if (key.startsWith(args[1].toLowerCase())) {
                    completions.add(key);
                }
            }
        } else if (args.length == 3 && List.of("ban", "mute", "freeze", "jail").contains(name)) {
            // Autocomplete durations
            for (String dur : List.of("30m", "1h", "1d", "7d", "30d", "permanent")) {
                if (dur.startsWith(args[2].toLowerCase())) {
                    completions.add(dur);
                }
            }
        }

        return completions;
    }
}
