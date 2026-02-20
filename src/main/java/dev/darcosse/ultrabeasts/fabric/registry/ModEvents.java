package dev.darcosse.ultrabeasts.fabric.registry;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import dev.darcosse.ultrabeasts.fabric.UltraBeasts;
import dev.darcosse.ultrabeasts.fabric.config.ConfigManager;
import dev.darcosse.ultrabeasts.fabric.entity.WormholeEntity;
import dev.darcosse.ultrabeasts.fabric.handler.CaptureUltraBeastHandler;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.Random;

public class ModEvents {
    private static int tickCounter = 0;
    private static final int CHECK_INTERVAL = ConfigManager.getTrySpawnInterval();

    public static void initialize() {
        CobblemonEvents.POKEDEX_DATA_CHANGED_POST.subscribe(
                Priority.HIGHEST, CaptureUltraBeastHandler.initialize()
        );

        ServerTickEvents.END_SERVER_TICK.register(ModEvents::onServerTick);
        ServerLifecycleEvents.SERVER_STOPPING.register(ModEvents::onServerStopping);
        ServerLifecycleEvents.SERVER_STARTED.register(ModEvents::onServerStart);

        UltraBeasts.LOGGER.info("Registering Events for " + UltraBeasts.MOD_ID);
    }

    private static void onServerTick(MinecraftServer server) {
        tickCounter++;
        if (tickCounter % CHECK_INTERVAL != 0) return;

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (player.getWorld().getRegistryKey().equals(ModDimensions.ULTRA_SPACE_DIMENSION)) {
                return;
            }
        }

        server.getWorlds().forEach(world -> {
            Random javaRandom = new Random();
            WormholeEntity.tryRandomSpawn(world, javaRandom);
        });
    }

    public static void cleanUp(MinecraftServer server) {
        UltraBeasts.LOGGER.info("Cleaning up Ultra-Beasts entities...");

        for (ServerWorld world : server.getWorlds()) {
            world.getEntitiesByType(EntityType.BLOCK_DISPLAY, entity -> entity.getCommandTags().contains("wormhole_animation_block"))
                    .forEach(Entity::discard);
            world.getEntitiesByType(ModEntities.WORMHOLE, e -> true)
                    .forEach(Entity::discard);
            world.getEntitiesByType(ModEntities.RETURN_WORMHOLE, e -> true)
                    .forEach(Entity::discard);
            world.getEntitiesByType(ModEntities.WORMHOLE_ANIMATION, e -> true)
                    .forEach(Entity::discard);

            UltraBeasts.LOGGER.info("Cleaning {} world !", world.getRegistryKey());
        }

        WormholeEntity.clearWormhole();

        UltraBeasts.LOGGER.info("Cleaning up Ultra-Beasts entities finished!");
    }

    private static void onServerStopping(MinecraftServer server) {
        cleanUp(server);
    }

    private static void onServerStart(MinecraftServer server) {
        cleanUp(server);
    }
}
