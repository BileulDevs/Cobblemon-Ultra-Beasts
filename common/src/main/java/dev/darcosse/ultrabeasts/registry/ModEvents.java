package dev.darcosse.ultrabeasts.registry;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import dev.darcosse.ultrabeasts.UltraBeasts;
import dev.darcosse.ultrabeasts.config.ConfigManager;
import dev.darcosse.ultrabeasts.dimension.UltraSpaceStructureManager;
import dev.darcosse.ultrabeasts.entity.WormholeEntity;
import dev.darcosse.ultrabeasts.handler.CaptureUltraBeastHandler;
import dev.darcosse.ultrabeasts.util.ServerScheduler;
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

    private static final Random RANDOM = new Random();

    /** How often the exit portal is checked, in ticks. */
    private static final int PORTAL_CHECK_INTERVAL = 20;

    private static int spawnTickCounter = 0;
    private static int portalTickCounter = 0;

    public static void init() {
        CobblemonEvents.POKEDEX_DATA_CHANGED_POST.subscribe(
                Priority.HIGHEST, CaptureUltraBeastHandler.initialize()
        );

        UltraBeasts.LOGGER.info("Registering Events for " + UltraBeasts.MOD_ID);
    }

    public static void onServerTick(MinecraftServer server) {
        ServerScheduler.tick(server);

        checkExitPortal(server);
        trySpawnWormhole(server);
    }

    /**
     * Cheap safety net: the exit portal is an entity, and entities go away on
     * server restart or chunk unload. A player stuck inside a closed structure
     * with no portal has no way out.
     */
    private static void checkExitPortal(MinecraftServer server) {
        if (++portalTickCounter < PORTAL_CHECK_INTERVAL) return;
        portalTickCounter = 0;

        ServerLevel ultraSpace = server.getLevel(ModDimensions.ULTRA_SPACE_DIMENSION);
        if (ultraSpace == null || ultraSpace.players().isEmpty()) return;

        UltraSpaceStructureManager.ensureReturnPortal(ultraSpace);
    }

    private static void trySpawnWormhole(MinecraftServer server) {
        // Read from the config on each check rather than caching it in a static
        // final: the old version was initialised at class-load time, before
        // loadConfig() had run, and never picked up /ultrabeasts reload.
        if (++spawnTickCounter < ConfigManager.getTrySpawnInterval()) return;
        spawnTickCounter = 0;

        // No wormhole while anyone is inside Ultra-Space.
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.level().dimension().equals(ModDimensions.ULTRA_SPACE_DIMENSION)) {
                return;
            }
        }

        WormholeEntity.tryRandomSpawn(server.overworld(), RANDOM);
    }

    /**
     * Clears leftover mod entities.
     *
     * The exit portal in Ultra-Space is deliberately spared when a structure is
     * still standing: destroying it would strand whoever is inside.
     */
    public static void cleanUp(MinecraftServer server) {
        UltraBeasts.LOGGER.info("Cleaning up Ultra-Beasts entities...");

        for (ServerLevel level : server.getAllLevels()) {
            level.getEntities(EntityType.BLOCK_DISPLAY,
                            e -> e.getTags().contains("wormhole_animation_block"))
                    .forEach(Entity::discard);
            level.getEntities(ModEntities.WORMHOLE, e -> true).forEach(Entity::discard);
            level.getEntities(ModEntities.WORMHOLE_ANIMATION, e -> true).forEach(Entity::discard);
            level.getEntities(ModEntities.RETURN_WORMHOLE, e -> true).forEach(Entity::discard);
        }

        UltraBeasts.LOGGER.info("Cleaning up Ultra-Beasts entities finished!");
    }

    public static void onServerStarted(MinecraftServer server) {
        cleanUp(server);

        // cleanUp() just removed the exit portal along with everything else.
        // Put it back if a structure survived in the save, so a player who
        // logged out inside Ultra-Space still finds a way home.
        ServerLevel ultraSpace = server.getLevel(ModDimensions.ULTRA_SPACE_DIMENSION);
        if (ultraSpace != null) {
            UltraSpaceStructureManager.ensureReturnPortal(ultraSpace);
        }
    }

    public static void onServerStopping(MinecraftServer server) {
        ServerScheduler.clear();
        cleanUp(server);
    }
}
