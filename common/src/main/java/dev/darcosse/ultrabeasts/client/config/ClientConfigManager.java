package dev.darcosse.ultrabeasts.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import dev.darcosse.ultrabeasts.UltraBeasts;
import dev.darcosse.ultrabeasts.config.ConfigManager;
import dev.darcosse.ultrabeasts.platform.Platform;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Owns the player's own settings, in {@code config/cobblemon_ultrabeast/client.json}, next
 * to the server's {@code main.json}.
 * <p>
 * Client only: a dedicated server never loads this class, so it never creates the file.
 * Same rules as {@link ConfigManager}: never throws, falls back to defaults on a broken
 * file without overwriting it, and writes the file back after a good read so that settings
 * added by a newer version appear in it.
 */
public final class ClientConfigManager {

    /** Client settings file, inside the mod's config folder. */
    private static final String CONFIG_FILE = "client.json";

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    /** Read on the client thread only, so no volatile needed. */
    private static UltraBeastsClientConfig config = new UltraBeastsClientConfig();

    /** Static access only, never instantiated. */
    private ClientConfigManager() {
    }

    /**
     * Loads the file, creating it with defaults if it does not exist. Called at client start,
     * and again before each use that matters, so a setting changed by hand applies without
     * restarting the game.
     */
    public static void load() {
        Path file = configFile();

        if (Files.notExists(file)) {
            config = new UltraBeastsClientConfig();
            save();
            UltraBeasts.LOGGER.info("Created default client config at {}", file);
            return;
        }

        UltraBeastsClientConfig loaded = read(file);
        if (loaded == null) {
            // Kept as is: overwriting it would destroy what the player was editing.
            config = new UltraBeastsClientConfig();
            UltraBeasts.LOGGER.warn("Could not read {}, using defaults until it is fixed", file);
            return;
        }

        config = loaded;
        save();
    }

    /** Whether arriving in Ultra-Space plays the encounter cinematic. */
    public static boolean isEncounterCinematicEnabled() {
        return config.ENCOUNTER_CINEMATIC;
    }

    /** Writes the settings, creating the folder first. Failures are logged, not thrown. */
    private static void save() {
        Path file = configFile();
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException e) {
            UltraBeasts.LOGGER.error("Could not write {}: {}", file, e.getMessage());
        }
    }

    /** Parses the file, or returns null if it is unreadable, empty or not valid JSON. */
    private static UltraBeastsClientConfig read(Path file) {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, UltraBeastsClientConfig.class);
        } catch (IOException | JsonParseException e) {
            UltraBeasts.LOGGER.warn("Invalid client config file {}: {}", file, e.getMessage());
            return null;
        }
    }

    /** Resolves {@code <config root>/cobblemon_ultrabeast/client.json}. */
    private static Path configFile() {
        return Platform.get().getConfigDir().resolve(ConfigManager.CONFIG_FOLDER).resolve(CONFIG_FILE);
    }
}
