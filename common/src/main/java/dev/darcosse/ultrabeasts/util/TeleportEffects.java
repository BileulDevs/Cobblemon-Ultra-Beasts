package dev.darcosse.ultrabeasts.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Status effects applied around every trip into or out of Ultra-Space, kept in one place so
 * the wormhole, the void fall and an eviction all feel the same.
 * Server side only.
 */
public final class TeleportEffects {

    /**
     * Blindness length in ticks (3 s). Hides the chunk pop-in while the destination loads;
     * shorter and players see the world assemble, longer and it feels like a bug.
     */
    private static final int BLINDNESS_TICKS = 60;

    /**
     * Resistance length in ticks (5 s) after landing in the overworld. The saved return point
     * may have been built over or turned into a drop meanwhile; 5 s is time to step away.
     */
    private static final int LANDING_PROTECTION_TICKS = 100;

    /**
     * Amplifier for both effects. Resistance V (amplifier 4) already cancels all damage;
     * 254 is kept from the original design and changes nothing in practice.
     */
    private static final int AMPLIFIER = 254;

    /** Static utility, never instantiated. */
    private TeleportEffects() {
    }

    /** Blinds the player for the duration of a dimension change. */
    public static void applyTransitionBlindness(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BLINDNESS_TICKS, AMPLIFIER,
                false, false, true));
    }

    /**
     * Makes the player briefly immune to damage after leaving Ultra-Space, where they were
     * protected by {@link dev.darcosse.ultrabeasts.handler.UltraSpaceDamageHandler}.
     */
    public static void applyLandingProtection(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, LANDING_PROTECTION_TICKS, AMPLIFIER,
                false, false, true));
    }
}
