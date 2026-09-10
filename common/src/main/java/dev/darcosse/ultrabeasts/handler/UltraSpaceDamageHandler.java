package dev.darcosse.ultrabeasts.handler;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Nobody dies in Ultra-Space.
 *
 * Blanket cancellation rather than the previous fall-damage-only rule. Death is
 * an exit path that bypasses everything: the occupancy claim would stay held,
 * the structure would stay standing, and the return point would stay stored.
 * Cancelling every source also closes cases that were never handled — landing
 * inside a block on arrival, starvation on a long hunt, or out-of-world damage
 * when a fall outruns the y = -20 check.
 */
public class UltraSpaceDamageHandler {

    /** @return true when the damage should be applied. */
    public static boolean allowDamage(LivingEntity entity, DamageSource source) {
        if (entity instanceof ServerPlayer player && UltraSpaceSession.isInUltraSpace(player)) {
            return false;
        }
        return true;
    }
}
