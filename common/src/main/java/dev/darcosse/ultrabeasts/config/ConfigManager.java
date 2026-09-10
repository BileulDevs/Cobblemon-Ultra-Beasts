package dev.darcosse.ultrabeasts.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import dev.darcosse.ultrabeasts.UltraBeasts;
import dev.darcosse.ultrabeasts.platform.Platform;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ConfigManager {

    private static final String CONFIG_FILE = "ultrabeasts.json";

    public static final int DEFAULT_SPAWN_CHANCE = 2500;
    public static final int DEFAULT_TRY_INTERVAL = 400;

    /** Below this, Random.nextInt(bound) would throw. */
    public static final int MIN_SPAWN_CHANCE = 1;
    /** A spawn attempt every tick would be pointless and expensive. */
    public static final int MIN_TRY_INTERVAL = 20;

    private static UltraBeastsConfig config;

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static File configFile() {
        return new File(Platform.get().getConfigDir().toFile(), CONFIG_FILE);
    }

    public static void loadConfig() {
        File configFile = configFile();

        if (!configFile.exists()) {
            config = new UltraBeastsConfig();
            saveConfig();
            return;
        }

        try (FileReader reader = new FileReader(configFile)) {
            config = GSON.fromJson(reader, UltraBeastsConfig.class);
        } catch (IOException | JsonParseException e) {
            UltraBeasts.LOGGER.warn("Could not read {}, falling back to defaults: {}",
                    CONFIG_FILE, e.getMessage());
            config = null;
        }

        // Empty file, or a file containing literally "null"
        if (config == null) {
            config = new UltraBeastsConfig();
        }

        if (validate()) {
            saveConfig();
        }
    }

    /**
     * Clamps out-of-range values back into the accepted domain.
     *
     * @return true if at least one value was corrected
     */
    private static boolean validate() {
        boolean corrected = false;

        if (config.WORMHOLE_SPAWN_CHANCE < MIN_SPAWN_CHANCE) {
            UltraBeasts.LOGGER.warn("Invalid WORMHOLE_SPAWN_CHANCE={}, clamped to {}.",
                    config.WORMHOLE_SPAWN_CHANCE, MIN_SPAWN_CHANCE);
            config.WORMHOLE_SPAWN_CHANCE = MIN_SPAWN_CHANCE;
            corrected = true;
        }

        if (config.TRY_SPAWN_INTERVAL < MIN_TRY_INTERVAL) {
            UltraBeasts.LOGGER.warn("Invalid TRY_SPAWN_INTERVAL={}, clamped to {}.",
                    config.TRY_SPAWN_INTERVAL, MIN_TRY_INTERVAL);
            config.TRY_SPAWN_INTERVAL = MIN_TRY_INTERVAL;
            corrected = true;
        }

        return corrected;
    }

    public static void saveConfig() {
        if (config == null) return;

        try (FileWriter writer = new FileWriter(configFile())) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            UltraBeasts.LOGGER.error("Could not write {}: {}", CONFIG_FILE, e.getMessage());
        }
    }

    private static UltraBeastsConfig get() {
        if (config == null) loadConfig();
        return config;
    }

    public static int getWormholeSpawnChance() {
        return get().WORMHOLE_SPAWN_CHANCE;
    }

    public static int getTrySpawnInterval() {
        return get().TRY_SPAWN_INTERVAL;
    }

    public static void reloadConfig() {
        config = null;
        loadConfig();
    }

    public static int getCurrentWormholeSpawnChance() {
        return get().WORMHOLE_SPAWN_CHANCE;
    }
}
