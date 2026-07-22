package com.chaeulso.punish.gui;

import com.chaeulso.punish.PunishPlugin;
import com.chaeulso.punish.model.Punishment;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Modern chest GUI renderer and clicks handler.
 * Completely configurable via gui.yml and reasons.yml.
 */
public class GUIManager implements Listener {
    private final PunishPlugin plugin;

    public GUIManager(@NotNull PunishPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public static class PunishHolder implements InventoryHolder {
        private final OfflinePlayer target;
        private final String action; // "MAIN", "DURATION", "REASON", "CONFIRM"
        private String selectedType; // "ban", "mute", "freeze", "jail", "kick", "warn"
        private String selectedDuration;
        private String selectedReason;

        public PunishHolder(OfflinePlayer target, String action) {
            this.target = target;
            this.action = action;
        }

        public OfflinePlayer getTarget() { return target; }
        public String getAction() { return action; }
        public String getSelectedType() { return selectedType; }
        public void setSelectedType(String selectedType) { this.selectedType = selectedType; }
        public String getSelectedDuration() { return selectedDuration; }
        public void setSelectedDuration(String selectedDuration) { this.selectedDuration = selectedDuration; }
        public String getSelectedReason() { return selectedReason; }
        public void setSelectedReason(String selectedReason) { this.selectedReason = selectedReason; }

        @Override
        public @NotNull Inventory getInventory() { return null; }
    }

    /**
     * Opens the main moderation selection dashboard.
     */
    public void openMainGUI(Player staff, OfflinePlayer target) {
        FileConfiguration guiConfig = plugin.getConfigManager().getGui();
        String title = guiConfig.getString("main-gui.title", "&4&lPunish Moderation");
        int rows = guiConfig.getInt("main-gui.rows", 3);

        PunishHolder holder = new PunishHolder(target, "MAIN");
        Inventory inv = Bukkit.createInventory(holder, rows * 9, LegacyComponentSerializer.legacyAmpersand().deserialize(title));

        // Populates buttons from gui.yml
        setupButton(inv, guiConfig, "main-gui.items.ban", Material.RED_WOOL, 10);
        setupButton(inv, guiConfig, "main-gui.items.kick", Material.ORANGE_WOOL, 11);
        setupButton(inv, guiConfig, "main-gui.items.mute", Material.YELLOW_WOOL, 12);
        setupButton(inv, guiConfig, "main-gui.items.freeze", Material.LIGHT_BLUE_WOOL, 13);
        setupButton(inv, guiConfig, "main-gui.items.jail", Material.GRAY_WOOL, 14);
        setupButton(inv, guiConfig, "main-gui.items.warn", Material.WHITE_WOOL, 15);
        setupButton(inv, guiConfig, "main-gui.items.close", Material.BARRIER, 22);

        staff.openInventory(inv);
    }

    private void setupButton(Inventory inv, FileConfiguration config, String path, Material defMaterial, int defSlot) {
        String matStr = config.getString(path + ".material");
        Material mat = defMaterial;
        try {
            if (matStr != null) mat = Material.valueOf(matStr.toUpperCase());
        } catch (Exception ignored) {}

        int slot = config.getInt(path + ".slot", defSlot);
        String name = config.getString(path + ".display-name", "&cButton");
        List<String> lore = config.getStringList(path + ".lore");

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(name));
            List<Component> compLore = new ArrayList<>();
            for (String line : lore) {
                compLore.add(LegacyComponentSerializer.legacyAmpersand().deserialize(line));
            }
            meta.lore(compLore);
            item.setItemMeta(meta);
        }
        inv.setItem(slot, item);
    }

    /**
     * Opens the Duration selection picker GUI.
     */
    public void openDurationGUI(Player staff, OfflinePlayer target, String type) {
        PunishHolder holder = new PunishHolder(target, "DURATION");
        holder.setSelectedType(type);

        Inventory inv = Bukkit.createInventory(holder, 27, LegacyComponentSerializer.legacyAmpersand().deserialize("&5&lSelect Duration"));

        // Preset durations
        String[] durs = {"30m", "1h", "6h", "12h", "1d", "3d", "7d", "30d", "permanent"};
        int[] slots = {1, 2, 3, 4, 5, 6, 7, 8, 13};

        for (int i = 0; i < durs.length; i++) {
            ItemStack item = new ItemStack(Material.CLOCK);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize("&d&l" + durs[i].toUpperCase()));
                meta.lore(List.of(LegacyComponentSerializer.legacyAmpersand().deserialize("&7Click to select this duration.")));
                item.setItemMeta(meta);
            }
            inv.setItem(slots[i], item);
        }

        // Back button
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize("&c&l« Back to Dashboard"));
            back.setItemMeta(backMeta);
        }
        inv.setItem(18, back);

        staff.openInventory(inv);
    }

    /**
     * Opens the Reason selection picker GUI.
     */
    public void openReasonGUI(Player staff, OfflinePlayer target, String type, String duration) {
        PunishHolder holder = new PunishHolder(target, "REASON");
        holder.setSelectedType(type);
        holder.setSelectedDuration(duration);

        Inventory inv = Bukkit.createInventory(holder, 27, LegacyComponentSerializer.legacyAmpersand().deserialize("&6&lSelect Reason"));

        FileConfiguration reasonsConfig = plugin.getConfigManager().getReasons();
        Set<String> keys = reasonsConfig.getConfigurationSection("reasons." + type).getKeys(false);

        int slot = 10;
        for (String key : keys) {
            String display = reasonsConfig.getString("reasons." + type + "." + key);
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize("&e&l" + display));
                meta.lore(List.of(
                        LegacyComponentSerializer.legacyAmpersand().deserialize("&7Reason ID: &d" + key),
                        LegacyComponentSerializer.legacyAmpersand().deserialize("&eClick to select this reason.")
                ));
                item.setItemMeta(meta);
            }
            inv.setItem(slot++, item);
            if (slot > 16) break;
        }

        staff.openInventory(inv);
    }

    /**
     * Opens the final accidental-proof Confirmation GUI.
     */
    public void openConfirmGUI(Player staff, OfflinePlayer target, String type, String duration, String reason) {
        PunishHolder holder = new PunishHolder(target, "CONFIRM");
        holder.setSelectedType(type);
        holder.setSelectedDuration(duration);
        holder.setSelectedReason(reason);

        Inventory inv = Bukkit.createInventory(holder, 27, LegacyComponentSerializer.legacyAmpersand().deserialize("&a&lConfirm Action"));

        // Yes/Confirm Button
        ItemStack confirm = new ItemStack(Material.GREEN_CONCRETE);
        ItemMeta cMeta = confirm.getItemMeta();
        if (cMeta != null) {
            cMeta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize("&a&l[ CONFIRM PUNISHMENT ]"));
            confirm.setItemMeta(cMeta);
        }
        inv.setItem(11, confirm);

        // Details Item (Informational center)
        ItemStack detail = new ItemStack(Material.BOOK);
        ItemMeta dMeta = detail.getItemMeta();
        if (dMeta != null) {
            dMeta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize("&e&lPunishment Details"));
            dMeta.lore(List.of(
                    LegacyComponentSerializer.legacyAmpersand().deserialize("&7Target: &e" + target.getName()),
                    LegacyComponentSerializer.legacyAmpersand().deserialize("&7Type: &d" + type.toUpperCase()),
                    LegacyComponentSerializer.legacyAmpersand().deserialize("&7Duration: &d" + duration.toUpperCase()),
                    LegacyComponentSerializer.legacyAmpersand().deserialize("&7Reason: &f" + reason)
            ));
            detail.setItemMeta(dMeta);
        }
        inv.setItem(13, detail);

        // Cancel Button
        ItemStack cancel = new ItemStack(Material.RED_CONCRETE);
        ItemMeta canMeta = cancel.getItemMeta();
        if (canMeta != null) {
            canMeta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize("&c&l[ CANCEL ]"));
            cancel.setItemMeta(canMeta);
        }
        inv.setItem(15, cancel);

        staff.openInventory(inv);
    }

    // --- Clicks Event Listening ---

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player staff)) return;
        if (!(event.getInventory().getHolder() instanceof PunishHolder holder)) return;

        event.setCancelled(true); // Disable item taking

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        OfflinePlayer target = holder.getTarget();

        switch (holder.getAction()) {
            case "MAIN" -> {
                int slot = event.getRawSlot();
                switch (slot) {
                    case 10 -> openDurationGUI(staff, target, "ban");
                    case 11 -> openReasonGUI(staff, target, "kick", "0s"); // Kick is immediate
                    case 12 -> openDurationGUI(staff, target, "mute");
                    case 13 -> openDurationGUI(staff, target, "freeze");
                    case 14 -> openDurationGUI(staff, target, "jail");
                    case 15 -> openReasonGUI(staff, target, "warn", "0s");
                    case 22 -> staff.closeInventory();
                }
            }
            case "DURATION" -> {
                if (event.getRawSlot() == 18) {
                    openMainGUI(staff, target);
                    return;
                }
                String name = clicked.getItemMeta().getDisplayName();
                if (name != null) {
                    String cleanDur = org.bukkit.ChatColor.stripColor(name).trim().toLowerCase();
                    openReasonGUI(staff, target, holder.getSelectedType(), cleanDur);
                }
            }
            case "REASON" -> {
                String name = clicked.getItemMeta().getDisplayName();
                if (name != null) {
                    String cleanReason = org.bukkit.ChatColor.stripColor(name).trim();
                    openConfirmGUI(staff, target, holder.getSelectedType(), holder.getSelectedDuration(), cleanReason);
                }
            }
            case "CONFIRM" -> {
                int slot = event.getRawSlot();
                if (slot == 11) {
                    // Approved/Confirmed! Apply punishment
                    staff.closeInventory();
                    executePunishment(staff, target, holder.getSelectedType(), holder.getSelectedDuration(), holder.getSelectedReason());
                } else if (slot == 15) {
                    // Cancelled! Back to dashboard
                    openMainGUI(staff, target);
                }
            }
        }
    }

    private void executePunishment(Player staff, OfflinePlayer target, String type, String duration, String reason) {
        UUID staffUuid = staff.getUniqueId();
        String staffName = staff.getName();

        long parsed = 0;
        if (List.of("ban", "mute", "freeze", "jail").contains(type)) {
            parsed = parseDuration(duration);
        }

        long expires = (parsed == -1) ? 0 : System.currentTimeMillis() + parsed;
        String prefix = type.toUpperCase();
        String punId = prefix + "-" + String.format("%06d", new Random().nextInt(100000));

        if ("warn".equalsIgnoreCase(type)) {
            String warnId = "WARN-" + String.format("%06d", new Random().nextInt(100000));
            plugin.getDatabaseManager().addWarn(warnId, target.getUniqueId(), staffName, reason).thenRun(() -> {
                plugin.getDatabaseManager().saveLog(staffName, "WARN", target.getName(), reason);
                staff.sendMessage("§a§l✔ §aSuccessfully warned player " + target.getName());
            });
            return;
        }

        Punishment.Type pType = Punishment.Type.valueOf(type.toUpperCase());
        Punishment p = new Punishment(punId, target.getUniqueId(), target.getName(), staffUuid, staffName,
                reason, pType, System.currentTimeMillis(), expires, "UTC", "Global");

        plugin.getPunishmentManager().applyPunishment(p);
        staff.sendMessage("§a§l✔ §aSuccessfully executed " + type.toUpperCase() + " punishment against player " + target.getName());
    }

    private long parseDuration(String input) {
        if (input == null || input.isEmpty()) return -1;
        String clean = input.toLowerCase().trim();
        if (clean.equals("perm") || clean.equals("permanent")) {
            return -1;
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
            return -2;
        }
        return seconds * 1000L;
    }
}
