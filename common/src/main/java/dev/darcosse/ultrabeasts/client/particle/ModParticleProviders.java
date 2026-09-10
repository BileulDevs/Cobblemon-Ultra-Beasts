package dev.darcosse.ultrabeasts.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * The three particle providers, grouped so both loaders reference the same class.
 */
public final class ModParticleProviders {

    private ModParticleProviders() {
    }

    public record Wormhole(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double velX, double velY, double velZ) {
            return new WormholeParticle(level, x, y, z, velX, velY, velZ, sprites);
        }
    }

    public record ReturnWormhole(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double velX, double velY, double velZ) {
            return new ReturnWormholeParticle(level, x, y, z, velX, velY, velZ, sprites);
        }
    }

    public record Spark(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double velX, double velY, double velZ) {
            return new SparkParticle(level, x, y, z, sprites);
        }
    }
}
