package dev.darcosse.ultrabeasts.registry;

import net.minecraft.core.particles.SimpleParticleType;

/**
 * SimpleParticleType's constructor is protected in vanilla, and Fabric/NeoForge
 * each expose it differently. Subclassing it once here gives both loaders a
 * constructor they can call directly.
 */
public class ModParticleType extends SimpleParticleType {

    /** @param overrideLimiter true to ignore the player's particle quality setting */
    public ModParticleType(boolean overrideLimiter) {
        super(overrideLimiter);
    }
}
