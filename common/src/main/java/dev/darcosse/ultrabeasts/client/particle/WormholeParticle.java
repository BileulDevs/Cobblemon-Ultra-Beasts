package dev.darcosse.ultrabeasts.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SpriteSet;

public class WormholeParticle extends AbstractWormholeParticle {

    public WormholeParticle(ClientLevel level, double x, double y, double z,
                            double vX, double vY, double vZ, SpriteSet sprite) {
        super(level, x, y, z, vX, vY, vZ, sprite, -3.0);
    }
}
