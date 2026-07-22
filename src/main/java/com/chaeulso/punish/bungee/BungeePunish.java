package com.chaeulso.punish.bungee;

import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.ProxyServer;

/**
 * BungeeCord native proxy wrapper for Punish v26.0.1.
 */
public class BungeePunish extends Plugin {
    @Override
    public void onEnable() {
        ProxyServer.getInstance().getConsole().sendMessage(new net.md_5.bungee.api.chat.TextComponent(
                "§c§lPunish §8» §aBungeeCord Proxy Module loaded successfully!"
        ));
    }

    @Override
    public void onDisable() {
        ProxyServer.getInstance().getConsole().sendMessage(new net.md_5.bungee.api.chat.TextComponent(
                "§c§lPunish §8» §cBungeeCord Proxy Module disabled."
        ));
    }
}
