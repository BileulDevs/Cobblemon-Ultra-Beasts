package dev.darcosse.ultrabeasts.fabric.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
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

    public static void loadConfig() {
        File configFile = new File(FabricLoader.getInstance().getConfigDir().toFile(), CONFIG_FILE);

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
        File configFile = new File(FabricLoader.getInstance().getConfigDir().toFile(), CONFIG_FILE);
        try (FileWriter writer = new FileWriter(configFile)) {
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