package dev.darcosse.ultrabeasts.fabric.client.particle;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;

public class ReturnWormholeParticleFactory implements ParticleFactory<SimpleParticleType> {

    private final SpriteProvider spriteProvider;

    public ReturnWormholeParticleFactory(SpriteProvider spriteProvider) {
        this.spriteProvider = spriteProvider;
    }

    @Override
    public Particle createParticle(SimpleParticleType type, ClientWorld world,
                                   double x, double y, double z,
                                   double velX, double velY, double velZ) {
        return new ReturnWormholeParticle(world, x, y, z, velX, velY, velZ, this.spriteProvider);
    }
}