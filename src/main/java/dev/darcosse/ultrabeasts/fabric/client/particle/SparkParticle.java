package dev.darcosse.ultrabeasts.fabric.client.particle;

import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;

public class SparkParticle extends SpriteBillboardParticle {
    private final float angularVelocity;

    public SparkParticle(ClientWorld world, double x, double y, double z,
                         SpriteProvider spriteProvider) {
        super(world, x, y, z, 0, 0, 0);

        this.maxAge = 40;
        this.scale = 0.5f;
        this.setSprite(spriteProvider);

        this.angle = (float) (world.random.nextDouble() * Math.PI * 2);
        this.prevAngle = this.angle;
        this.angularVelocity = (float) (world.random.nextDouble() - 0.5) * 0.1f;
    }

    @Override
    public void tick() {
        this.prevAngle = this.angle;
        super.tick();

        this.angle += this.angularVelocity;
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }
}