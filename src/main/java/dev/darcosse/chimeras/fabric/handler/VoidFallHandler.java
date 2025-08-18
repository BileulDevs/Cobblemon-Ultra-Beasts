package dev.darcosse.chimeras.fabric.handler;

import dev.darcosse.chimeras.fabric.ChimerasPortalBlock;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static dev.darcosse.chimeras.fabric.ChimerasPortalBlock.savedPositions;

public class VoidFallHandler {

    public static final RegistryKey<World> CHIMERAS_DIMENSION =
            RegistryKey.of(RegistryKeys.WORLD, Identifier.of("cobblemon_chimeras", "chimeras_dimension"));

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
        if (!player.getWorld().getRegistryKey().equals(CHIMERAS_DIMENSION)) {
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
        ChimerasPortalBlock.killAllPokemonsOfWorld((ServerWorld) player.getWorld());
        ChimerasPortalBlock.checkAndPlaceChimerasCore((ServerWorld) player.getWorld());
        teleportBackToOverworld(player);
    }

    private static void teleportBackToOverworld(ServerPlayerEntity player) {
        BlockPos savedPos = savedPositions.get(player.getUuid());

        if (savedPos != null) {
            ServerWorld overworldWorld = player.getServer().getOverworld();
            player.teleport(overworldWorld, savedPos.getX() + 0.5, savedPos.getY(), savedPos.getZ() + 0.5, 0, 0);

            overworldWorld.spawnParticles(ParticleTypes.PORTAL,
                    player.getX(), player.getY() + 1, player.getZ(),
                    30, 0.5, 1, 0.5, 0.1);

            savedPositions.remove(player.getUuid());

        } else {
            ServerWorld overworldWorld = player.getServer().getOverworld();
            BlockPos worldSpawn = overworldWorld.getSpawnPos();
            player.teleport(overworldWorld,
                    worldSpawn.getX() + 0.5, worldSpawn.getY(), worldSpawn.getZ() + 0.5, 0, 0);
        }
    }
}
