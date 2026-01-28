package dev.darcosse.ultrabeasts.fabric.client.particle;

import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;

public class WormholeParticle extends SpriteBillboardParticle {
    private final double startX, startY, startZ;
    private final double destX, destY, destZ;
    private final double orbitOffset;

    public WormholeParticle(ClientWorld world, double x, double y, double z,
                            double vX, double vY, double vZ, SpriteProvider sprite) {
        super(world, x, y, z, 0, 0, 0);
        this.startX = x;
        this.startY = y;
        this.startZ = z;
        this.destX = vX;
        this.destY = vY;
        this.destZ = vZ;

        this.orbitOffset = world.random.nextDouble() * Math.PI * 2;
        this.maxAge = 40; // Temps pour parcourir les 2 blocs
        this.scale = 0.4f;
        this.setSprite(sprite);
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

        // 1. Calcul de la progression (0.0 au début, 1.0 à la fin)
        float progress = (float) this.age / this.maxAge;

        // 2. Déplacement linéaire sur l'axe Z
        // La particule part de sa position d'origine (startZ)
        // et recule de 2 blocs (progress * 2.0)
        this.z = startZ - (progress * 2.0);

        if (this.y > destY) {
            this.y = this.y - 0.3;
        } else if (this.y < destY) {
            this.y = this.y + 0.3;
        }



        // 3. Gestion de l'opacité (Optionnel mais recommandé)
        // La particule devient transparente vers la fin pour éviter un "pop" sec
        this.alpha = 1.0f - progress;
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }
}