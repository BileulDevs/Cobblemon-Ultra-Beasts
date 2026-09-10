package dev.darcosse.ultrabeasts.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.darcosse.ultrabeasts.UltraBeasts;
import dev.darcosse.ultrabeasts.entity.WormholeEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class WormholeRenderer extends EntityRenderer<WormholeEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(UltraBeasts.MOD_ID, "textures/entity/wormhole.png");

    public WormholeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(WormholeEntity entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int light) {

        poseStack.pushPose();

        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180f));

        float scale = 2f;
        poseStack.scale(scale, scale, scale);

        float time = (entity.tickCount + partialTick) * 2f;

        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(time));
        drawQuad(poseStack, bufferSource, ModRenderTypes.wormhole(TEXTURE));
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(-time * 0.5f));
        poseStack.scale(0.8f, 0.8f, 0.8f);
        drawQuad(poseStack, bufferSource, ModRenderTypes.wormhole(TEXTURE));
        poseStack.popPose();

        poseStack.popPose();
    }

    static void drawQuad(PoseStack poseStack, MultiBufferSource bufferSource, RenderType layer) {
        VertexConsumer consumer = bufferSource.getBuffer(layer);
        PoseStack.Pose entry = poseStack.last();

        consumer.addVertex(entry, -0.5f, -0.5f, 0f)
                .setColor(255, 255, 255, 255)
                .setUv(0f, 0f)
                .setOverlay(0)
                .setLight(0x00F000F0) // FULL BRIGHT
                .setNormal(entry, 0f, 0f, 1f);

        consumer.addVertex(entry, -0.5f, 0.5f, 0f)
                .setColor(255, 255, 255, 255)
                .setUv(0f, 1f)
                .setOverlay(0)
                .setLight(0x00F000F0)
                .setNormal(entry, 0f, 0f, 1f);

        consumer.addVertex(entry, 0.5f, 0.5f, 0f)
                .setColor(255, 255, 255, 255)
                .setUv(1f, 1f)
                .setOverlay(0)
                .setLight(0x00F000F0)
                .setNormal(entry, 0f, 0f, 1f);

        consumer.addVertex(entry, 0.5f, -0.5f, 0f)
                .setColor(255, 255, 255, 255)
                .setUv(1f, 0f)
                .setOverlay(0)
                .setLight(0x00F000F0)
                .setNormal(entry, 0f, 0f, 1f);
    }

    @Override
    public ResourceLocation getTextureLocation(WormholeEntity entity) {
        return TEXTURE;
    }
}
