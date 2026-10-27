package dev.darcosse.ultrabeasts.registry;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import dev.darcosse.ultrabeasts.UltraBeasts;
import dev.darcosse.ultrabeasts.config.ConfigManager;
import dev.darcosse.ultrabeasts.dimension.UltraSpaceStructureManager;
import dev.darcosse.ultrabeasts.entity.WormholeEntity;
import dev.darcosse.ultrabeasts.entity.WormholeSpawnAnimation;
import dev.darcosse.ultrabeasts.handler.CaptureUltraBeastHandler;
import dev.darcosse.ultrabeasts.util.ServerScheduler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.Random;

/**
 * Loader-independent server lifecycle logic: the per-tick loop, start-up and shutdown
 * clean-up, and the Cobblemon event subscriptions.
 * <p>
 * Each loader module wires {@link #onServerTick}, {@link #onServerStarted} and
 * {@link #onServerStopping} to its own event bus. Server side only.
 */
public final class ModEvents {

    private static final Random RANDOM = new Random();

    /**
     * Ticks between two checks that the exit portal still exists. Lower reacts faster to a
     * lost portal, at the cost of one entity lookup in Ultra-Space per check.
     */
    private static final int PORTAL_CHECK_INTERVAL = 20;

    private static int spawnTickCounter = 0;
    private static int portalTickCounter = 0;

    /** Static event handlers only, never instantiated. */
    private ModEvents() {
    }

    /** Subscribes to the Cobblemon events the mod reacts to. Called once from {@code UltraBeasts.init}. */
    public static void init() {
        CobblemonEvents.POKEDEX_DATA_CHANGED_POST.subscribe(
                Priority.HIGHEST, CaptureUltraBeastHandler.initialize()
        );

        UltraBeasts.LOGGER.info("Registering Events for {}", UltraBeasts.MOD_ID);
    }

    /**
     * Per-tick server work. Runs 20 times a second: everything below either returns early
     * on a counter or does constant-time work.
     */
    public static void onServerTick(MinecraftServer server) {
        ServerScheduler.tick();

        checkExitPortal(server);
        trySpawnWormhole(server);
    }

    /**
     * Cheap safety net: the exit portal is an entity, and entities go away on server restart
     * or chunk unload. A player stuck inside a closed structure with no portal has no way out.
     */
    private static void checkExitPortal(MinecraftServer server) {
        if (++portalTickCounter < PORTAL_CHECK_INTERVAL) return;
        portalTickCounter = 0;

        ServerLevel ultraSpace = server.getLevel(ModDimensions.ULTRA_SPACE_DIMENSION);
        if (ultraSpace == null || ultraSpace.players().isEmpty()) return;

        UltraSpaceStructureManager.ensureReturnPortal(ultraSpace);
    }

    /** Rolls for a random wormhole in the overworld once every configured interval. */
    private static void trySpawnWormhole(MinecraftServer server) {
        // Read from the config on each check rather than cached in a static final: a cached
        // value was initialised at class-load time, before loadConfig() had run, and never
        // picked up /ultrabeasts reload.
        if (++spawnTickCounter < ConfigManager.getTrySpawnInterval()) return;
        spawnTickCounter = 0;

        // No wormhole while anyone is inside Ultra-Space. Asking the level directly replaces
        // a scan of every online player.
        ServerLevel ultraSpace = server.getLevel(ModDimensions.ULTRA_SPACE_DIMENSION);
        if (ultraSpace != null && !ultraSpace.players().isEmpty()) return;

        WormholeEntity.tryRandomSpawn(server.overworld(), RANDOM);
    }

    /**
     * Removes every leftover mod entity in every level: wormholes, spawn animations, their
     * debris, and exit portals. {@link #onServerStarted} puts the exit portal back afterwards
     * if a structure is still standing.
     */
    public static void cleanUp(MinecraftServer server) {
        UltraBeasts.LOGGER.info("Cleaning up Ultra-Beasts entities...");

        for (ServerLevel level : server.getAllLevels()) {
            level.getEntities(EntityType.BLOCK_DISPLAY,
                            e -> e.getTags().contains(WormholeSpawnAnimation.BLOCK_TAG))
                    .forEach(Entity::discard);
            level.getEntities(ModEntities.WORMHOLE, e -> true).forEach(Entity::discard);
            level.getEntities(ModEntities.WORMHOLE_ANIMATION, e -> true).forEach(Entity::discard);
            level.getEntities(ModEntities.RETURN_WORMHOLE, e -> true).forEach(Entity::discard);
        }

        UltraBeasts.LOGGER.info("Cleaning up Ultra-Beasts entities finished!");
    }

    /** Clears stale entities from the last session and restores the exit portal if needed. */
    public static void onServerStarted(MinecraftServer server) {
        cleanUp(server);

        // cleanUp() just removed the exit portal along with everything else. Put it back if a
        // structure survived in the save, so a player who logged out inside Ultra-Space still
        // finds a way home.
        ServerLevel ultraSpace = server.getLevel(ModDimensions.ULTRA_SPACE_DIMENSION);
        if (ultraSpace != null) {
            UltraSpaceStructureManager.ensureReturnPortal(ultraSpace);
        }
    }

    /** Drops pending tasks and mod entities so nothing carries over to the next world. */
    public static void onServerStopping(MinecraftServer server) {
        ServerScheduler.clear();
        cleanUp(server);
    }
}
