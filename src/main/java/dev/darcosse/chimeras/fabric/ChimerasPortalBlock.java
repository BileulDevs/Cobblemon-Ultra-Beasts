package dev.darcosse.chimeras.fabric;

import com.cobblemon.mod.common.CobblemonEntities;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.registry.RegistryKey;

public class ChimerasPortalBlock extends Block {
    public ChimerasPortalBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player,
                                 BlockHitResult hit) {
        if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) {
            teleportToChimerasDimension(serverPlayer);
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    @Override
    public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
        if (!world.isClient && entity instanceof ServerPlayerEntity serverPlayer) {
            teleportToChimerasDimension(serverPlayer);
        }
    }

    private void teleportToChimerasDimension(ServerPlayerEntity player) {
        ServerWorld chimerasWorld = player.getServer().getWorld(Chimeras.CHIMERAS_DIMENSION);
        if (chimerasWorld != null) {
            checkAndPlaceChimerasCore(chimerasWorld);
            player.teleport(chimerasWorld, -0.5, 83, -22.5, player.getYaw(), player.getPitch());
            killAllPokemonsOfWorld(chimerasWorld);
        }
    }

    private void checkAndPlaceChimerasCore(ServerWorld world) {
        BlockPos centerPos = new BlockPos(0, 84, 0);

        boolean coreExists = false;
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos checkPos = centerPos.add(x, 0, z);
                if (world.getBlockState(checkPos).isOf(Chimeras.CHIMERAS_CORE_BLOCK)) {
                    coreExists = true;
                    break;
                }
            }
            if (coreExists) break;
        }

        if (!coreExists) {
            world.setBlockState(centerPos, Chimeras.CHIMERAS_CORE_BLOCK.getDefaultState());
        }
    }

    private void killAllPokemonsOfWorld(ServerWorld chimerasWorld) {
        if (chimerasWorld != null) {
            chimerasWorld.getEntitiesByType(CobblemonEntities.POKEMON, pokemonEntity -> {
                pokemonEntity.discard();
                return false;
            });
        }
    }
}