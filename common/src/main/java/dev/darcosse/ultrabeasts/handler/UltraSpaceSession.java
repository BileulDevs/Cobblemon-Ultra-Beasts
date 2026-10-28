package dev.darcosse.ultrabeasts.handler;

import dev.darcosse.ultrabeasts.UltraBeasts;
import dev.darcosse.ultrabeasts.dimension.ReturnPointStore;
import dev.darcosse.ultrabeasts.dimension.UltraSpaceState;
import dev.darcosse.ultrabeasts.dimension.UltraSpaceStructureManager;
import dev.darcosse.ultrabeasts.registry.ModDimensions;
import dev.darcosse.ultrabeasts.util.ServerScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import dev.darcosse.ultrabeasts.util.TeleportEffects;

import java.util.List;
import java.util.UUID;

/**
 * Owns the "one player at a time" rule of Ultra-Space.
 * <p>
 * Every path in and out of the dimension goes through here, so the rule lives in one
 * readable place instead of emerging from scattered checks. The claim itself is persisted
 * in {@link UltraSpaceState}. Server side only.
 */
public final class UltraSpaceSession {

    /**
     * How long a disconnected occupant keeps their claim, in ticks.
     *
     * The countdown ONLY runs while they are logged out. An online player can
     * stay as long as they like — hunting an Ultra Beast is not on a timer.
     * Five minutes is enough to recover from a crash, short enough that nobody
     * freezes the mod for the whole server.
     */
    public static final int GRACE_TICKS = 5 * 60 * 20;

    /** Ticks to wait before evicting on login, so the client finishes loading. */
    private static final int EVICTION_DELAY = 20;

    /** Static handlers only, never instantiated. */
    private UltraSpaceSession() {
    }

    /**
     * The single clock every lease check reads. Mixing the overworld's and Ultra-Space's game
     * time would let two checks disagree on whether a lease has expired.
     */
    private static long gameTime(MinecraftServer server) {
        return server.overworld().getGameTime();
    }

    /** Returns the persisted session state, or null if the dimension is not loaded. */
    private static UltraSpaceState state(MinecraftServer server) {
        ServerLevel ultraSpace = server.getLevel(ModDimensions.ULTRA_SPACE_DIMENSION);
        return ultraSpace == null ? null : UltraSpaceState.getServerState(ultraSpace);
    }

    /** Whether the player is currently standing in Ultra-Space. */
    public static boolean isInUltraSpace(ServerPlayer player) {
        return player.level().dimension().equals(ModDimensions.ULTRA_SPACE_DIMENSION);
    }

    /**
     * Whether this player may enter right now: the dimension is free, its lease has expired,
     * or the player already holds it.
     */
    public static boolean canEnter(ServerPlayer player) {
        UltraSpaceState state = state(player.getServer());
        if (state == null) return false;

        long now = gameTime(player.getServer());
        UUID occupant = state.getOccupant(now);

        return occupant == null || occupant.equals(player.getUUID());
    }

    /** Whether anyone, online or within their grace period, holds the dimension. Holds back wormhole spawns. */
    public static boolean isOccupied(MinecraftServer server) {
        UltraSpaceState state = state(server);
        return state != null && state.isClaimed(gameTime(server));
    }

    /** Gives the dimension to the player. Called once they are actually being sent in. */
    public static void claim(ServerPlayer player) {
        UltraSpaceState state = state(player.getServer());
        if (state != null) {
            state.claim(player.getUUID());
        }
    }

    /** Frees the dimension. Called on every way out: portal, void, or operator clear. */
    public static void release(MinecraftServer server) {
        UltraSpaceState state = state(server);
        if (state != null) {
            state.clearClaim();
        }
    }

    /**
     * Starts the grace countdown when the occupant logs out, rather than freeing the slot
     * immediately, so a crash does not cost them their Ultra Beast.
     */
    public static void onPlayerDisconnect(ServerPlayer player) {
        if (!isInUltraSpace(player)) return;

        MinecraftServer server = player.getServer();
        UltraSpaceState state = state(server);
        if (state == null) return;

        long now = gameTime(server);
        if (!state.isOccupant(player.getUUID(), now)) return;

        state.beginGrace(now, GRACE_TICKS);
        UltraBeasts.LOGGER.info("Occupant {} went offline, claim held for {} ticks.",
                player.getGameProfile().getName(), GRACE_TICKS);
    }

    /**
     * On login inside Ultra-Space: resumes the session if the player still holds it, or
     * sends them home if the dimension changed hands while they were away.
     */
    public static void onPlayerJoin(ServerPlayer player) {
        if (!isInUltraSpace(player)) return;

        MinecraftServer server = player.getServer();
        UltraSpaceState state = state(server);
        if (state == null) return;

        long now = gameTime(server);

        if (state.isOccupant(player.getUUID(), now)) {
            // Back in time: cancel the countdown, carry on.
            state.endGrace();
            UltraSpaceStructureManager.ensureReturnPortal(
                    server.getLevel(ModDimensions.ULTRA_SPACE_DIMENSION));
            return;
        }

        // Someone else holds it, or the lease lapsed. Either way this player no longer
        // belongs here. Deferred: teleporting while the client is still loading the world
        // gives unreliable results.
        ServerScheduler.schedule(EVICTION_DELAY, () ->
                evict(player, "the claim had changed hands"));
    }

    /**
     * Empties the dimension and frees the claim. Used by the operator clear command.
     * <p>
     * Releasing the claim without evicting would let a second player walk in and have
     * placeStructure() wipe the structure from under whoever is still inside, which is
     * exactly what the claim exists to prevent.
     *
     * @return how many players were sent home
     */
    public static int evictEveryoneAndRelease(MinecraftServer server) {
        ServerLevel ultraSpace = server.getLevel(ModDimensions.ULTRA_SPACE_DIMENSION);

        int evicted = 0;

        if (ultraSpace != null) {
            // Copy first: teleporting mutates the level's player list, and
            // MinecraftServer.execute() does NOT defer when already on the server thread.
            for (ServerPlayer player : List.copyOf(ultraSpace.players())) {
                evict(player, "an operator cleared the dimension");
                evicted++;
            }

            UltraSpaceStructureManager.removeStructure(ultraSpace);
        }

        release(server);

        return evicted;
    }

    /**
     * Sends a player home without ending anyone's session.
     *
     * @param reason English text for the server log only, never shown to players
     */
    private static void evict(ServerPlayer player, String reason) {
        if (!player.isAlive() || player.isRemoved()) return;
        if (!isInUltraSpace(player)) return;

        MinecraftServer server = player.getServer();
        ServerLevel overworld = server.overworld();

        BlockPos savedPos = ReturnPointStore.get(server).remove(player.getUUID());
        BlockPos destination = savedPos != null ? savedPos : overworld.getSharedSpawnPos();

        player.displayClientMessage(
                Component.translatable("message.ultrabeasts.evicted"), false);

        player.teleportTo(overworld,
                destination.getX() + 0.5, destination.getY(), destination.getZ() + 0.5, 0, 0);

        // The Ultra-Space blanket invulnerability does not follow them out, and the saved
        // position may have been built over while they were away.
        TeleportEffects.applyLandingProtection(player);

        UltraBeasts.LOGGER.info("Evicted {} from Ultra-Space: {}.",
                player.getGameProfile().getName(), reason);
    }
}
