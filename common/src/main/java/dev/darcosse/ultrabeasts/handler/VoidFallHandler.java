package dev.darcosse.ultrabeasts.handler;

import dev.darcosse.ultrabeasts.dimension.UltraSpaceStructureManager;
import dev.darcosse.ultrabeasts.registry.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static dev.darcosse.ultrabeasts.entity.WormholeEntity.savedPositions;

/**
 * Loader-independent logic. Each platform module wires the two hooks:
 * onServerTick() every tick, and onPlayerDisconnect() on logout.
 */
public class VoidFallHandler {

    private static final Set<UUID> processedPlayers = new HashSet<>();

    public static void onServerTick(net.minecraft.server.MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            checkPlayerVoidFall(player);
        }
    }

    public static void onPlayerDisconnect(ServerPlayer player) {
        processedPlayers.remove(player.getUUID());
    }

    private static void checkPlayerVoidFall(ServerPlayer player) {
        if (!player.level().dimension().equals(ModDimensions.ULTRA_SPACE_DIMENSION)) {
            return;
        }

        double playerY = player.getY();
        UUID playerId = player.getUUID();

        if (playerY <= -20 && !processedPlayers.contains(playerId)) {
            processedPlayers.add(playerId);
            onPlayerReachVoidLimit(player);
        }

        if (playerY > -10 && processedPlayers.contains(playerId)) {
            processedPlayers.remove(playerId);
        }
    }

    private static void onPlayerReachVoidLimit(ServerPlayer player) {
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);

        ((ServerLevel) player.level()).sendParticles(
                ParticleTypes.PORTAL,
                player.getX(), player.getY(), player.getZ(),
                50, 1, 1, 1, 0.1
        );

        handleUltraBeastVoidFall(player);
    }

    public static void handleUltraBeastVoidFall(ServerPlayer player) {
        UltraSpaceStructureManager.killAllPokemonOfWorld((ServerLevel) player.level());
        teleportBackToOverworld(player);
    }

    private static void teleportBackToOverworld(ServerPlayer player) {
        BlockPos savedPos = savedPositions.get(player.getUUID());
        ServerLevel overworld = player.getServer().overworld();

        UltraSpaceStructureManager.removeStructure((ServerLevel) player.level());

        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 254, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 254, false, false, true));

        player.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.5f, 1.0f);

        overworld.getServer().execute(() -> {
            if (savedPos != null) {
                player.teleportTo(overworld, savedPos.getX() + 0.5, savedPos.getY(), savedPos.getZ() + 0.5, 0, 0);

                overworld.playSound(
                        null,
                        savedPos.getX(), savedPos.getY(), savedPos.getZ(),
                        SoundEvents.ENDERMAN_SCREAM,
                        SoundSource.HOSTILE,
                        2.0f,
                        1.0f
                );

                overworld.sendParticles(ParticleTypes.PORTAL,
                        player.getX(), player.getY() + 1, player.getZ(),
                        30, 0.5, 1, 0.5, 0.1);

                savedPositions.remove(player.getUUID());
            } else {
                BlockPos worldSpawn = overworld.getSharedSpawnPos();
                player.teleportTo(overworld,
                        worldSpawn.getX() + 0.5, worldSpawn.getY(), worldSpawn.getZ() + 0.5, 0, 0);
            }
        });
    }
}
