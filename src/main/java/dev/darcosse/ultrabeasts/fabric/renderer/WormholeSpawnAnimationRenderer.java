package dev.darcosse.ultrabeasts.fabric.renderer;

import dev.darcosse.ultrabeasts.fabric.entity.WormholeSpawnAnimation;
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
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    @Override
    public Identifier getTexture(WormholeSpawnAnimation entity) {
        return null;
    }
}