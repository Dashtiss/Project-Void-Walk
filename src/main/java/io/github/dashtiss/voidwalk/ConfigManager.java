package io.github.dashtiss.voidwalk;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import net.minecraft.util.JsonHelper;
import org.slf4j.Logger;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigManager {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE = "voidwalk_config.json";

    public static ConfigData DATA = new ConfigData();

    public static class ConfigData {
        public boolean bountyEnabled = true;
        public int supplyDropHeight = 80;
        public int crateIntervalMinTicks = 36000;
        public int crateIntervalMaxTicks = 72000;
        public int captureTimeoutTicks = 600;
        public int vanishDetectionRange = 15;
        public boolean debugMode = false;
    }

    /**
     * Load configuration from the server root directory
     */
    public static void loadConfig() {
        Path configPath = Paths.get(CONFIG_FILE);
        if (!Files.exists(configPath)) {
            DATA = new ConfigData();
            saveConfig();
            LOGGER.info("Created new config file: {}", CONFIG_FILE);
            return;
        }

        try (Reader reader = new FileReader(configPath.toFile())) {
            DATA = GSON.fromJson(reader, ConfigData.class);
            LOGGER.info("Loaded config with {} drop height, bounty {}", DATA.supplyDropHeight, DATA.bountyEnabled ? "enabled" : "disabled");
        } catch (Exception e) {
            LOGGER.error("Failed to load config, using defaults", e);
            DATA = new ConfigData();
        }
    }

    /**
     * Save current configuration to file
     */
    public static void saveConfig() {
        try (Writer writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(DATA, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to save config", e);
        }
    }

    /**
     * Helper to reload config via command or runtime
     */
    public static void reloadConfig() {
        loadConfig();
        // Apply runtime updates where needed
        VoidWalk.BOUNTY_ENABLED = DATA.bountyEnabled;
    }
}
