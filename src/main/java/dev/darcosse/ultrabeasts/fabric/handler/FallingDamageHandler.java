package dev.darcosse.ultrabeasts.fabric.handler;

import dev.darcosse.ultrabeasts.fabric.registry.ModDimensions;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;

public class FallingDamageHandler {
    public static void initialize() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((livingEntity, damageSource, v) -> {
            if (livingEntity instanceof ServerPlayerEntity p) {
                if (p.getWorld().getRegistryKey().equals(ModDimensions.ULTRA_SPACE_DIMENSION) && damageSource.isOf(DamageTypes.FALL)) {
                    return false;
                }
            }
            return true;
        });
    }
}