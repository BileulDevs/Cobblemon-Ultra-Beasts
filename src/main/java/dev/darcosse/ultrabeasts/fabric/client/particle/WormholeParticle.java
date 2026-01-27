package dev.darcosse.ultrabeasts.fabric.client.particle;

import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;

public class WormholeParticle extends SpriteBillboardParticle {

    private final double centerX;
    private final double centerZ;

    protected WormholeParticle(ClientWorld world, double x, double y, double z,
                               double centerX, double centerY, double radius,
                               SpriteProvider spriteProvider) {
        super(world, x, y, z, 0, 0, 0);
        this.centerX = centerX;
        this.centerZ = centerY; // à corriger si nécessaire
        this.scale = 0.3f;
        this.maxAge = 40;
        this.setSprite(spriteProvider.getSprite(0, 0));
    }

    @Override
    public void tick() {
        super.tick();

        // Convergence vers le centre
        double dx = centerX - this.x;
        double dz = centerZ - this.z;
        this.velocityX += dx * 0.05;
        this.velocityZ += dz * 0.05;

        // Spirale légère
        this.velocityX += Math.sin(age * 0.3) * 0.01;
        this.velocityZ += Math.cos(age * 0.3) * 0.01;

        this.move(this.velocityX, this.velocityY, this.velocityZ);

        if (this.age++ >= this.maxAge) this.markDead();
    }

    @Override
    public ParticleTextureSheet getType() {
        return null;
    }
}
