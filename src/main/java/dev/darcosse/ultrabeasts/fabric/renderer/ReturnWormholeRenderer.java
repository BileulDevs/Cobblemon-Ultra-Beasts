package dev.darcosse.ultrabeasts.fabric.renderer;

import dev.darcosse.ultrabeasts.fabric.UltraBeasts;
import dev.darcosse.ultrabeasts.fabric.entity.ReturnWormholeEntity;
import dev.darcosse.ultrabeasts.fabric.entity.WormholeEntity;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

public class ReturnWormholeRenderer extends EntityRenderer<ReturnWormholeEntity> {

    private static final Identifier TEXTURE = Identifier.of(UltraBeasts.MOD_ID, "textures/entity/wormhole.png");

    public ReturnWormholeRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public void render(ReturnWormholeEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {

        matrices.push();

        matrices.multiply(this.dispatcher.getRotation());
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f));

        float scale = 2f;
        matrices.scale(scale, scale, scale);

        float time = (entity.age + tickDelta) * 2f;

        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(time));
        drawQuad(matrices, vertexConsumers, ModRenderLayers.wormhole(TEXTURE));
        matrices.pop();

        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-time * 0.5f));
        matrices.scale(0.8f, 0.8f, 0.8f);
        drawQuad(matrices, vertexConsumers, ModRenderLayers.wormhole(TEXTURE));
        matrices.pop();

        matrices.pop();
    }

    private void drawQuad(MatrixStack matrices, VertexConsumerProvider vertexConsumers, net.minecraft.client.render.RenderLayer layer) {
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(layer);
        MatrixStack.Entry entry = matrices.peek();

        vertexConsumer.vertex(entry.getPositionMatrix(), -0.5f, -0.5f, 0f)
                .color(255, 255, 255, 255)
                .texture(0f, 0f)
                .overlay(0)
                .light(0x00F000F0) // FULL BRIGHT
                .normal(entry, 0f, 0f, 1f);

        vertexConsumer.vertex(entry.getPositionMatrix(), -0.5f, 0.5f, 0f)
                .color(255, 255, 255, 255)
                .texture(0f, 1f)
                .overlay(0)
                .light(0x00F000F0)
                .normal(entry, 0f, 0f, 1f);

        vertexConsumer.vertex(entry.getPositionMatrix(), 0.5f, 0.5f, 0f)
                .color(255, 255, 255, 255)
                .texture(1f, 1f)
                .overlay(0)
                .light(0x00F000F0)
                .normal(entry, 0f, 0f, 1f);

        vertexConsumer.vertex(entry.getPositionMatrix(), 0.5f, -0.5f, 0f)
                .color(255, 255, 255, 255)
                .texture(1f, 0f)
                .overlay(0)
                .light(0x00F000F0)
                .normal(entry, 0f, 0f, 1f);
    }

    @Override
    public Identifier getTexture(ReturnWormholeEntity entity) {
        return TEXTURE;
    }
}
