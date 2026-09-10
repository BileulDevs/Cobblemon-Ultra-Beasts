package dev.darcosse.ultrabeasts;

import dev.darcosse.ultrabeasts.config.ConfigManager;
import dev.darcosse.ultrabeasts.platform.Platform;
import dev.darcosse.ultrabeasts.platform.PlatformAdapter;
import dev.darcosse.ultrabeasts.registry.ModEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Common entry point. Each loader module calls init() after providing its
 * adapter and after registering its own entities / particles / sounds.
 */
public final class UltraBeasts {

    public static final String MOD_ID = "cobblemon_ultrabeast";
    public static final Logger LOGGER = LogManager.getLogger("Ultra-Beasts");

    private UltraBeasts() {
    }

    public static void init(PlatformAdapter adapter) {
        Platform.set(adapter);

        ConfigManager.loadConfig();
        ModEvents.init();

        LOGGER.info("Ultra-Beasts mod initialized!");
    }
}
