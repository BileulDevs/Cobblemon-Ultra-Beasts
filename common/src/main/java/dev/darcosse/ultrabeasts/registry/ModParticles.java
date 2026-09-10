package dev.darcosse.ultrabeasts.registry;

import net.minecraft.core.particles.SimpleParticleType;

/**
 * Particle type definitions. Registration is done by each loader.
 */
public final class ModParticles {

    public static final String WORMHOLE_ID = "wormhole";
    public static final String RETURN_WORMHOLE_ID = "return_wormhole";
    public static final String SPARK_ID = "spark";

    /** Filled by the platform module during registration. */
    public static SimpleParticleType WORMHOLE;
    public static SimpleParticleType RETURN_WORMHOLE;
    public static SimpleParticleType SPARK;

    private ModParticles() {
    }

    public static SimpleParticleType create() {
        return new ModParticleType(false);
    }
}
