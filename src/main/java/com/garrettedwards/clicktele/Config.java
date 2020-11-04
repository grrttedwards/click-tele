package com.garrettedwards.clicktele;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Properties;

public class Config {
    private static final Logger LOGGER = LogManager.getLogger();

    private Duration cooldownSeconds = Duration.ofSeconds(10);
    private int maxDistance = 80;

    private final Path propertiesFilePath;
    private Properties properties;

    public Config(Path workingDir) {
        this.propertiesFilePath = workingDir.resolve("clicktele.properties");
    }

    public Duration getCooldownSeconds() {
        return cooldownSeconds;
    }

    public void setCooldownSeconds(Duration cooldownSeconds) {
        this.cooldownSeconds = cooldownSeconds;
        storeValue("cooldown-seconds", (int) cooldownSeconds.getSeconds());
    }

    public int getMaxDistance() {
        return maxDistance;
    }

    public void setMaxDistance(int maxDistance) {
        this.maxDistance = maxDistance;
        storeValue("max-distance", maxDistance);
    }

    public void load() {
        properties = new Properties();
        try (InputStream stream = new FileInputStream(propertiesFilePath.toFile())) {
            properties.load(stream);
        } catch (FileNotFoundException ignored) {
        } catch (IOException e) {
            LOGGER.warn("Failed to read configuration", e);
        }

        cooldownSeconds = Duration.ofSeconds(retrieveValue("cooldown-seconds", (int) cooldownSeconds.getSeconds()));
        maxDistance = retrieveValue("max-distance", maxDistance);
    }

    private void storeValue(String key, int value) {
        properties.setProperty(key, String.valueOf(value));
        try (OutputStream stream = new FileOutputStream(propertiesFilePath.toFile())) {
            properties.store(stream, "ClickTele configuration values");
        } catch (IOException e) {
            LOGGER.warn("Failed to read configuration", e);
        }
    }

    private int retrieveValue(String key, int defaultValue) {
        String val = properties.getProperty(key);
        if (val == null) {
            properties.setProperty(key, String.valueOf(defaultValue));
            return defaultValue;
        } else {
            try {
                return Integer.parseInt(val);
            } catch (NumberFormatException e) {
                properties.setProperty(key, String.valueOf(defaultValue));
                return defaultValue;
            }
        }
    }
}
