package dev.darcosse.ultrabeasts.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import dev.darcosse.ultrabeasts.UltraBeasts;
import dev.darcosse.ultrabeasts.platform.Platform;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Owns the mod configuration: loads it from {@code config/cobblemon_ultrabeast/main.json},
 * migrates the pre-4.1 {@code config/ultrabeasts.json} once, repairs out-of-range values
 * and exposes the result to the rest of the mod.
 * <p>
 * Loader-agnostic: the config root comes from {@link Platform}, so Fabric and NeoForge
 * share the same layout. Server-side only, nothing here is synced to clients.
 */
public final class ConfigManager {

    /**
     * Sub-folder of the loader's config directory. It matches the mod id so that every file
     * this mod may add later sits in one place. Changing it orphans existing configs: users
     * would silently fall back to defaults.
     */
    private static final String CONFIG_FOLDER = UltraBeasts.MOD_ID;

    /** Main settings file inside {@link #CONFIG_FOLDER}. */
    private static final String CONFIG_FILE = "main.json";

    /** Location used up to 4.0, directly in the config root. Only ever read for migration. */
    private static final String LEGACY_CONFIG_FILE = "ultrabeasts.json";

    /**
     * Appended to the legacy file once migrated. Renaming instead of deleting keeps the
     * user's original values recoverable and prevents the migration from running twice.
     */
    private static final String MIGRATED_SUFFIX = ".migrated";

    /** Default "1 in N" odds for a spawn attempt to open a wormhole. Higher means rarer. */
    public static final int DEFAULT_SPAWN_CHANCE = 2500;

    /**
     * Default ticks between two spawn attempts (400 ticks = 20 s). Lower means more frequent
     * wormholes and more frequent spawn checks on the server thread.
     */
    public static final int DEFAULT_TRY_INTERVAL = 400;

    /** Below this, {@code Random.nextInt(bound)} would throw. */
    public static final int MIN_SPAWN_CHANCE = 1;

    /** A spawn attempt every tick would be pointless and expensive. */
    public static final int MIN_TRY_INTERVAL = 20;

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    // volatile because /ultrabeasts reload may run from the console thread while the tick
    // loop reads the values on the server thread.
    private static volatile UltraBeastsConfig config;

    /** Utility class, never instantiated. */
    private ConfigManager() {
    }

    /**
     * Loads the configuration into memory, migrating or creating the file as needed. Never
     * throws: any problem is logged and defaults are used, because a broken config file must
     * not prevent the server from starting.
     */
    public static void loadConfig() {
        Path configFile = configFile();

        if (Files.notExists(configFile)) {
            migrateLegacyConfig(legacyConfigFile(), configFile);
        }

        if (Files.notExists(configFile)) {
            config = new UltraBeastsConfig();
            saveConfig();
            UltraBeasts.LOGGER.info("Created default config at {}", configFile);
            return;
        }

        UltraBeastsConfig loaded = read(configFile);
        if (loaded == null) {
            // The broken file is deliberately NOT overwritten with defaults: that would
            // destroy whatever the user was in the middle of editing.
            config = new UltraBeastsConfig();
            UltraBeasts.LOGGER.warn("Could not read {}, falling back to defaults until it is fixed", configFile);
            return;
        }

        config = loaded;
        if (validate()) {
            saveConfig();
        }
    }

    /**
     * Writes the in-memory configuration to {@code main.json}, creating the folder first.
     * Failures are logged, not thrown: the in-memory values remain usable.
     */
    public static void saveConfig() {
        UltraBeastsConfig snapshot = config;
        if (snapshot == null) return;

        Path configFile = configFile();
        try {
            // The sub-folder does not exist on first launch, and opening a writer does not create it.
            Files.createDirectories(configFile.getParent());
            try (Writer writer = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8)) {
                GSON.toJson(snapshot, writer);
            }
        } catch (IOException e) {
            UltraBeasts.LOGGER.error("Could not write {}: {}", configFile, e.getMessage());
        }
    }

    /** Discards the in-memory values and reads the file again. Backs {@code /ultrabeasts reload}. */
    public static void reloadConfig() {
        config = null;
        loadConfig();
    }

    /** Returns the "1 in N" odds that a spawn attempt opens a wormhole. */
    public static int getWormholeSpawnChance() {
        return get().WORMHOLE_SPAWN_CHANCE;
    }

    /** Returns the number of ticks between two wormhole spawn attempts. */
    public static int getTrySpawnInterval() {
        return get().TRY_SPAWN_INTERVAL;
    }

    /**
     * Same value as {@link #getWormholeSpawnChance()}. Kept because the commands use this
     * name; both always return the live value.
     */
    public static int getCurrentWormholeSpawnChance() {
        return getWormholeSpawnChance();
    }

    /** Returns the loaded configuration, loading it first if nothing has done so yet. */
    private static UltraBeastsConfig get() {
        UltraBeastsConfig snapshot = config;
        if (snapshot == null) {
            loadConfig();
            snapshot = config;
        }
        return snapshot;
    }

    /** Resolves {@code <config root>/cobblemon_ultrabeast/main.json}. */
    private static Path configFile() {
        return Platform.get().getConfigDir().resolve(CONFIG_FOLDER).resolve(CONFIG_FILE);
    }

    /** Resolves the pre-4.1 location, {@code <config root>/ultrabeasts.json}. */
    private static Path legacyConfigFile() {
        return Platform.get().getConfigDir().resolve(LEGACY_CONFIG_FILE);
    }

    /**
     * Copies the values of the pre-4.1 file to the new location, then renames the old file
     * with {@link #MIGRATED_SUFFIX}. Does nothing if there is no legacy file.
     */
    private static void migrateLegacyConfig(Path legacyFile, Path newFile) {
        if (Files.notExists(legacyFile)) return;

        UltraBeastsConfig legacy = read(legacyFile);
        if (legacy == null) {
            // Left in place so the user can copy the values by hand; the new file is
            // created with defaults right after.
            UltraBeasts.LOGGER.error("Could not read legacy config {}; it was not migrated, copy its values to {} by hand",
                    legacyFile, newFile);
            return;
        }

        config = legacy;
        validate();
        saveConfig();

        if (Files.notExists(newFile)) {
            // saveConfig() already logged why; keep the legacy file so nothing is lost.
            return;
        }

        try {
            Files.move(legacyFile, legacyFile.resolveSibling(LEGACY_CONFIG_FILE + MIGRATED_SUFFIX),
                    StandardCopyOption.REPLACE_EXISTING);
            UltraBeasts.LOGGER.info("Migrated config from {} to {}", legacyFile, newFile);
        } catch (IOException e) {
            // Harmless: main.json now exists, so the legacy file will simply be ignored.
            UltraBeasts.LOGGER.warn("Config migrated to {}, but {} could not be renamed: {}",
                    newFile, legacyFile, e.getMessage());
        }
    }

    /**
     * Parses a config file.
     *
     * @return the parsed values, or {@code null} if the file is unreadable, empty or not
     *         valid JSON
     */
    private static UltraBeastsConfig read(Path file) {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            // Returns null for an empty file or one containing literally "null".
            return GSON.fromJson(reader, UltraBeastsConfig.class);
        } catch (IOException | JsonParseException e) {
            UltraBeasts.LOGGER.warn("Invalid config file {}: {}", file, e.getMessage());
            return null;
        }
    }

    /**
     * Clamps out-of-range values of the active configuration back into the accepted domain.
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
}
