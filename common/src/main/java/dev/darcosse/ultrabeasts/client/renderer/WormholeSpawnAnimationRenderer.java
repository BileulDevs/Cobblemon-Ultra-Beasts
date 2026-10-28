package dev.darcosse.ultrabeasts.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.darcosse.ultrabeasts.UltraBeasts;
import dev.darcosse.ultrabeasts.entity.WormholeSpawnAnimation;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.Random;

/**
 * Draws the tear of the wormhole's arrival ({@link WormholeSpawnAnimation}), facing the
 * camera like the portal itself:
 * <ul>
 *   <li>a jagged line of light, growing from the centre;</li>
 *   <li>which splits into a lens onto the void beyond, edged with light;</li>
 *   <li>which turns half a revolution while rounding into the portal's disc, then dissolves
 *   into the swirl of particles that the animation ramps up meanwhile.</li>
 * </ul>
 * The void is a texture of its own: the portal's {@code wormhole.png} is fully transparent
 * (it has been since 3.x), so the portal itself is only its particles, and the tear has to
 * fade out rather than hand over to a disc.
 * <p>
 * Client side only. Cost while an animation plays: about 25 textured quads and 150 glow
 * quads per frame; nothing at all otherwise.
 */
public class WormholeSpawnAnimationRenderer extends EntityRenderer<WormholeSpawnAnimation> {

    /**
     * The void seen through the tear: a radial gradient, dark at the centre and pink at the
     * rim, laid on the lens' own shape so it turns with it.
     */
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(UltraBeasts.MOD_ID, "textures/entity/wormhole_tear.png");

    /**
     * The light of the crack: white, fading out across its width, tinted per layer by the
     * vertex colour. A texture rather than the vanilla lightning render type used at first:
     * that one did not show in game, while this goes through the same entity translucent
     * path as the lens, which does.
     */
    private static final ResourceLocation GLOW_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(UltraBeasts.MOD_ID, "textures/entity/wormhole_glow.png");

    /** Segments of the crack. More is smoother at the lens' tips, but each is several quads. */
    private static final int SEGMENTS = 24;

    /**
     * Largest sideways kink of the crack, in blocks. Above ~0.25 the lens edges cross each
     * other where two kinks meet at the start of the opening.
     */
    private static final double CRACK_JITTER = 0.16;

    /** Half-width of the fully opened lens at its middle, in blocks, before it rounds out. */
    private static final double LENS_HALF_WIDTH = 0.55;

    /**
     * Exponent of the lens profile: 0.5 gives an ellipse, 1 a pointed almond. In between,
     * the lens reads as a tear without looking like an eye.
     */
    private static final double LENS_PROFILE_EXPONENT = 0.75;

    /** Radius of the portal's disc, in blocks: its quad is 1 unit drawn at scale 2. */
    private static final double PORTAL_RADIUS = 1.0;

    /**
     * Share of the opening it takes the lens to become fully opaque. The texture fades in
     * rather than popping, but is solid well before the spiral starts.
     */
    private static final double LENS_FADE_SHARE = 0.6;

    /**
     * Share of the spiral after which the lens starts dissolving, reaching nothing at the
     * end, while the swirl particles ramp up. Lower hands over to the swirl sooner.
     */
    private static final double DISSOLVE_FROM = 0.5;

    /**
     * Glow layers, widest first: half-width in blocks, then red, green, blue, alpha. Alpha
     * blended, not additive, so they also read against a bright daytime sky.
     */
    private static final float[][] GLOW_LAYERS = {
            {0.22f, 0.55f, 0.75f, 1.0f, 0.45f},  // azure halo
            {0.10f, 1.0f, 0.55f, 0.85f, 0.8f},    // pink
            {0.04f, 1.0f, 1.0f, 1.0f, 1.0f},      // white core
    };

    /**
     * Amplitude of the crack's live tremor, in blocks, and its speed in radians per tick.
     * It makes the line crackle like an electric arc while it tears, and calms down as the
     * lens opens. Above ~0.06 the line looks like it is wobbling rather than sparking.
     */
    private static final double TREMOR_AMPLITUDE = 0.04;
    private static final double TREMOR_SPEED = 1.9;

    /** Flicker speed of the glow, in radians per tick, and its depth (share of the alpha). */
    private static final float FLICKER_SPEED = 2.3f;
    private static final float FLICKER_DEPTH = 0.25f;


    /** Called by the loader's renderer registration. */
    public WormholeSpawnAnimationRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    /** Draws the tear for the current moment of the animation. */
    @Override
    public void render(WormholeSpawnAnimation entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int light) {
        float t = entity.elapsed(partialTick);
        if (t <= 0.0f || t >= WormholeSpawnAnimation.TOTAL_TICKS) {
            return;
        }

        double grow = WormholeSpawnAnimation.easeOut(t / WormholeSpawnAnimation.CRACK_TICKS);
        double open = t < WormholeSpawnAnimation.OPEN_AT ? 0.0
                : WormholeSpawnAnimation.easeInOut((t - WormholeSpawnAnimation.OPEN_AT) / WormholeSpawnAnimation.OPEN_TICKS);
        double morph = t < WormholeSpawnAnimation.SPIRAL_AT ? 0.0
                : WormholeSpawnAnimation.easeInOut((t - WormholeSpawnAnimation.SPIRAL_AT) / WormholeSpawnAnimation.SPIRAL_TICKS);

        double fullHalfLength = WormholeSpawnAnimation.CRACK_HALF_LENGTH;
        double span = t < WormholeSpawnAnimation.OPEN_AT
                ? fullHalfLength * grow
                : Mth.lerp(morph, fullHalfLength, PORTAL_RADIUS);
        // Half a turn while rounding out: on a circle the last part of the turn is invisible,
        // so the motion settles by itself.
        double theta = Math.PI * morph;
        double cos = Math.cos(theta);
        double sin = Math.sin(theta);

        double[] jitter = jitter(entity.crackSeed());
        double[] halfWidthNorm = new double[SEGMENTS + 1];
        double[] yNorm = new double[SEGMENTS + 1];
        double[] leftX = new double[SEGMENTS + 1];
        double[] rightX = new double[SEGMENTS + 1];
        double[] leftY = new double[SEGMENTS + 1];
        double[] rightY = new double[SEGMENTS + 1];
        double[] midX = new double[SEGMENTS + 1];
        double[] midY = new double[SEGMENTS + 1];

        for (int k = 0; k <= SEGMENTS; k++) {
            double yn = -1.0 + 2.0 * k / SEGMENTS;
            double y = yn * span;
            // The kinks fade as the lens rounds out: the disc has a clean edge.
            double tremor = TREMOR_AMPLITUDE * Math.sin(t * TREMOR_SPEED + k * 1.7) * (1.0 - open);
            double cx = (jitter[k] + tremor) * (1.0 - morph);
            double lens = open * LENS_HALF_WIDTH * Math.pow(Math.max(0.0, 1.0 - yn * yn), LENS_PROFILE_EXPONENT);
            double disc = Math.sqrt(Math.max(0.0, 1.0 - yn * yn)) * PORTAL_RADIUS;
            double halfWidth = Mth.lerp(morph, lens, disc);
            // Texture coordinates in the shape's own frame, relative to its length: the
            // radial texture is sampled along a thin slice while the lens is narrow, and in
            // full once it is a disc.
            halfWidthNorm[k] = span > 1.0E-4 ? halfWidth / span : 0.0;
            yNorm[k] = yn;

            leftX[k] = (cx - halfWidth) * cos - y * sin;
            leftY[k] = (cx - halfWidth) * sin + y * cos;
            rightX[k] = (cx + halfWidth) * cos - y * sin;
            rightY[k] = (cx + halfWidth) * sin + y * cos;
            midX[k] = cx * cos - y * sin;
            midY[k] = cx * sin + y * cos;
        }

        poseStack.pushPose();
        // Same orientation as the portal's quads (AbstractWormholeRenderer), so the texture
        // seen through the lens is the one the portal will show.
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180f));
        PoseStack.Pose pose = poseStack.last();

        if (open > 0.0) {
            double fadeIn = Mth.clamp(open / LENS_FADE_SHARE, 0.0, 1.0);
            double fadeOut = 1.0 - Mth.clamp((morph - DISSOLVE_FROM) / (1.0 - DISSOLVE_FROM), 0.0, 1.0);
            float alpha = (float) (fadeIn * fadeOut);
            if (alpha > 0.0f) {
                drawLens(pose, bufferSource.getBuffer(ModRenderTypes.wormhole(TEXTURE)),
                        leftX, leftY, rightX, rightY, halfWidthNorm, yNorm, alpha);
            }
        }

        float flicker = 1.0f - FLICKER_DEPTH * (0.5f + 0.5f * Mth.sin(t * FLICKER_SPEED));
        // Fetched after the lens: buffers of different render types are flushed in the order
        // they are first used, so the light is drawn over the void, not under it.
        VertexConsumer glow = bufferSource.getBuffer(ModRenderTypes.wormhole(GLOW_TEXTURE));
        if (open <= 0.0) {
            drawGlow(pose, glow, midX, midY, flicker);
        } else {
            // The light runs along both lips of the tear, and dies as it becomes the portal.
            float fade = (float) (1.0 - morph) * flicker;
            drawGlow(pose, glow, leftX, leftY, fade);
            drawGlow(pose, glow, rightX, rightY, fade);
        }

        poseStack.popPose();
    }

    /**
     * Sideways offsets of the crack's points, from the animation's seed: the same crack on
     * every client and every frame. Zero at the centre, where the tear starts, and smaller
     * at the tips.
     */
    private static double[] jitter(long seed) {
        Random random = new Random(seed);
        double[] offsets = new double[SEGMENTS + 1];
        for (int k = 0; k <= SEGMENTS; k++) {
            double yn = -1.0 + 2.0 * k / SEGMENTS;
            offsets[k] = (random.nextDouble() * 2.0 - 1.0) * CRACK_JITTER * (1.0 - Math.abs(yn) * 0.5);
        }
        offsets[SEGMENTS / 2] = 0.0;
        return offsets;
    }

    /** Fills the lens between its two edges with the void texture. */
    private static void drawLens(PoseStack.Pose pose, VertexConsumer consumer,
                                 double[] leftX, double[] leftY, double[] rightX, double[] rightY,
                                 double[] halfWidthNorm, double[] yNorm, float alpha) {
        int a = Math.round(alpha * 255.0f);
        // NO_OVERLAY below, not the 0 the portal writes: 0 is the red damage row, which only
        // goes unnoticed on the portal because its texture is transparent.
        for (int k = 0; k < SEGMENTS; k++) {
            textured(consumer, pose, leftX[k], leftY[k], -halfWidthNorm[k], yNorm[k], a);
            textured(consumer, pose, leftX[k + 1], leftY[k + 1], -halfWidthNorm[k + 1], yNorm[k + 1], a);
            textured(consumer, pose, rightX[k + 1], rightY[k + 1], halfWidthNorm[k + 1], yNorm[k + 1], a);
            textured(consumer, pose, rightX[k], rightY[k], halfWidthNorm[k], yNorm[k], a);
        }
    }

    /**
     * One lens vertex at (x, y), sampling the texture at the shape-relative point (sx, sy),
     * both in [-1, 1].
     */
    private static void textured(VertexConsumer consumer, PoseStack.Pose pose, double x, double y,
                                 double sx, double sy, int alpha) {
        consumer.addVertex(pose, (float) x, (float) y, 0f)
                .setColor(255, 255, 255, alpha)
                .setUv((float) (sx * 0.5 + 0.5), (float) (sy * 0.5 + 0.5))
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0f, 0f, 1f);
    }

    /** Draws a glowing polyline as layered ribbons, widest first, each over the previous one. */
    private static void drawGlow(PoseStack.Pose pose, VertexConsumer consumer, double[] xs, double[] ys, float strength) {
        if (strength <= 0.0f) {
            return;
        }
        for (float[] layer : GLOW_LAYERS) {
            float halfWidth = layer[0];
            float alpha = layer[4] * strength;
            for (int k = 0; k < SEGMENTS; k++) {
                ribbon(consumer, pose, xs[k], ys[k], xs[k + 1], ys[k + 1], halfWidth,
                        layer[1], layer[2], layer[3], alpha);
            }
        }
    }

    /**
     * One ribbon segment from (x1, y1) to (x2, y2), textured across its width with the glow
     * falloff. Lengthened a little at both ends so consecutive segments overlap at the kinks.
     */
    private static void ribbon(VertexConsumer consumer, PoseStack.Pose pose, double x1, double y1, double x2, double y2,
                               float halfWidth, float r, float g, float b, float a) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double length = Math.sqrt(dx * dx + dy * dy);
        if (length < 1.0E-5) {
            return;
        }
        float nx = (float) (-dy / length * halfWidth);
        float ny = (float) (dx / length * halfWidth);
        float ex = (float) (dx / length * halfWidth * 0.5);
        float ey = (float) (dy / length * halfWidth * 0.5);

        float ax = (float) x1 - ex, ay = (float) y1 - ey;
        float bx = (float) x2 + ex, by = (float) y2 + ey;

        // u runs across the ribbon (0 on one side, 1 on the other): the texture's falloff.
        glowVertex(consumer, pose, ax + nx, ay + ny, 0f, 0f, r, g, b, a);
        glowVertex(consumer, pose, ax - nx, ay - ny, 1f, 0f, r, g, b, a);
        glowVertex(consumer, pose, bx - nx, by - ny, 1f, 1f, r, g, b, a);
        glowVertex(consumer, pose, bx + nx, by + ny, 0f, 1f, r, g, b, a);
    }

    /** One glow vertex, full bright, tinted by the layer's colour. */
    private static void glowVertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float u, float v,
                                   float r, float g, float b, float a) {
        consumer.addVertex(pose, x, y, 0f)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0f, 0f, 1f);
    }

    /** Required by the renderer contract: the void texture. */
    @Override
    public ResourceLocation getTextureLocation(WormholeSpawnAnimation entity) {
        return TEXTURE;
    }
}
