package dev.darcosse.ultrabeasts.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;

public class SparkParticle extends TextureSheetParticle {

    private final float angularVelocity;

    public SparkParticle(ClientLevel level, double x, double y, double z, SpriteSet spriteSet) {
        super(level, x, y, z, 0, 0, 0);

        this.lifetime = 40;
        this.quadSize = 0.5f;
        this.pickSprite(spriteSet);

        this.roll = (float) (level.random.nextDouble() * Math.PI * 2);
        this.oRoll = this.roll;
        this.angularVelocity = (float) (level.random.nextDouble() - 0.5) * 0.1f;
    }

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
