package dev.darcosse.ultrabeasts.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.darcosse.ultrabeasts.UltraBeasts;
import dev.darcosse.ultrabeasts.entity.ReturnWormholeEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class ReturnWormholeRenderer extends EntityRenderer<ReturnWormholeEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(UltraBeasts.MOD_ID, "textures/entity/wormhole.png");

    public ReturnWormholeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(ReturnWormholeEntity entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int light) {

        poseStack.pushPose();

        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180f));

        float scale = 2f;
        poseStack.scale(scale, scale, scale);

        float time = (entity.tickCount + partialTick) * 2f;

        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(time));
        WormholeRenderer.drawQuad(poseStack, bufferSource, ModRenderTypes.wormhole(TEXTURE));
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(-time * 0.5f));
        poseStack.scale(0.8f, 0.8f, 0.8f);
        WormholeRenderer.drawQuad(poseStack, bufferSource, ModRenderTypes.wormhole(TEXTURE));
        poseStack.popPose();

        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(ReturnWormholeEntity entity) {
        return TEXTURE;
    }
}
