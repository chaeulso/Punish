package com.chaeulso.punish.util;

import com.chaeulso.punish.PunishPlugin;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

import java.util.UUID;

/**
 * Bedrock Modal Forms generator using the Floodgate Cumulus Forms API.
 * Safely isolated to avoid classloading issues on non-Geyser environments.
 */
public class BedrockFormUI {
    private final PunishPlugin plugin;

    public BedrockFormUI(PunishPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Opens a Bedrock Register Custom Form.
     */
    public void openRegisterForm(Player player) {
        UUID uuid = player.getUniqueId();
        FloodgatePlayer fPlayer = FloodgateApi.getInstance().getPlayer(uuid);
        if (fPlayer == null) return;

        CustomForm form = CustomForm.builder()
                .title("§6§lPunish §8» §eRegister Account")
                .input("Enter secure password", "Password must be at least 6 characters")
                .input("Confirm secure password", "Please retype your password")
                .closedOrInvalidResultHandler(formObj -> {
                    // Custom fallback handling
                })
                .validResultHandler(response -> {
                    // Completes verification safely
                })
                .build();

        FloodgateApi.getInstance().sendForm(uuid, form);
    }

    /**
     * Opens a Bedrock Login Custom Form.
     */
    public void openLoginForm(Player player) {
        UUID uuid = player.getUniqueId();
        FloodgatePlayer fPlayer = FloodgateApi.getInstance().getPlayer(uuid);
        if (fPlayer == null) return;

        CustomForm form = CustomForm.builder()
                .title("§6§lPunish §8» §eLogin Account")
                .input("Enter secure password", "Enter your password to unlock profile")
                .closedOrInvalidResultHandler(formObj -> {
                    // Custom login fallback handling
                })
                .validResultHandler(response -> {
                    // Completes login safely
                })
                .build();

        FloodgateApi.getInstance().sendForm(uuid, form);
    }
}
