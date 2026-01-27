package dev.darcosse.ultrabeasts.fabric;

import dev.darcosse.ultrabeasts.fabric.commands.UltraBeastsCommands;
import dev.darcosse.ultrabeasts.fabric.config.ConfigManager;
import dev.darcosse.ultrabeasts.fabric.handler.UnbreakableBlocksHandler;
import dev.darcosse.ultrabeasts.fabric.handler.VoidFallHandler;
import dev.darcosse.ultrabeasts.fabric.registry.*;
import dev.darcosse.ultrabeasts.fabric.entity.WormholeEntity;
import dev.darcosse.ultrabeasts.fabric.events.EventsHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Random;

public class UltraBeasts implements ModInitializer {
    public static final String MOD_ID = "cobblemon_ultrabeast";
    public static Logger LOGGER = LogManager.getLogger("Ultra-Beasts");

    private int tickCounter = 0;
    private static final int CHECK_INTERVAL = ConfigManager.getTrySpawnInterval(); // En tick

    @Override
    public void onInitialize() {
        // Charger la config
        ConfigManager.loadConfig();

        // Initialisations
        ModParticles.register();
        ModBlocks.registerBlocks();
        ModItems.registerItems();
        ModGenerators.initialize();
        ModEntities.initialize();
        ModSounds.registerSounds();

        UltraBeastsCommands.registerCommands();
        VoidFallHandler.initialize();
        UnbreakableBlocksHandler.initialize();
        EventsHandler.initializeEvents();

        // Tick serveur
        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);

        // Nettoyage à l'arrêt
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStopping);

        LOGGER.info("Ultra-Beasts mod initialized!");
    }

    private void onServerTick(MinecraftServer server) {
        tickCounter++;
        if (tickCounter % CHECK_INTERVAL != 0) return;

        server.getWorlds().forEach(world -> {
            Random javaRandom = new Random();
            WormholeEntity.tryRandomSpawn(world, javaRandom);
        });
    }

    private void onServerStopping(MinecraftServer server) {
        LOGGER.info("Server stopping, cleaning up entities...");
        for (ServerWorld world : server.getWorlds()) {
            world.getEntitiesByType(net.minecraft.entity.EntityType.BLOCK_DISPLAY, e -> true)
                    .forEach(entity -> entity.discard());
        }
        LOGGER.info("Cleanup complete.");
    }
}