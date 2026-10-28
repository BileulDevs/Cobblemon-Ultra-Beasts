package dev.darcosse.ultrabeasts.handler;

import dev.darcosse.ultrabeasts.dimension.ReturnPointStore;
import dev.darcosse.ultrabeasts.dimension.UltraSpaceStructureManager;
import dev.darcosse.ultrabeasts.registry.ModDimensions;
import dev.darcosse.ultrabeasts.util.TeleportEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Sends a player back to the overworld once they fall past the bottom of Ultra-Space, and
 * owns the shared "leave Ultra-Space" routine also used by the exit portal.
 * <p>
 * Loader-independent: each loader module calls {@link #onServerTick} every tick and
 * {@link #onPlayerDisconnect} on logout. Server side only.
 */
public final class VoidFallHandler {

    /**
     * Height that triggers the return. Must stay above the dimension's min_y (-64) plus the
     * vanilla out-of-world margin, or the void would be reached first.
     */
    private static final double VOID_LIMIT_Y = -20;

    /**
     * Height a player must climb back above before they can trigger again. The gap with
     * {@link #VOID_LIMIT_Y} is the debounce window.
     */
    private static final double VOID_RESET_Y = -10;

    /** Portal particles burst where the player vanishes. */
    private static final int DEPARTURE_PARTICLES = 50;

    /** Portal particles burst where the player reappears. */
    private static final int ARRIVAL_PARTICLES = 30;

    /** Players already being sent home, so the teleport does not fire on every tick of the fall. */
    private static final Set<UUID> processedPlayers = new HashSet<>();

    /** Static handlers only, never instantiated. */
    private VoidFallHandler() {
    }

    /** Per-tick check for players below {@link #VOID_LIMIT_Y}. */
    public static void onServerTick(MinecraftServer server) {
        // Almost always empty, and this runs 20 times a second: skip the whole player scan
        // when nobody can possibly be falling.
        ServerLevel ultraSpace = server.getLevel(ModDimensions.ULTRA_SPACE_DIMENSION);
        if (ultraSpace == null || ultraSpace.players().isEmpty()) {
            if (!processedPlayers.isEmpty()) processedPlayers.clear();
            return;
        }

        // Collect first, act afterwards. Teleporting removes the player from
        // ultraSpace.players(), and the teleport is NOT deferred: MinecraftServer.execute()
        // runs the task inline when already on the server thread. Acting inside the loop
        // would mutate the list being iterated.
        List<ServerPlayer> falling = null;

        for (ServerPlayer player : ultraSpace.players()) {
            UUID playerId = player.getUUID();
            double y = player.getY();

            if (y <= VOID_LIMIT_Y) {
                if (processedPlayers.add(playerId)) {
                    if (falling == null) falling = new ArrayList<>(1);
                    falling.add(player);
                }
            } else if (y > VOID_RESET_Y) {
                processedPlayers.remove(playerId);
            }
        }

        if (falling == null) return;

        for (ServerPlayer player : falling) {
            onPlayerReachVoidLimit(player);
        }
    }

    /** Forgets the debounce entry of a player who logged out. */
    public static void onPlayerDisconnect(ServerPlayer player) {
        processedPlayers.remove(player.getUUID());
    }

    /** Plays the departure effects, then runs the shared exit routine. */
    private static void onPlayerReachVoidLimit(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();

        level.playSound(null, player.blockPosition(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);

        level.sendParticles(ParticleTypes.PORTAL,
                player.getX(), player.getY(), player.getZ(),
                DEPARTURE_PARTICLES, 1, 1, 1, 0.1);

        handleUltraBeastVoidFall(player);
    }

    /**
     * Ends the player's Ultra-Space session: removes the Ultra Beast and the structure, frees
     * the dimension, and sends the player home. Also called by the exit portal.
     */
    public static void handleUltraBeastVoidFall(ServerPlayer player) {
        UltraSpaceStructureManager.killAllPokemonOfWorld((ServerLevel) player.level());
        teleportBackToOverworld(player);
    }

    /** Teleports the player to their saved return point, or to world spawn without one. */
    private static void teleportBackToOverworld(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        ServerLevel overworld = server.overworld();

        BlockPos savedPos = ReturnPointStore.get(server).remove(player.getUUID());

        UltraSpaceStructureManager.removeStructure((ServerLevel) player.level());

        // Free the dimension for the next player.
        UltraSpaceSession.release(server);

        TeleportEffects.applyTransitionBlindness(player);
        TeleportEffects.applyLandingProtection(player);
        player.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.5f, 1.0f);

        BlockPos destination = savedPos != null ? savedPos : overworld.getSharedSpawnPos();

        player.teleportTo(overworld,
                destination.getX() + 0.5, destination.getY(), destination.getZ() + 0.5, 0, 0);

        processedPlayers.remove(player.getUUID());

        // Arrival effects are reserved for a real return point, never world spawn (unchanged
        // from 4.0).
        if (savedPos == null) return;

        overworld.playSound(null, destination, SoundEvents.ENDERMAN_SCREAM,
                SoundSource.HOSTILE, 2.0f, 1.0f);

        overworld.sendParticles(ParticleTypes.PORTAL,
                player.getX(), player.getY() + 1, player.getZ(),
                ARRIVAL_PARTICLES, 0.5, 1, 0.5, 0.1);
    }
}
