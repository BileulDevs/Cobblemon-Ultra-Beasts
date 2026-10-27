package dev.darcosse.ultrabeasts.registry;

import net.minecraft.core.particles.SimpleParticleType;

/**
 * Particle type definitions shared by both loaders. Each loader registers them under the ids
 * below and publishes the registered types into the static fields. Client and server must
 * agree on these ids, or particles fail to resolve on the client.
 */
public final class ModParticles {

    public static final String WORMHOLE_ID = "wormhole";
    public static final String RETURN_WORMHOLE_ID = "return_wormhole";
    public static final String SPARK_ID = "spark";

    /** Filled by the loader module during registration; null before that. */
    public static SimpleParticleType WORMHOLE;
    public static SimpleParticleType RETURN_WORMHOLE;
    public static SimpleParticleType SPARK;

    /** Definitions only, never instantiated. */
    private ModParticles() {
    }

    /**
     * Creates one particle type. overrideLimiter is false: these particles honour the
     * player's "Particles: Decreased / Minimal" setting.
     */
    public static SimpleParticleType create() {
        return new ModParticleType(false);
    }
}
