package dev.darcosse.ultrabeasts.fabric.client.particle;

import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;

public class SparkParticle extends SpriteBillboardParticle {

    // On déclare notre propre variable de vitesse
    private final float angularVelocity;

    public SparkParticle(ClientWorld world, double x, double y, double z,
                         SpriteProvider spriteProvider) {
        super(world, x, y, z, 0, 0, 0);

        this.maxAge = 40;
        this.scale = 0.5f;
        this.setSprite(spriteProvider);

        // Initialisation de l'angle et de la vitesse de rotation personnalisée
        this.angle = (float) (world.random.nextDouble() * Math.PI * 2);
        this.prevAngle = this.angle;
        this.angularVelocity = (float) (world.random.nextDouble() - 0.5) * 0.1f;
    }

    @Override
    public void tick() {
        this.prevAngle = this.angle; // Stocke l'angle précédent pour l'interpolation
        super.tick(); // Gère le mouvement et la durée de vie

        // Mise à jour manuelle de l'angle puisque le champ natif est introuvable
        this.angle += this.angularVelocity;
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }
}