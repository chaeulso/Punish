package com.chaeulso.punish.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Handles extracting and writing configurations for Proxy platforms (BungeeCord and Velocity).
 */
public class ProxyConfigLoader {
    /**
     * Extracts default configs from jar to proxy directory if they don't exist.
     */
    public static void saveResources(File dataFolder) {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        String[] files = {
            "config.yml", "messages_en.yml", "messages_id.yml", "messages_cn.yml", "messages_es.yml", "messages_ru.yml",
            "permissions.yml", "database.yml", "jail.yml", "gui.yml", "reasons.yml", "blacklist.yml", "whitelist.yml",
            "timezone.yml", "sounds.yml", "webhook.yml", "templates.yml"
        };

        for (String fileName : files) {
            File target = new File(dataFolder, fileName);
            if (!target.exists()) {
                try (InputStream is = ProxyConfigLoader.class.getClassLoader().getResourceAsStream(fileName)) {
                    if (is != null) {
                        try (OutputStream os = new FileOutputStream(target)) {
                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            while ((bytesRead = is.read(buffer)) != -1) {
                                os.write(buffer, 0, bytesRead);
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[Punish] Failed to save proxy resource " + fileName + ": " + e.getMessage());
                }
            }
        }
    }
}
