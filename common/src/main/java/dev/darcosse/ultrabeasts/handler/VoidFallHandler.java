package dev.darcosse.ultrabeasts.handler;

import dev.darcosse.ultrabeasts.dimension.ReturnPointStore;
import dev.darcosse.ultrabeasts.dimension.UltraSpaceStructureManager;
import dev.darcosse.ultrabeasts.registry.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Sends a player back to the overworld once they fall past the bottom of
 * Ultra-Space.
 *
 * Loader-independent: each platform module wires onServerTick() every tick and
 * onPlayerDisconnect() on logout.
 */
public class VoidFallHandler {

    private static final double VOID_LIMIT_Y = -20;
    private static final double VOID_RESET_Y = -10;

    /** Debounce, so the teleport does not fire on every tick of the fall. */
    private static final Set<UUID> processedPlayers = new HashSet<>();

    public static void onServerTick(MinecraftServer server) {
        // Almost always empty, and this runs 20 times a second: skip the whole
        // player scan when nobody can possibly be falling.
        ServerLevel ultraSpace = server.getLevel(ModDimensions.ULTRA_SPACE_DIMENSION);
        if (ultraSpace == null || ultraSpace.players().isEmpty()) {
            if (!processedPlayers.isEmpty()) processedPlayers.clear();
            return;
        }

        // Collect first, act afterwards.
        //
        // Teleporting removes the player from ultraSpace.players(), and the
        // teleport is NOT deferred: MinecraftServer.execute() runs the task
        // inline when it is already on the server thread. Acting inside the
        // loop therefore mutates the list being iterated.
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

    public static void onPlayerDisconnect(ServerPlayer player) {
        processedPlayers.remove(player.getUUID());
    }

    private static void onPlayerReachVoidLimit(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();

        level.playSound(null, player.blockPosition(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);

        level.sendParticles(ParticleTypes.PORTAL,
                player.getX(), player.getY(), player.getZ(),
                50, 1, 1, 1, 0.1);

        handleUltraBeastVoidFall(player);
    }

    /**
     * Also called directly by ReturnWormholeEntity when the player walks into
     * the exit portal.
     */
    public static void handleUltraBeastVoidFall(ServerPlayer player) {
        UltraSpaceStructureManager.killAllPokemonOfWorld((ServerLevel) player.level());
        teleportBackToOverworld(player);
    }

    private static void teleportBackToOverworld(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        ServerLevel overworld = server.overworld();

        BlockPos savedPos = ReturnPointStore.get(server).remove(player.getUUID());

        UltraSpaceStructureManager.removeStructure((ServerLevel) player.level());

        // Free the dimension for the next player.
        UltraSpaceSession.release(server);

        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 254, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 254, false, false, true));
        player.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.5f, 1.0f);

        BlockPos destination = savedPos != null ? savedPos : overworld.getSharedSpawnPos();

        player.teleportTo(overworld,
                destination.getX() + 0.5, destination.getY(), destination.getZ() + 0.5, 0, 0);

        processedPlayers.remove(player.getUUID());

        if (savedPos == null) return;

        overworld.playSound(null, destination, SoundEvents.ENDERMAN_SCREAM,
                SoundSource.HOSTILE, 2.0f, 1.0f);

        overworld.sendParticles(ParticleTypes.PORTAL,
                player.getX(), player.getY() + 1, player.getZ(),
                30, 0.5, 1, 0.5, 0.1);
    }
}
