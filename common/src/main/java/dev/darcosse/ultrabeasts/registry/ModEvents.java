package dev.darcosse.ultrabeasts.registry;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import dev.darcosse.ultrabeasts.UltraBeasts;
import dev.darcosse.ultrabeasts.config.ConfigManager;
import dev.darcosse.ultrabeasts.entity.WormholeEntity;
import dev.darcosse.ultrabeasts.handler.CaptureUltraBeastHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.Random;

/**
 * Loader-independent event logic. Each platform module wires
 * onServerTick / onServerStarted / onServerStopping to its own event bus.
 */
public class ModEvents {

    private static int tickCounter = 0;
    private static final int CHECK_INTERVAL = ConfigManager.getTrySpawnInterval();

    public static void init() {
        CobblemonEvents.POKEDEX_DATA_CHANGED_POST.subscribe(
                Priority.HIGHEST, CaptureUltraBeastHandler.initialize()
        );

        UltraBeasts.LOGGER.info("Registering Events for " + UltraBeasts.MOD_ID);
    }

    public static void onServerTick(MinecraftServer server) {
        tickCounter++;
        if (tickCounter % CHECK_INTERVAL != 0) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.level().dimension().equals(ModDimensions.ULTRA_SPACE_DIMENSION)) {
                return;
            }
        }

        server.getAllLevels().forEach(level -> {
            Random javaRandom = new Random();
            WormholeEntity.tryRandomSpawn(level, javaRandom);
        });
    }

    public static void cleanUp(MinecraftServer server) {
        UltraBeasts.LOGGER.info("Cleaning up Ultra-Beasts entities...");

        for (ServerLevel level : server.getAllLevels()) {
            level.getEntities(EntityType.BLOCK_DISPLAY,
                            entity -> entity.getTags().contains("wormhole_animation_block"))
                    .forEach(Entity::discard);
            level.getEntities(ModEntities.WORMHOLE, e -> true)
                    .forEach(Entity::discard);
            level.getEntities(ModEntities.RETURN_WORMHOLE, e -> true)
                    .forEach(Entity::discard);
            level.getEntities(ModEntities.WORMHOLE_ANIMATION, e -> true)
                    .forEach(Entity::discard);

            UltraBeasts.LOGGER.info("Cleaning {} world !", level.dimension());
        }

        WormholeEntity.clearWormhole();

        UltraBeasts.LOGGER.info("Cleaning up Ultra-Beasts entities finished!");
    }

    public static void onServerStopping(MinecraftServer server) {
        cleanUp(server);
    }

    public static void onServerStarted(MinecraftServer server) {
        cleanUp(server);
    }
}
