package dev.darcosse.ultrabeasts.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;

/**
 * A still, slowly spinning spark. Registered for resource packs and commands; the mod's own
 * code does not spawn it. Client side only.
 */
public class SparkParticle extends TextureSheetParticle {

    /** Lifetime in ticks (2 s). */
    private static final int LIFETIME = 40;

    /** Quad size, in blocks. */
    private static final float QUAD_SIZE = 0.5f;

    /** Maximum spin per tick, in radians (±0.05). */
    private static final float MAX_SPIN = 0.1f;

    /** Spin per tick, in radians, fixed at birth. */
    private final float angularVelocity;

    /** Spawns a spark with a random orientation and spin direction. */
    public SparkParticle(ClientLevel level, double x, double y, double z, SpriteSet spriteSet) {
        super(level, x, y, z, 0, 0, 0);

        this.lifetime = LIFETIME;
        this.quadSize = QUAD_SIZE;
        this.pickSprite(spriteSet);

        this.roll = (float) (level.random.nextDouble() * Math.PI * 2);
        this.oRoll = this.roll;
        this.angularVelocity = (float) (level.random.nextDouble() - 0.5) * MAX_SPIN;
    }

    /** Spins the spark; oRoll is saved first so the renderer interpolates the rotation. */
    @Override
    public void tick() {
        this.oRoll = this.roll;
        super.tick();

        this.roll += this.angularVelocity;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
}
