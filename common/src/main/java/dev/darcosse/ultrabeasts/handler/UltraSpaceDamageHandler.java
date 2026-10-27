package dev.darcosse.ultrabeasts.handler;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Makes players invulnerable while they are in Ultra-Space.
 * <p>
 * Blanket cancellation rather than the earlier fall-damage-only rule. Death is an exit path
 * that bypasses everything: the occupancy claim would stay held, the structure would stay
 * standing, and the return point would stay stored. Cancelling every source also covers
 * landing inside a block on arrival, starvation on a long hunt, and out-of-world damage when
 * a fall outruns the y = -20 check. Wired by each loader; server side only.
 */
public final class UltraSpaceDamageHandler {

    /** Static predicate only, never instantiated. */
    private UltraSpaceDamageHandler() {
    }

    /**
     * Decides whether incoming damage applies.
     *
     * @return true when the damage should be applied, false to cancel it
     */
    public static boolean allowDamage(LivingEntity entity, DamageSource source) {
        if (entity instanceof ServerPlayer player && UltraSpaceSession.isInUltraSpace(player)) {
            return false;
        }
        return true;
    }
}
