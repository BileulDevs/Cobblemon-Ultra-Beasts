package dev.darcosse.ultrabeasts.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.darcosse.ultrabeasts.UltraBeasts;
import dev.darcosse.ultrabeasts.entity.WormholeSpawnAnimation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class WormholeSpawnAnimationRenderer extends EntityRenderer<WormholeSpawnAnimation> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(UltraBeasts.MOD_ID, "textures/entity/wormhole.png");

    public WormholeSpawnAnimationRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(WormholeSpawnAnimation entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int light) {
        super.render(entity, yaw, partialTick, poseStack, bufferSource, light);
    }

    @Override
    public ResourceLocation getTextureLocation(WormholeSpawnAnimation entity) {
        return TEXTURE;
    }
}
