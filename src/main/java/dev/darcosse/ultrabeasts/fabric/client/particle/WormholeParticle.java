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

        // Bleu ciel doux → Blanc lumineux avec touche de lavande
        this.red = 0.7f + (progress * 0.3f);    // 0.7 → 1.0 (presque blanc)
        this.green = 0.8f + (progress * 0.2f);  // 0.8 → 1.0 (presque blanc)
        this.blue = 1.0f;                        // 1.0 constant (toujours lumineux)
        this.alpha = 0.5f + (progress * 0.4f);  // 0.5 → 0.9 (délicat)
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }
}