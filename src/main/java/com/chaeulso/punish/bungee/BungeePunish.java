package com.chaeulso.punish.bungee;

import com.chaeulso.punish.util.ProxyConfigLoader;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.ProxyServer;

/**
 * BungeeCord native proxy wrapper for Punish v26.0.1.
 */
public class BungeePunish extends Plugin {
    @Override
    public void onEnable() {
        // Automatically save all resource files on BungeeCord data folder
        ProxyConfigLoader.saveResources(getDataFolder());

        ProxyServer.getInstance().getConsole().sendMessage(new net.md_5.bungee.api.chat.TextComponent(
                "§c§lPunish §8» §aBungeeCord Proxy Module loaded successfully! Configs generated."
        ));
    }

    @Override
    public void onDisable() {
        ProxyServer.getInstance().getConsole().sendMessage(new net.md_5.bungee.api.chat.TextComponent(
                "§c§lPunish §8» §cBungeeCord Proxy Module disabled."
        ));
    }
}
