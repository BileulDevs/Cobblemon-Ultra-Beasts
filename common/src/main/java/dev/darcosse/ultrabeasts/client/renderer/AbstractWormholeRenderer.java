package dev.darcosse.ultrabeasts.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.darcosse.ultrabeasts.UltraBeasts;
import dev.darcosse.ultrabeasts.entity.AbstractPortalEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * Draws a portal as two counter-rotating textured quads that always face the camera.
 * Shared by the inbound and outbound portals, which looked identical and used to duplicate
 * this code line for line. Client side only.
 */
public abstract class AbstractWormholeRenderer<T extends AbstractPortalEntity> extends EntityRenderer<T> {

    /** Texture of both portals. */
    protected static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(UltraBeasts.MOD_ID, "textures/entity/wormhole.png");

    /** Size of the outer quad, in blocks. The spawn height in WormholeEntity assumes this value. */
    private static final float QUAD_SCALE = 2f;

    /** Spin speed of the outer quad, in degrees per tick. */
    private static final float SPIN_DEGREES_PER_TICK = 2f;

    /** Spin of the inner quad relative to the outer one: half speed, opposite direction. */
    private static final float INNER_SPIN_RATIO = -0.5f;

    /** Size of the inner quad relative to the outer one. */
    private static final float INNER_SCALE = 0.8f;

    /**
     * Overlay UV written on every vertex. 0 is NOT "no overlay": it selects the red damage row
     * of the overlay texture, which mixes about 30% red into the portal. Kept because it is
     * part of the current look; OverlayTexture.NO_OVERLAY would show the texture's true
     * colours.
     */
    private static final int OVERLAY_UV = 0;

    /** Creates the renderer; called by the loader's renderer registration. */
    protected AbstractWormholeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    /** Draws the two quads, billboarded towards the camera. */
    @Override
    public void render(T entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int light) {

        poseStack.pushPose();

        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180f));

        poseStack.scale(QUAD_SCALE, QUAD_SCALE, QUAD_SCALE);

        float spin = (entity.tickCount + partialTick) * SPIN_DEGREES_PER_TICK;
        RenderType renderType = ModRenderTypes.wormhole(TEXTURE);

        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(spin));
        drawQuad(poseStack, bufferSource, renderType);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(spin * INNER_SPIN_RATIO));
        poseStack.scale(INNER_SCALE, INNER_SCALE, INNER_SCALE);
        drawQuad(poseStack, bufferSource, renderType);
        poseStack.popPose();

        poseStack.popPose();
    }

    /** Emits one unit quad centred on the current pose, fully lit. */
    private static void drawQuad(PoseStack poseStack, MultiBufferSource bufferSource, RenderType layer) {
        VertexConsumer consumer = bufferSource.getBuffer(layer);
        PoseStack.Pose entry = poseStack.last();

        vertex(consumer, entry, -0.5f, -0.5f, 0f, 0f);
        vertex(consumer, entry, -0.5f, 0.5f, 0f, 1f);
        vertex(consumer, entry, 0.5f, 0.5f, 1f, 1f);
        vertex(consumer, entry, 0.5f, -0.5f, 1f, 0f);
    }

    /** Emits one white, fully lit vertex facing +Z. */
    private static void vertex(VertexConsumer consumer, PoseStack.Pose entry, float x, float y, float u, float v) {
        consumer.addVertex(entry, x, y, 0f)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OVERLAY_UV)
                // Full bright: the portal is its own light source, even in Ultra-Space's dark.
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(entry, 0f, 0f, 1f);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return TEXTURE;
    }
}
