package dev.darcosse.ultrabeasts.client.particle;

import dev.darcosse.ultrabeasts.entity.ReturnWormholeEntity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SpriteSet;

/** Swirl particle of the exit portal in Ultra-Space. Client side only. */
public class ReturnWormholeParticle extends AbstractWormholeParticle {

    /** Z offset used only when the particle has no explicit target; same as the entity's. */
    private static final double FALLBACK_Z_OFFSET = ReturnWormholeEntity.PARTICLE_DESTINATION_Z_OFFSET;

    /** Called by the particle provider; see {@link AbstractWormholeParticle} for the arguments. */
    public ReturnWormholeParticle(ClientLevel level, double x, double y, double z,
                                  double vX, double vY, double vZ, SpriteSet sprite) {
        super(level, x, y, z, vX, vY, vZ, sprite, FALLBACK_Z_OFFSET);
    }
}
