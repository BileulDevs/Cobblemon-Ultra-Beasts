package dev.darcosse.ultrabeasts.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.darcosse.ultrabeasts.platform.Platform;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ConfigManager {

    private static final String CONFIG_FILE = "ultrabeasts.json";
    private static UltraBeastsConfig config;
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static File configFile() {
        return new File(Platform.get().getConfigDir().toFile(), CONFIG_FILE);
    }

    public static void loadConfig() {
        File configFile = configFile();

        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                config = GSON.fromJson(reader, UltraBeastsConfig.class);
            } catch (IOException e) {
                config = new UltraBeastsConfig();
            }
        } else {
            config = new UltraBeastsConfig();
            saveConfig();
        }
    }

    public static void saveConfig() {
        try (FileWriter writer = new FileWriter(configFile())) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static int getWormholeSpawnChance() {
        if (config == null) loadConfig();
        return config.WORMHOLE_SPAWN_CHANCE;
    }

    public static int getTrySpawnInterval() {
        if (config == null) loadConfig();
        return config.TRY_SPAWN_INTERVAL;
    }

    public static void reloadConfig() {
        config = null;
        loadConfig();
    }

    public static int getCurrentWormholeSpawnChance() {
        if (config == null) loadConfig();
        return config.WORMHOLE_SPAWN_CHANCE;
    }
}
