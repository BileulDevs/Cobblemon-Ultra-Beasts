package dev.darcosse.ultrabeasts.fabric;

import dev.darcosse.ultrabeasts.fabric.commands.UltraBeastsCommands;
import dev.darcosse.ultrabeasts.fabric.config.ConfigManager;
import dev.darcosse.ultrabeasts.fabric.handler.UnbreakableBlocksHandler;
import dev.darcosse.ultrabeasts.fabric.handler.VoidFallHandler;
import dev.darcosse.ultrabeasts.fabric.registry.*;
import dev.darcosse.ultrabeasts.fabric.registry.ModEvents;
import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class UltraBeasts implements ModInitializer {
    public static final String MOD_ID = "cobblemon_ultrabeast";
    public static Logger LOGGER = LogManager.getLogger("Ultra-Beasts");

    @Override
    public void onInitialize() {
        ConfigManager.loadConfig();

        ModParticles.register();
        ModBlocks.registerBlocks();
        ModItems.registerItems();
        ModGenerators.initialize();
        ModEntities.initialize();
        ModSounds.registerSounds();

        UltraBeastsCommands.registerCommands();
        VoidFallHandler.initialize();
        UnbreakableBlocksHandler.initialize();
        ModEvents.initialize();

        LOGGER.info("Ultra-Beasts mod initialized!");
    }
}