package dev.darcosse.ultrabeasts.client.particle;

import dev.darcosse.ultrabeasts.entity.ReturnWormholeEntity;
import dev.darcosse.ultrabeasts.entity.WormholeEntity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

/**
 * Shared behaviour for the inbound and outbound wormhole particles.
 * The only difference between the two is the Z offset of the target point.
 */
public abstract class AbstractWormholeParticle extends TextureSheetParticle {

    private final double startX, startY, startZ;
    private final double destX, destY, destZ;
    private final double initialDistanceX;
    private final double initialDistanceY;

    protected AbstractWormholeParticle(ClientLevel level, double x, double y, double z,
                                       double vX, double vY, double vZ, SpriteSet sprite,
                                       double zOffset) {
        super(level, x, y, z, 0, 0, 0);

        this.startX = x;
        this.startY = y;
        this.startZ = z + (level.random.nextDouble() * 0.5 - 0.25);

        if (Math.abs(vX) > 0.0001 || Math.abs(vY) > 0.0001) {
            this.destX = vX;
            this.destY = vY;
            this.destZ = vZ;
        } else {
            AABB searchBox = new AABB(x - 5, y - 5, z - 5, x + 5, y + 5, z + 5);

            Entity portal = level.getEntitiesOfClass(Entity.class, searchBox,
                            e -> e instanceof WormholeEntity || e instanceof ReturnWormholeEntity)
                    .stream().findFirst().orElse(null);

            if (portal != null) {
                this.destX = portal.getX();
                this.destY = portal.getY();
                this.destZ = portal.getZ() + zOffset;
            } else {
                this.destX = x;
                this.destY = y;
                this.destZ = z + zOffset;
            }
        }

        this.rCol = 1.0f;
        this.gCol = 0.8f;
        this.bCol = 0.9f;

        this.initialDistanceX = x - destX;
        this.initialDistanceY = y - destY;

        this.lifetime = 25 + level.random.nextInt(20);
        this.pickSprite(sprite);
        this.gravity = 0.0f;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        float progress = (float) this.age / this.lifetime;

        this.z = startZ + (destZ - startZ) * progress;
        double curve = Math.pow(1.0 - progress, 3);
        this.x = destX + (initialDistanceX * curve);
        this.y = destY + (initialDistanceY * curve);

        if (progress < 0.08f) {
            // 0-8%: pure pastel pink
            this.rCol = 1.0f;
            this.gCol = 0.8f;
            this.bCol = 0.9f;
        } else if (progress < 0.15f) {
            // 8-15%: pink -> lilac
            float local = (progress - 0.08f) / (0.15f - 0.08f);
            this.rCol = lerp(local, 1.0f, 0.8f);
            this.gCol = lerp(local, 0.8f, 0.7f);
            this.bCol = lerp(local, 0.9f, 1.0f);
        } else if (progress < 0.22f) {
            // 15-22%: lilac -> azure
            float local = (progress - 0.15f) / (0.22f - 0.15f);
            this.rCol = lerp(local, 0.8f, 0.6f);
            this.gCol = lerp(local, 0.7f, 0.85f);
            this.bCol = lerp(local, 1.0f, 1.0f);
        } else {
            // 22-100%: azure -> white, then pure white
            float local = (progress - 0.22f) / (1.0f - 0.22f);

            this.rCol = lerp(Math.min(local * 2.0f, 1.0f), 0.6f, 1.0f);
            this.gCol = lerp(Math.min(local * 2.0f, 1.0f), 0.85f, 1.0f);
            this.bCol = 1.0f;
        }

        this.alpha = (float) Math.sin(Math.PI * progress);
        this.quadSize = 0.4f * (1.1f - progress);
    }

    private float lerp(float delta, float start, float end) {
        return start + delta * (end - start);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    @Override
    public int getLightColor(float partialTick) {
        return 15728880;
    }
}
