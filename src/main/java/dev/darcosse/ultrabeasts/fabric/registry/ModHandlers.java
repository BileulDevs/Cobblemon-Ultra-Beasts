package dev.darcosse.ultrabeasts.fabric.registry;

import dev.darcosse.ultrabeasts.fabric.UltraBeasts;
import dev.darcosse.ultrabeasts.fabric.handler.FallingDamageHandler;
import dev.darcosse.ultrabeasts.fabric.handler.UnbreakableBlocksHandler;
import dev.darcosse.ultrabeasts.fabric.handler.VoidFallHandler;

public class ModHandlers {
    public static void initialize() {
        FallingDamageHandler.initialize();
        UnbreakableBlocksHandler.initialize();
        VoidFallHandler.initialize();

        UltraBeasts.LOGGER.info("Registering Handlers for " + UltraBeasts.MOD_ID);
    }
}