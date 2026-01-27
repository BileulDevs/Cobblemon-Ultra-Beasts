package dev.darcosse.ultrabeasts.fabric.client.particle;

import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;

public class WormholeParticleFactory implements ParticleFactory<SimpleParticleType> {

    private final SpriteProvider spriteProvider;

    public WormholeParticleFactory(SpriteProvider spriteProvider) {
        this.spriteProvider = spriteProvider;
    }

    @Override
    public Particle createParticle(SimpleParticleType type, ClientWorld world, double x, double y, double z,
                                   double centerX, double centerY, double radius) {
        // Les paramètres centerX, centerY, radius sont ceux envoyés par le serveur depuis spawnParticles()
        return new WormholeParticle(world, x, y, z, centerX, centerY, radius, spriteProvider);
    }
}
