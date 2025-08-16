package dev.darcosse.chimeras.fabric;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
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
        ServerWorld voidWorld = player.getServer().getWorld(Chimeras.CHIMERAS_DIMENSION);
        if (voidWorld != null) {
            player.teleport(voidWorld, 0, 64, 0, player.getYaw(), player.getPitch());
        }
    }
}