package dev.darcosse.ultrabeasts.fabric.client.particle;

import dev.darcosse.ultrabeasts.fabric.entity.WormholeEntity;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Box;

public class WormholeParticle extends SpriteBillboardParticle {
    private final double startX, startY, startZ;
    private final double destX, destY, destZ;
    private final double initialDistanceX;
    private final double initialDistanceY;

    public WormholeParticle(ClientWorld world, double x, double y, double z,
                            double vX, double vY, double vZ, SpriteProvider sprite) {
        super(world, x, y, z, 0, 0, 0);

        this.startX = x;
        this.startY = y;
        this.startZ = z;

        // 1. On tente d'utiliser les data du serveur (si elles ne sont pas à 0)
        if (Math.abs(vX) > 0.0001 || Math.abs(vY) > 0.0001) {
            this.destX = vX;
            this.destY = vY;
            this.destZ = vZ;
        }
        // 2. Sinon, on cherche l'entité WormholeEntity la plus proche
        else {
            // On utilise getEntitiesByClass pour plus de flexibilité avec les types non-Living
            Box searchBox = new Box(x - 5, y - 5, z - 5, x + 5, y + 5, z + 5);
            java.util.List<WormholeEntity> entities = world.getEntitiesByClass(WormholeEntity.class, searchBox, entity -> true);

            if (!entities.isEmpty()) {
                // On prend la première trouvée (la plus proche en général)
                WormholeEntity portal = entities.get(0);
                this.destX = portal.getX();
                this.destY = portal.getY();
                this.destZ = portal.getZ() - 2.0;
            } else {
                // Secours ultime
                this.destX = x;
                this.destY = y;
                this.destZ = z - 2.0;
            }
        }

        this.initialDistanceX = x - destX;
        this.initialDistanceY = y - destY;

        this.maxAge = 35;
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

        // Progression de 0.0 à 1.0
        float progress = (float) this.age / this.maxAge;

        // 1. Recul linéaire de 2 blocs sur l'axe Z
        this.z = startZ + (destZ - startZ) * progress;

        // 2. Convergence courbe (L'effet entonnoir de ton dessin)
        // Math.pow(..., 3) rend la courbe très prononcée
        double curve = Math.pow(1.0 - progress, 3);

        this.x = destX + (initialDistanceX * curve);
        this.y = destY + (initialDistanceY * curve);

        // 3. Opacité pour la fluidité
        // Apparition en fondu au début, disparition à la fin
        this.alpha = (float) Math.sin(Math.PI * progress);
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }
}