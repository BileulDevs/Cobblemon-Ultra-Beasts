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

/**
 * Draws the singularity of the wormhole's arrival ({@link WormholeSpawnAnimation}), facing the
 * camera like the portal itself:
 * <ul>
 *   <li>a black core with a glowing rim, growing, ringed by arcs of light that spin faster
 *   and faster, with ripples of light falling into it;</li>
 *   <li>the core shrinking to a point as the arcs close in;</li>
 *   <li>a shockwave racing out while the portal's disc snaps open, a little too wide, then
 *   dissolving into the swirl.</li>
 * </ul>
 * <p>
 * Client side only. Cost while an animation plays: at most ~50 textured quads and ~900 glow
 * quads per frame (rings are 48 segments times 3 layers); nothing at all otherwise.
 */
public class WormholeSpawnAnimationRenderer extends EntityRenderer<WormholeSpawnAnimation> {

    /**
     * The void: a radial gradient, dark at the centre and pink at the rim. Mapped onto discs
     * so the rim of the texture falls on the rim of the shape.
     */
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(UltraBeasts.MOD_ID, "textures/entity/wormhole_tear.png");

    /** Light: white, fading out across its width, tinted per layer by the vertex colour. */
    private static final ResourceLocation GLOW_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(UltraBeasts.MOD_ID, "textures/entity/wormhole_glow.png");

    /** Radius of the portal's disc, in blocks: its quad is 1 unit drawn at scale 2. */
    private static final double PORTAL_RADIUS = 1.0;

    /** Segments of a full circle. More is rounder, each costs a few quads per glow layer. */
    private static final int CIRCLE_SEGMENTS = 48;

    /**
     * Glow layers, widest first: half-width in blocks, then red, green, blue, alpha. Alpha
     * blended, not additive, so they also read against a bright daytime sky. Shared with the
     * tear of 4.1.
     */
    private static final float[][] GLOW_LAYERS = {
            {0.22f, 0.55f, 0.75f, 1.0f, 0.45f},  // azure halo
            {0.10f, 1.0f, 0.55f, 0.85f, 0.8f},    // pink
            {0.04f, 1.0f, 1.0f, 1.0f, 1.0f},      // white core
    };

    /**
     * Share of the settling phase after which the disc starts dissolving into the swirl,
     * reaching nothing at the end. Lower hands over to the swirl sooner.
     */
    private static final double DISSOLVE_FROM = 0.5;

    /** Radius of the core at the end of the gathering, in blocks. */
    private static final double CORE_MAX_RADIUS = 0.45;

    /** Radius of the core at the end of the collapse: a point, not quite nothing. */
    private static final double CORE_MIN_RADIUS = 0.04;

    /** Number of arcs spinning around the core. */
    private static final int ARC_COUNT = 3;

    /** Angular span of one arc, in radians (about 100 degrees). */
    private static final double ARC_SPAN = 1.75;

    /**
     * Distance of the arcs from the core's centre, in core radii, at the start of the
     * gathering. They close in on the core during the collapse.
     */
    private static final double ARC_DISTANCE = 2.2;

    /**
     * Spin of the arcs, in radians per tick, at the start and at the end of the collapse. It
     * accelerates in between, which is what sells the wind-up.
     */
    private static final double ARC_SPIN_START = 0.05;
    private static final double ARC_SPIN_END = 0.9;

    /** Ticks between two infalling ripples, and how long one takes to reach the core. */
    private static final int RIPPLE_INTERVAL = 10;
    private static final int RIPPLE_TICKS = 20;

    /** Radius ripples start from, in blocks. */
    private static final double RIPPLE_START_RADIUS = 3.0;

    /** How far the shockwave reaches, in blocks, and how long it takes. */
    private static final double SHOCKWAVE_RADIUS = 7.0;
    private static final int SHOCKWAVE_TICKS = 18;

    /** Width of the shockwave relative to the other glows: a blast, not a line. */
    private static final float SHOCKWAVE_WIDTH = 2.5f;

    /** Ticks the disc takes to snap open after the impact. */
    private static final int DISC_OPEN_TICKS = 10;

    /** Called by the loader's renderer registration. */
    public WormholeSpawnAnimationRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    /** Draws the singularity for the current moment of the animation. */
    @Override
    public void render(WormholeSpawnAnimation entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int light) {
        float t = entity.elapsed(partialTick);
        if (t <= 0.0f || t >= WormholeSpawnAnimation.TOTAL_TICKS) {
            return;
        }

        poseStack.pushPose();
        faceCamera(poseStack);
        PoseStack.Pose pose = poseStack.last();

        // The void first, then the light: buffers of different render types are flushed in
        // the order they are first used, so the light is drawn over the void, not under it.
        VertexConsumer voidBuffer = bufferSource.getBuffer(ModRenderTypes.wormhole(TEXTURE));

        if (t < WormholeSpawnAnimation.IMPACT_AT) {
            double core = coreRadius(t);
            disc(pose, voidBuffer, 0, 0, core, 1.0f);

            VertexConsumer glow = bufferSource.getBuffer(ModRenderTypes.wormhole(GLOW_TEXTURE));
            double gather = Mth.clamp(t / WormholeSpawnAnimation.GATHER_TICKS, 0.0, 1.0);
            arc(pose, glow, 0, 0, core, 0, Math.PI * 2, 0.5f, (float) (0.4 + 0.6 * gather));
            drawArcs(pose, glow, t, core);
            drawRipples(pose, glow, t, core);
        } else {
            double since = t - WormholeSpawnAnimation.IMPACT_AT;
            double settle = since / WormholeSpawnAnimation.SETTLE_TICKS;
            float alpha = dissolve(settle);
            double radius = PORTAL_RADIUS * WormholeSpawnAnimation.easeOutBack(since / DISC_OPEN_TICKS);
            disc(pose, voidBuffer, 0, 0, radius, alpha);

            VertexConsumer glow = bufferSource.getBuffer(ModRenderTypes.wormhole(GLOW_TEXTURE));
            arc(pose, glow, 0, 0, radius, 0, Math.PI * 2, 0.6f, alpha);
            if (since < SHOCKWAVE_TICKS) {
                double wave = since / SHOCKWAVE_TICKS;
                float fade = (float) (1.0 - wave);
                double reach = SHOCKWAVE_RADIUS * WormholeSpawnAnimation.easeOut(wave);
                arc(pose, glow, 0, 0, reach, 0, Math.PI * 2, SHOCKWAVE_WIDTH, fade);
                // A second, slower wave behind the first gives the blast some depth.
                arc(pose, glow, 0, 0, reach * 0.65, 0, Math.PI * 2, SHOCKWAVE_WIDTH * 0.5f, fade * 0.6f);
            }
        }

        poseStack.popPose();
    }

    /** Core radius: grows during the gathering, then collapses to a point. */
    private static double coreRadius(double t) {
        if (t < WormholeSpawnAnimation.COLLAPSE_AT) {
            return CORE_MAX_RADIUS * WormholeSpawnAnimation.easeOut(t / WormholeSpawnAnimation.GATHER_TICKS);
        }
        double collapse = (t - WormholeSpawnAnimation.COLLAPSE_AT) / WormholeSpawnAnimation.COLLAPSE_TICKS;
        return Mth.lerp(WormholeSpawnAnimation.easeIn(collapse), CORE_MAX_RADIUS, CORE_MIN_RADIUS);
    }

    /**
     * The arcs spinning around the core. Their angle is the integral of an accelerating spin,
     * so they wind up smoothly instead of jumping when the speed changes.
     */
    private static void drawArcs(PoseStack.Pose pose, VertexConsumer glow, double t, double core) {
        double windUp = Mth.clamp(t / WormholeSpawnAnimation.IMPACT_AT, 0.0, 1.0);
        // Integral of a spin going linearly from START to END over IMPACT_AT ticks.
        double turned = WormholeSpawnAnimation.IMPACT_AT
                * (ARC_SPIN_START * windUp + (ARC_SPIN_END - ARC_SPIN_START) * windUp * windUp / 2.0);

        double distance;
        if (t < WormholeSpawnAnimation.COLLAPSE_AT) {
            distance = CORE_MAX_RADIUS * ARC_DISTANCE;
        } else {
            // Closing in on the shrinking core.
            double collapse = (t - WormholeSpawnAnimation.COLLAPSE_AT) / WormholeSpawnAnimation.COLLAPSE_TICKS;
            distance = Mth.lerp(WormholeSpawnAnimation.easeIn(collapse), CORE_MAX_RADIUS * ARC_DISTANCE, core * 1.5);
        }

        float strength = (float) Mth.clamp(t / 20.0, 0.0, 1.0);
        for (int i = 0; i < ARC_COUNT; i++) {
            double start = turned + Math.PI * 2 * i / ARC_COUNT;
            arc(pose, glow, 0, 0, distance, start, start + ARC_SPAN, 0.45f, strength);
        }
    }

    /**
     * Rings of light shrinking into the core, one every {@value #RIPPLE_INTERVAL} ticks: the
     * pull made visible. They brighten as they fall in.
     */
    private static void drawRipples(PoseStack.Pose pose, VertexConsumer glow, double t, double core) {
        int lastBorn = (int) Math.floor(t / RIPPLE_INTERVAL);
        for (int n = lastBorn; n >= 0; n--) {
            double age = t - n * RIPPLE_INTERVAL;
            if (age >= RIPPLE_TICKS) break;
            double fall = WormholeSpawnAnimation.easeIn(age / RIPPLE_TICKS);
            double radius = Mth.lerp(fall, RIPPLE_START_RADIUS, core);
            arc(pose, glow, 0, 0, radius, 0, Math.PI * 2, 0.35f, (float) (0.15 + 0.5 * fall));
        }
    }

    // ---- drawing helpers -------------------------------------------------

    /**
     * Fills a disc with the void texture, as a fan of quads (the render type draws quads; the
     * fourth corner of each repeats the centre).
     */
    private static void disc(PoseStack.Pose pose, VertexConsumer consumer, double cx, double cy,
                             double radius, float alpha) {
        if (radius <= 1.0E-4 || alpha <= 0.0f) return;
        int a = Math.round(Mth.clamp(alpha, 0.0f, 1.0f) * 255.0f);
        for (int k = 0; k < CIRCLE_SEGMENTS; k++) {
            double a0 = Math.PI * 2 * k / CIRCLE_SEGMENTS;
            double a1 = Math.PI * 2 * (k + 1) / CIRCLE_SEGMENTS;
            textured(consumer, pose, cx, cy, 0.0, 0.0, a);
            textured(consumer, pose, cx + Math.cos(a0) * radius, cy + Math.sin(a0) * radius,
                    Math.cos(a0), Math.sin(a0), a);
            textured(consumer, pose, cx + Math.cos(a1) * radius, cy + Math.sin(a1) * radius,
                    Math.cos(a1), Math.sin(a1), a);
            textured(consumer, pose, cx, cy, 0.0, 0.0, a);
        }
    }

    /** A glowing circle, or the part of it between two angles (radians). */
    private static void arc(PoseStack.Pose pose, VertexConsumer consumer, double cx, double cy, double radius,
                            double fromAngle, double toAngle, float widthScale, float strength) {
        if (radius <= 1.0E-4 || strength <= 0.0f) return;
        boolean full = toAngle - fromAngle >= Math.PI * 2 - 1.0E-6;
        int segments = Math.max(2, (int) Math.ceil(CIRCLE_SEGMENTS * (toAngle - fromAngle) / (Math.PI * 2)));
        int points = full ? segments : segments + 1;
        double[] xs = new double[points];
        double[] ys = new double[points];
        for (int k = 0; k < points; k++) {
            double angle = fromAngle + (toAngle - fromAngle) * k / segments;
            xs[k] = cx + Math.cos(angle) * radius;
            ys[k] = cy + Math.sin(angle) * radius;
        }
        glow(pose, consumer, xs, ys, full, widthScale, strength);
    }

    /**
     * Draws a glowing polyline as layered ribbons, widest first, each over the previous one.
     *
     * @param closed     joins the last point back to the first
     * @param widthScale multiplies every layer's width: below 1 for fine lines, above for blasts
     */
    private static void glow(PoseStack.Pose pose, VertexConsumer consumer, double[] xs, double[] ys,
                             boolean closed, float widthScale, float strength) {
        if (strength <= 0.0f) return;
        int segments = closed ? xs.length : xs.length - 1;
        for (float[] layer : GLOW_LAYERS) {
            float halfWidth = layer[0] * widthScale;
            float alpha = Math.min(1.0f, layer[4] * strength);
            for (int k = 0; k < segments; k++) {
                int next = (k + 1) % xs.length;
                ribbon(consumer, pose, xs[k], ys[k], xs[next], ys[next], halfWidth,
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

    /**
     * One void-texture vertex at (x, y), sampling the texture at the shape-relative point
     * (sx, sy), both in [-1, 1]. NO_OVERLAY, not the 0 the portal writes: 0 is the red damage
     * row, which only goes unnoticed on the portal because its texture is transparent.
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

    /** Faces the camera, like the portal's own quads, so the hand-over is seamless. */
    private void faceCamera(PoseStack poseStack) {
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180f));
    }

    /**
     * Opacity of the disc while it hands over to the swirl: full until
     * {@link #DISSOLVE_FROM} of the settling phase, then down to nothing at its end.
     */
    private static float dissolve(double settleProgress) {
        return (float) (1.0 - Mth.clamp((settleProgress - DISSOLVE_FROM) / (1.0 - DISSOLVE_FROM), 0.0, 1.0));
    }

    /** Required by the renderer contract: the void texture. */
    @Override
    public ResourceLocation getTextureLocation(WormholeSpawnAnimation entity) {
        return TEXTURE;
    }
}
