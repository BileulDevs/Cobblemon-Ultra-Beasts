package dev.darcosse.ultrabeasts.fabric.client.particle;

import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;

public class WormholeParticle extends SpriteBillboardParticle {

    private final double centerX;
    private final double centerY;
    private final double centerZ;
    private final double initialDistance;

    public WormholeParticle(ClientWorld world, double x, double y, double z,
                               double velX, double velY, double velZ,
                               SpriteProvider spriteProvider) {
        super(world, x, y, z, 0, 0, 0);

        this.centerX = velX;
        this.centerY = velY;
        this.centerZ = velZ;

        double dx = x - centerX;
        double dy = y - centerY;
        double dz = z - centerZ;
        this.initialDistance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        this.maxAge = 40;
        this.scale = 0.5f;
        this.setSprite(spriteProvider);
    }

    @Override
    public void tick() {
        this.prevPosX = this.x;
        this.prevPosY = this.y;
        this.prevPosZ = this.z;

        if (this.age++ >= this.maxAge) {
            this.markDead();
            return;
        }

        float progress = (float) this.age / this.maxAge;
        progress = Math.max(0, Math.min(1, progress));

        this.red = 0.4f + (progress * 0.6f);
        this.green = 0.9f + (progress * 0.1f);
        this.blue = 1.0f;
        this.alpha = 0.6f + (progress * 0.4f);
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }
}