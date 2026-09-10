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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.UUID;

/**
 * Owns the "one player at a time" rule.
 *
 * Every path in and out of Ultra-Space goes through here, so the rule is one
 * readable place instead of a property that emerges from scattered checks.
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

    private UltraSpaceSession() {
    }

    private static long gameTime(MinecraftServer server) {
        // One clock for every check: the overworld's.
        return server.overworld().getGameTime();
    }

    private static UltraSpaceState state(MinecraftServer server) {
        ServerLevel ultraSpace = server.getLevel(ModDimensions.ULTRA_SPACE_DIMENSION);
        return ultraSpace == null ? null : UltraSpaceState.getServerState(ultraSpace);
    }

    public static boolean isInUltraSpace(ServerPlayer player) {
        return player.level().dimension().equals(ModDimensions.ULTRA_SPACE_DIMENSION);
    }

    /**
     * Can this player enter right now?
     *
     * True when the dimension is free, when the lease has expired, or when the
     * player already holds it.
     */
    public static boolean canEnter(ServerPlayer player) {
        UltraSpaceState state = state(player.getServer());
        if (state == null) return false;

        long now = gameTime(player.getServer());
        UUID occupant = state.getOccupant(now);

        return occupant == null || occupant.equals(player.getUUID());
    }

    /** Is anyone holding the dimension? Used to hold back wormhole spawns. */
    public static boolean isOccupied(MinecraftServer server) {
        UltraSpaceState state = state(server);
        return state != null && state.isClaimed(gameTime(server));
    }

    /** Called once the player is actually being sent in. */
    public static void claim(ServerPlayer player) {
        UltraSpaceState state = state(player.getServer());
        if (state != null) {
            state.claim(player.getUUID());
        }
    }

    /** Called on every way out: portal, void, or eviction. */
    public static void release(MinecraftServer server) {
        UltraSpaceState state = state(server);
        if (state != null) {
            state.clearClaim();
        }
    }

    /**
     * The occupant logged out: start the countdown rather than freeing the slot
     * immediately, so a crash does not cost them their wormhole.
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
     * Either resumes the session, or throws the player out if the dimension
     * changed hands while they were away.
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

        // Someone else holds it, or the lease lapsed and the structure was
        // replaced. Either way this player no longer belongs here.
        // Deferred: teleporting while the client is still loading the world
        // gives unreliable results.
        ServerScheduler.schedule(EVICTION_DELAY, () -> evict(player));
    }

    private static void evict(ServerPlayer player) {
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

        // The Ultra-Space blanket invulnerability does not follow them out, and
        // the saved position may have been built over while they were away.
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 254, false, false, true));

        UltraBeasts.LOGGER.info("Evicted {} from Ultra-Space: the claim had changed hands.",
                player.getGameProfile().getName());
    }
}
