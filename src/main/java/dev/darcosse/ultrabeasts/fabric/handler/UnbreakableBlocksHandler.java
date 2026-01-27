package dev.darcosse.ultrabeasts.fabric.handler;

import dev.darcosse.ultrabeasts.fabric.registry.ModDimensions;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;

public class UnbreakableBlocksHandler {
    public static void initialize() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (world.getRegistryKey().equals(ModDimensions.ULTRA_SPACE_DIMENSION)) {
                return false;
            }
            return true;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.getRegistryKey().equals(ModDimensions.ULTRA_SPACE_DIMENSION)) {
                ItemStack itemStack = player.getStackInHand(hand);

                if (!(itemStack.getItem() instanceof BlockItem)) {
                    return ActionResult.PASS;
                }

                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });
    }
}