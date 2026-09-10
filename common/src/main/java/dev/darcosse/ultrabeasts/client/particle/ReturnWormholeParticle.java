package dev.darcosse.ultrabeasts.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SpriteSet;

public class ReturnWormholeParticle extends AbstractWormholeParticle {

    public ReturnWormholeParticle(ClientLevel level, double x, double y, double z,
                                  double vX, double vY, double vZ, SpriteSet sprite) {
        super(level, x, y, z, vX, vY, vZ, sprite, 3.0);
    }
}
