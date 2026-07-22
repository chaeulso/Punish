package com.chaeulso.punish.velocity;

import com.chaeulso.punish.util.ProxyConfigLoader;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * Velocity native proxy wrapper for Punish v26.0.1.
 */
@Plugin(
        id = "punish",
        name = "Punish",
        version = "26.0.1",
        description = "Modern, customizable moderation & punishment plugin.",
        authors = {"Chaeulso"}
)
public class VelocityPunish {
    private final Logger logger;
    private final Path dataDirectory;

    @Inject
    public VelocityPunish(Logger logger, @DataDirectory Path dataDirectory) {
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        // Automatically save all resource files on Velocity data directory
        ProxyConfigLoader.saveResources(dataDirectory.toFile());

        logger.info("Punish Velocity Proxy Module initialized successfully! Configs generated.");
    }
}
