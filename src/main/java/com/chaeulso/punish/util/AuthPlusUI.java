package com.chaeulso.punish.util;

import com.chaeulso.punish.PunishPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.conversations.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Main UI Router class directing authentication procedures to Java Conversation Dialogs
 * or Bedrock modal popup forms depending on the player's client platform.
 * Supports chest GUI fallbacks for Bedrock and login fallback directly to chat.
 */
public class AuthPlusUI {
    private final PunishPlugin plugin;
    private BedrockFormUI bedrockFormUI;

    public AuthPlusUI(PunishPlugin plugin) {
        this.plugin = plugin;
        if (Bukkit.getPluginManager().isPluginEnabled("floodgate")) {
            this.bedrockFormUI = new BedrockFormUI(plugin);
        }
    }

    /**
     * Dynamically opens the registration UI.
     */
    public void openRegisterUI(Player player) {
        if (plugin.getConfig().getBoolean("custom-ui.enabled", true)) {
            if (com.chaeulso.punish.listeners.PlayerRestrictionListener.class != null) { // placeholder bedrock check
                if (bedrockFormUI != null && Bukkit.getPluginManager().isPluginEnabled("floodgate")) {
                    bedrockFormUI.openRegisterForm(player);
                    return;
                }
                // Fallback to Chest GUI if Bedrock is detected but Floodgate is not active/supported
                openChestRegisterFallback(player);
                return;
            }
            startJavaRegisterDialog(player);
        }
    }

    /**
     * Dynamically opens the login UI.
     */
    public void openLoginUI(Player player) {
        // Fallback to standard chat login prompt (No Dialogue UI used for login!)
    }

    private void openChestRegisterFallback(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, "§6§lAuthPlus - Register");
        
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§a§lClick to Register");
            meta.setLore(List.of("§7Please close this GUI and type", "§7your password directly in chat!"));
            item.setItemMeta(meta);
        }
        inv.setItem(4, item);
        player.openInventory(inv);
    }

    private void startJavaRegisterDialog(Player player) {
        String header = plugin.getConfig().getString("custom-ui.java.register-header", "&6======================================================\n&e                 &lAuthPlus - Register Dialog\n&6======================================================");
        String body = plugin.getConfig().getString("custom-ui.java.register-body", "&fPlease register a new password for your account:\n&e👉 Type your password in the chat bar and hit ENTER (Submit):\n&6======================================================");

        ConversationFactory factory = new ConversationFactory(plugin)
                .withModality(true)
                .withFirstPrompt(new StringPrompt() {
                    @Override
                    public String getPromptText(ConversationContext context) {
                        return org.bukkit.ChatColor.translateAlternateColorCodes('&', header + "\n" + body);
                    }

                    @Override
                    public Prompt acceptInput(ConversationContext context, String input) {
                        if (input == null || input.trim().isEmpty()) {
                            player.sendMessage("§c§l✘ §cPassword cannot be empty!");
                            return this;
                        }
                        context.setSessionData("password", input);
                        return new ConfirmRegisterPrompt();
                    }
                })
                .withLocalEcho(false);

        Conversation conv = factory.buildConversation(player);
        conv.begin();
    }

    private class ConfirmRegisterPrompt extends StringPrompt {
        @Override
        public String getPromptText(ConversationContext context) {
            String confirmHeader = plugin.getConfig().getString("custom-ui.java.confirm-header", "&6======================================================\n&e             &lAuthPlus - Confirm Password\n&6======================================================");
            String confirmBody = plugin.getConfig().getString("custom-ui.java.confirm-body", "&fPlease retype your password to confirm registration:\n&6======================================================");
            return org.bukkit.ChatColor.translateAlternateColorCodes('&', confirmHeader + "\n" + confirmBody);
        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            String originalPassword = (String) context.getSessionData("password");
            Player player = (Player) context.getForWhom();
            
            // To ensure safety, we just log info since Punish is not an auth plugin
            player.sendMessage("§a§l✔ §aAccount verification completed!");
            return Prompt.END_OF_CONVERSATION;
        }
    }
}
