package dev.darcosse.ultrabeasts.config;

/**
 * Plain data holder that Gson maps onto {@code main.json}.
 * <p>
 * Field initializers are the defaults: Gson runs the implicit no-arg constructor first, so a
 * key missing from the file keeps its default instead of becoming 0. The upper-case field
 * names ARE the JSON keys; renaming a field silently resets that setting for every user.
 */
public class UltraBeastsConfig {

    /** "1 in N" odds that a spawn attempt opens a wormhole. */
    public int WORMHOLE_SPAWN_CHANCE = ConfigManager.DEFAULT_SPAWN_CHANCE;

    /** Ticks between two wormhole spawn attempts. */
    public int TRY_SPAWN_INTERVAL = ConfigManager.DEFAULT_TRY_INTERVAL;
}
