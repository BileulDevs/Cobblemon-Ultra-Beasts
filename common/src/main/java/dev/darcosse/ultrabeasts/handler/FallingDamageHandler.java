package dev.darcosse.ultrabeasts.handler;

import dev.darcosse.ultrabeasts.registry.ModDimensions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;

/**
 * Loader-independent logic. Each platform module wires allowDamage().
 */
public class FallingDamageHandler {

    /** @return true when the damage should be applied. */
    public static boolean allowDamage(LivingEntity entity, DamageSource source) {
        if (entity instanceof ServerPlayer player) {
            if (player.level().dimension().equals(ModDimensions.ULTRA_SPACE_DIMENSION)
                    && source.is(DamageTypes.FALL)) {
                return false;
            }
        }
        return true;
    }
}
