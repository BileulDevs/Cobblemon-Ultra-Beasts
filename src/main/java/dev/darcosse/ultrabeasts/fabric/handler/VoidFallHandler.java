package dev.darcosse.ultrabeasts.fabric.handler;

import dev.darcosse.ultrabeasts.fabric.dimension.UltraSpaceStructureManager;
import dev.darcosse.ultrabeasts.fabric.entity.WormholeEntity;
import dev.darcosse.ultrabeasts.fabric.registry.ModDimensions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static dev.darcosse.ultrabeasts.fabric.entity.WormholeEntity.savedPositions;

public class VoidFallHandler {

    private static final Set<UUID> processedPlayers = new HashSet<>();

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                checkPlayerVoidFall(player);
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            processedPlayers.remove(handler.player.getUuid());
        });
    }

    private static void checkPlayerVoidFall(ServerPlayerEntity player) {
        if (!player.getWorld().getRegistryKey().equals(ModDimensions.ULTRA_SPACE_DIMENSION)) {
            return;
        }

        double playerY = player.getY();
        UUID playerId = player.getUuid();

        if (playerY <= -20 && !processedPlayers.contains(playerId)) {
            processedPlayers.add(playerId);
            onPlayerReachVoidLimit(player);
        }

        if (playerY > -10 && processedPlayers.contains(playerId)) {
            processedPlayers.remove(playerId);
        }
    }

    private static void onPlayerReachVoidLimit(ServerPlayerEntity player) {
        player.getWorld().playSound(null, player.getBlockPos(),
                SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.0f);

        ((ServerWorld) player.getWorld()).spawnParticles(
                ParticleTypes.PORTAL,
                player.getX(), player.getY(), player.getZ(),
                50, 1, 1, 1, 0.1
        );

        handleChimeraVoidFall(player);
    }

    private static void handleChimeraVoidFall(ServerPlayerEntity player) {
        WormholeEntity.killAllPokemonsOfWorld((ServerWorld) player.getWorld());
        WormholeEntity.checkAndPlaceChimerasCore((ServerWorld) player.getWorld());
        teleportBackToOverworld(player);
    }

    private static void teleportBackToOverworld(ServerPlayerEntity player) {
        BlockPos savedPos = savedPositions.get(player.getUuid());
        ServerWorld overworldWorld = player.getServer().getOverworld();

        UltraSpaceStructureManager.removeStructure((ServerWorld) player.getWorld());

        player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 60, 254, false, false, true));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 100, 254, false, false, true));

        player.playSound(SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.5f, 1.0f);

        overworldWorld.getServer().execute(() -> {
            if (savedPos != null) {
                player.teleport(overworldWorld, savedPos.getX() + 0.5, savedPos.getY(), savedPos.getZ() + 0.5, 0, 0);

                overworldWorld.playSound(
                        null,
                        savedPos.getX(), savedPos.getY(), savedPos.getZ(),
                        SoundEvents.ENTITY_ENDERMAN_SCREAM,
                        SoundCategory.HOSTILE,
                        2.0f,
                        1.0f
                );

                overworldWorld.spawnParticles(ParticleTypes.PORTAL,
                        player.getX(), player.getY() + 1, player.getZ(),
                        30, 0.5, 1, 0.5, 0.1);

                savedPositions.remove(player.getUuid());
            } else {
                BlockPos worldSpawn = overworldWorld.getSpawnPos();
                player.teleport(overworldWorld,
                        worldSpawn.getX() + 0.5, worldSpawn.getY(), worldSpawn.getZ() + 0.5, 0, 0);
            }
        });
    }
}
