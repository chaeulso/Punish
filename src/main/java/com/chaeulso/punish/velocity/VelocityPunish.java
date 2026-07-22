package com.chaeulso.punish.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import org.slf4j.Logger;

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

    @Inject
    public VelocityPunish(Logger logger) {
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("Punish Velocity Proxy Module initialized successfully!");
    }
}
