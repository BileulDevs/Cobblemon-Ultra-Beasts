package dev.darcosse.ultrabeasts.fabric.client.particle;

import dev.darcosse.ultrabeasts.fabric.entity.ReturnWormholeEntity;
import dev.darcosse.ultrabeasts.fabric.entity.WormholeEntity;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;

public class ReturnWormholeParticle extends SpriteBillboardParticle {
    private final double startX, startY, startZ;
    private final double destX, destY, destZ;
    private final double initialDistanceX;
    private final double initialDistanceY;

    public ReturnWormholeParticle(ClientWorld world, double x, double y, double z,
                                  double vX, double vY, double vZ, SpriteProvider sprite) {
        super(world, x, y, z, 0, 0, 0);

        this.startX = x;
        this.startY = y;
        this.startZ = z + (world.random.nextDouble() * 0.5 - 0.25);

        if (Math.abs(vX) > 0.0001 || Math.abs(vY) > 0.0001) {
            this.destX = vX;
            this.destY = vY;
            this.destZ = vZ;
        } else {
            Box searchBox = new Box(x - 5, y - 5, z - 5, x + 5, y + 5, z + 5);

            Entity portal = world.getEntitiesByClass(Entity.class, searchBox,
                            e -> e instanceof WormholeEntity || e instanceof ReturnWormholeEntity)
                    .stream().findFirst().orElse(null);

            if (portal != null) {
                this.destX = portal.getX();
                this.destY = portal.getY();
                this.destZ = portal.getZ() + 3.0;
            } else {
                this.destX = x;
                this.destY = y;
                this.destZ = z + 3.0;
            }
        }

        this.red = 1.0f;
        this.green = 0.8f;
        this.blue = 0.9f;

        this.initialDistanceX = x - destX;
        this.initialDistanceY = y - destY;

        this.maxAge = 25 + world.random.nextInt(20);
        this.setSprite(sprite);
        this.gravityStrength = 0.0f;
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

        this.z = startZ + (destZ - startZ) * progress;
        double curve = Math.pow(1.0 - progress, 3);
        this.x = destX + (initialDistanceX * curve);
        this.y = destY + (initialDistanceY * curve);

        if (progress < 0.08f) {
            // 0-8% : Rose Pastel pur
            this.red = 1.0f;
            this.green = 0.8f;
            this.blue = 0.9f;
        } else if (progress < 0.15f) {
            // 8-15% : Transition Rose -> Violet Lilas (Très rapide)
            float local = (progress - 0.08f) / (0.15f - 0.08f);
            this.red = lerp(local, 1.0f, 0.8f);
            this.green = lerp(local, 0.8f, 0.7f);
            this.blue = lerp(local, 0.9f, 1.0f);
        } else if (progress < 0.22f) {
            // 15-22% : Transition Violet -> Bleu Azur (Très rapide)
            float local = (progress - 0.15f) / (0.22f - 0.15f);
            this.red = lerp(local, 0.8f, 0.6f);
            this.green = lerp(local, 0.7f, 0.85f);
            this.blue = lerp(local, 1.0f, 1.0f);
        } else {
            // 22-100% : Transition Bleu -> Blanc, puis Blanc pur
            float local = (progress - 0.22f) / (1.0f - 0.22f);

            this.red = lerp(Math.min(local * 2.0f, 1.0f), 0.6f, 1.0f);
            this.green = lerp(Math.min(local * 2.0f, 1.0f), 0.85f, 1.0f);
            this.blue = 1.0f;
        }

        this.alpha = (float) Math.sin(Math.PI * progress);
        this.scale = 0.4f * (1.1f - progress);
    }

    private float lerp(float delta, float start, float end) {
        return start + delta * (end - start);
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_OPAQUE;
    }

    @Override
    public int getBrightness(float tint) {
        return 15728880;
    }
}