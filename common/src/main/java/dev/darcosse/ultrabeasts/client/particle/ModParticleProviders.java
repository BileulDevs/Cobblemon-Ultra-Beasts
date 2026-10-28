package dev.darcosse.ultrabeasts.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * The three particle providers, grouped so both loaders register the same classes. Each one
 * just forwards the spawn arguments to its particle. Client side only.
 */
public final class ModParticleProviders {

    /** Providers only, never instantiated. */
    private ModParticleProviders() {
    }

    /** Creates {@link WormholeParticle}s; the velocity carries the target point. */
    public record Wormhole(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double velX, double velY, double velZ) {
            return new WormholeParticle(level, x, y, z, velX, velY, velZ, sprites);
        }
    }

    /** Creates {@link ReturnWormholeParticle}s; the velocity carries the target point. */
    public record ReturnWormhole(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double velX, double velY, double velZ) {
            return new ReturnWormholeParticle(level, x, y, z, velX, velY, velZ, sprites);
        }
    }

    /** Creates {@link SparkParticle}s; the velocity is ignored. */
    public record Spark(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double velX, double velY, double velZ) {
            return new SparkParticle(level, x, y, z, sprites);
        }
    }
}
