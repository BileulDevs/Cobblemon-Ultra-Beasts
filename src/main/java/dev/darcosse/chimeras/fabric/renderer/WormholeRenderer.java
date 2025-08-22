package dev.darcosse.chimeras.fabric.renderer;

import dev.darcosse.chimeras.fabric.Chimeras;
import dev.darcosse.chimeras.fabric.entity.WormholeEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

public class WormholeRenderer extends EntityRenderer<WormholeEntity> {
    private static final Identifier TEXTURE = Identifier.of(Chimeras.MOD_ID, "textures/entity/wormhole.png");

    public WormholeRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public void render(WormholeEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {

        matrices.push();

        // Billboard : toujours orienté vers la caméra
        matrices.multiply(this.dispatcher.getRotation());
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f));

        // Taille du wormhole
        float scale = 2.0f;
        matrices.scale(scale, scale, scale);

        // Temps pour l’animation
        float time = (entity.age + tickDelta) * 2.0f;

        // === Couche principale ===
        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(time));

        drawQuad(matrices, vertexConsumers, RenderLayer.getEntityTranslucentEmissive(getTexture(entity)), light);
        matrices.pop();

        // === Couche secondaire (plus lente, pour effet vortex) ===
        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-time * 0.5f));
        matrices.scale(0.8f, 0.8f, 0.8f); // un peu plus petit

        drawQuad(matrices, vertexConsumers, RenderLayer.getEntityTranslucentEmissive(getTexture(entity)), light);
        matrices.pop();

        matrices.pop();

        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    private void drawQuad(MatrixStack matrices, VertexConsumerProvider vertexConsumers, RenderLayer layer, int light) {
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(layer);
        MatrixStack.Entry entry = matrices.peek();

        vertexConsumer.vertex(entry.getPositionMatrix(), -0.5f, -0.5f, 0.0f)
                .color(255, 255, 255, 255)
                .texture(0, 0)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(entry, 0.0f, 0.0f, 1.0f);

        vertexConsumer.vertex(entry.getPositionMatrix(), -0.5f, 0.5f, 0.0f)
                .color(255, 255, 255, 255)
                .texture(0, 1)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(entry, 0.0f, 0.0f, 1.0f);

        vertexConsumer.vertex(entry.getPositionMatrix(), 0.5f, 0.5f, 0.0f)
                .color(255, 255, 255, 255)
                .texture(1, 1)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(entry, 0.0f, 0.0f, 1.0f);

        vertexConsumer.vertex(entry.getPositionMatrix(), 0.5f, -0.5f, 0.0f)
                .color(255, 255, 255, 255)
                .texture(1, 0)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(entry, 0.0f, 0.0f, 1.0f);
    }

    @Override
    public Identifier getTexture(WormholeEntity entity) {
        return TEXTURE;
    }
}