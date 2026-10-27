package dev.darcosse.ultrabeasts;

import dev.darcosse.ultrabeasts.config.ConfigManager;
import dev.darcosse.ultrabeasts.platform.Platform;
import dev.darcosse.ultrabeasts.platform.PlatformAdapter;
import dev.darcosse.ultrabeasts.registry.ModEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Loader-independent entry point of the mod.
 * <p>
 * Each loader module registers its own entities, particles and sounds first, then calls
 * {@link #init(PlatformAdapter)} so the common code can start with everything in place.
 */
public final class UltraBeasts {

    /**
     * Namespace of every registry entry, resource and config folder of the mod. Changing it
     * breaks existing worlds (unknown entities, missing dimension) and orphans configs.
     */
    public static final String MOD_ID = "cobblemon_ultrabeast";

    /** Shared logger; the name is what shows in brackets in the server log. */
    public static final Logger LOGGER = LogManager.getLogger("Ultra-Beasts");

    /** Static entry point only, never instantiated. */
    private UltraBeasts() {
    }

    /**
     * Initialises the common part of the mod: platform services, configuration and
     * Cobblemon event subscriptions.
     *
     * @param adapter the running loader's implementation of the platform services
     */
    public static void init(PlatformAdapter adapter) {
        // Must come first: the config loader resolves its folder through the adapter.
        Platform.set(adapter);

        ConfigManager.loadConfig();
        ModEvents.init();

        LOGGER.info("Ultra-Beasts mod initialized!");
    }
}
