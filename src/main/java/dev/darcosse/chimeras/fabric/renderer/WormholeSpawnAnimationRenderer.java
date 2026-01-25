package dev.darcosse.chimeras.fabric.renderer;

import dev.darcosse.chimeras.fabric.entity.WormholeSpawnAnimation;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class WormholeSpawnAnimationRenderer extends EntityRenderer<WormholeSpawnAnimation> {

    public WormholeSpawnAnimationRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public void render(WormholeSpawnAnimation entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        // Pas de rendu visuel, tout se fait via particules dans clientAnimation()
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    @Override
    public Identifier getTexture(WormholeSpawnAnimation entity) {
        // Pas de texture nécessaire
        return null;
    }
}