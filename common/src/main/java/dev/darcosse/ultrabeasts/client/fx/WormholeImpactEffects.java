package dev.darcosse.ultrabeasts.client.fx;

import dev.darcosse.ultrabeasts.fx.WormholeFxBridge;
import dev.darcosse.ultrabeasts.fx.WormholeImpactLevers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Client side of the inbound wormhole's impact: camera shake, screen flash, vignette,
 * darkening, ground shockwave and title, each behind its switch in {@link WormholeImpactLevers}.
 * <p>
 * The screen effects are pure functions of the level clock: the animation reports when it
 * started and when its impact falls, and every frame derives its envelope from those two
 * times. Nothing ticks, so an effect cannot get stuck if a tick is missed, and a client that
 * joins mid-animation lands at the right point. The two one-shot effects (title, ground wave)
 * fire from {@link #onImpact}.
 * <p>
 * Client only. Cost: nothing while idle (one null check per frame from the GUI and camera
 * hooks); during an arrival, at most three full-screen fills and ~50 thin strips per frame,
 * and a one-off burst of about {@value WormholeImpactLevers#GROUND_WAVE_PARTICLES} x 2
 * particles.
 */
public final class WormholeImpactEffects implements WormholeFxBridge.Handler {

    /** The handler installed into the bridge. */
    public static final WormholeImpactEffects INSTANCE = new WormholeImpactEffects();

    /** Vertical strips drawn per side for the vignette. More is smoother, each is one quad. */
    private static final int VIGNETTE_SIDE_STRIPS = 16;

    /** Yaw and pitch frequencies of the shake, in radians per tick. Unrelated so it never loops visibly. */
    private static final float SHAKE_YAW_FREQUENCY = 1.9f;
    private static final float SHAKE_YAW_FREQUENCY_2 = 4.3f;
    private static final float SHAKE_PITCH_FREQUENCY = 2.3f;
    private static final float SHAKE_PITCH_FREQUENCY_2 = 5.1f;

    /** Pitch shake relative to yaw: vertical shake reads as stronger, so it is toned down. */
    private static final float SHAKE_PITCH_RATIO = 0.7f;

    /** Upward speed of the ground wave's dust, in blocks per tick: lifts it off the grass. */
    private static final double GROUND_WAVE_LIFT = 0.02;

    /** Upward speed of the debris thrown near the centre of the ground wave. */
    private static final double GROUND_DEBRIS_LIFT = 0.25;

    /** Radius of the ring the ground wave starts from, in blocks. */
    private static final double GROUND_WAVE_START_RADIUS = 0.6;

    /** Translation key of the impact title, and of its subtitle. */
    private static final String TITLE_KEY = "title.ultrabeasts.wormhole_impact";
    private static final String SUBTITLE_KEY = "title.ultrabeasts.wormhole_impact.subtitle";

    /** Fully transparent black, the far end of the vignette's gradients. */
    private static final int TRANSPARENT = 0x00000000;

    /** Whether an arrival is being played; false once every envelope has run out. */
    private boolean active;

    /** Level the arrival belongs to: a dimension change or a disconnect drops the effects. */
    private ClientLevel level;

    private Vec3 origin;
    private long startGameTime;
    private long impactGameTime;

    /** Handler instance only; see {@link #INSTANCE}. */
    private WormholeImpactEffects() {
    }

    // ---- bridge ----------------------------------------------------------

    /** Records the timeline of a new arrival. A newer arrival replaces an older one. */
    @Override
    public void onArrivalStarted(Vec3 origin, long startGameTime, long impactGameTime) {
        ClientLevel current = Minecraft.getInstance().level;
        if (current == null) return;

        this.level = current;
        this.origin = origin;
        this.startGameTime = startGameTime;
        this.impactGameTime = impactGameTime;
        this.active = true;
    }

    /** Fires the one-shot effects. Also anchors the timeline if the start was never seen. */
    @Override
    public void onImpact(Vec3 origin) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel current = minecraft.level;
        if (current == null) return;

        if (!active || this.level != current) {
            // Should not happen (the start is reported on the animation's first client tick),
            // but a missing start must not leave the screen effects unplayed.
            long now = current.getGameTime();
            onArrivalStarted(origin, now, now);
        }

        float strength = strengthAt(origin);
        if (strength <= 0.0f) return;

        if (WormholeImpactLevers.IMPACT_TITLE) {
            minecraft.gui.setTimes(WormholeImpactLevers.TITLE_FADE_IN_TICKS,
                    WormholeImpactLevers.TITLE_STAY_TICKS, WormholeImpactLevers.TITLE_FADE_OUT_TICKS);
            // Subtitle first: setting the title is what shows both.
            minecraft.gui.setSubtitle(Component.translatable(SUBTITLE_KEY));
            minecraft.gui.setTitle(Component.translatable(TITLE_KEY));
        }

        if (WormholeImpactLevers.GROUND_WAVE) {
            spawnGroundWave(current, origin);
        }
    }

    /** Eyes of the local player, or null outside a world. */
    @Override
    public Vec3 viewerPosition() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null ? player.getEyePosition() : null;
    }

    // ---- per-frame hooks -------------------------------------------------

    /**
     * Shake to add to the camera this frame, as {yaw, pitch} in degrees, or null for none.
     * Called by {@code CameraMixin} once per frame.
     */
    public static float[] cameraShake(float partialTick) {
        if (!WormholeImpactLevers.CAMERA_SHAKE) return null;

        WormholeImpactEffects fx = INSTANCE;
        if (!fx.isPlaying()) return null;

        double sinceImpact = fx.sinceImpact(partialTick);
        if (sinceImpact < 0 || sinceImpact > WormholeImpactLevers.SHAKE_TICKS) return null;

        float decay = 1.0f - (float) sinceImpact / WormholeImpactLevers.SHAKE_TICKS;
        float amplitude = WormholeImpactLevers.SHAKE_MAX_DEGREES * fx.strengthAt(fx.origin) * decay * decay;
        if (amplitude <= 0.0f) return null;

        float t = (float) sinceImpact;
        float yaw = amplitude * (Mth.sin(t * SHAKE_YAW_FREQUENCY) + 0.5f * Mth.sin(t * SHAKE_YAW_FREQUENCY_2));
        float pitch = amplitude * SHAKE_PITCH_RATIO
                * (Mth.cos(t * SHAKE_PITCH_FREQUENCY) + 0.5f * Mth.sin(t * SHAKE_PITCH_FREQUENCY_2));
        return new float[]{yaw, pitch};
    }

    /**
     * Draws the screen effects over the HUD: darkening, then vignette, then flash, so the
     * flash covers everything. Called by {@code GuiMixin} once per frame.
     */
    public static void renderOverlay(GuiGraphics graphics, float partialTick) {
        WormholeImpactEffects fx = INSTANCE;
        if (!fx.isPlaying()) return;

        double sinceImpact = fx.sinceImpact(partialTick);
        float strength = fx.strengthAt(fx.origin);

        if (sinceImpact > fx.longestEnvelope()) {
            // Every envelope has run out: stop paying even the checks above.
            fx.active = false;
            return;
        }
        if (strength <= 0.0f) return;

        int width = graphics.guiWidth();
        int height = graphics.guiHeight();

        if (WormholeImpactLevers.DARKEN) {
            float alpha = fx.darkenAlpha(sinceImpact, partialTick) * strength;
            if (alpha > 0.0f) {
                graphics.fill(0, 0, width, height, argb(alpha, 0x000000));
            }
        }

        if (WormholeImpactLevers.SCREEN_VIGNETTE) {
            float alpha = vignetteAlpha(sinceImpact) * strength;
            if (alpha > 0.0f) {
                drawVignette(graphics, width, height, alpha);
            }
        }

        if (WormholeImpactLevers.SCREEN_FLASH) {
            float alpha = flashAlpha(sinceImpact) * strength;
            if (alpha > 0.0f) {
                graphics.fill(0, 0, width, height, argb(alpha, 0xFFFFFF));
            }
        }
    }

    // ---- envelopes -------------------------------------------------------

    /** Whether an arrival is playing in the level the player is in. */
    private boolean isPlaying() {
        return active && level != null && level == Minecraft.getInstance().level;
    }

    /** Ticks since the impact, negative before it, on the level clock. */
    private double sinceImpact(float partialTick) {
        return (level.getGameTime() - impactGameTime) + partialTick;
    }

    /** Ticks after the impact by which every effect is over. */
    private int longestEnvelope() {
        return Math.max(Math.max(WormholeImpactLevers.SHAKE_TICKS, WormholeImpactLevers.FLASH_TICKS),
                Math.max(WormholeImpactLevers.VIGNETTE_FADE_TICKS, WormholeImpactLevers.DARKEN_RELEASE_TICKS));
    }

    /**
     * Black veil: grows over the whole build-up with an ease-in (the light seems to drain
     * faster as the impact nears), then lifts linearly.
     */
    private float darkenAlpha(double sinceImpact, float partialTick) {
        if (sinceImpact < 0) {
            double buildUp = impactGameTime - startGameTime;
            if (buildUp <= 0) return 0.0f;
            double progress = Mth.clamp(((level.getGameTime() - startGameTime) + partialTick) / buildUp, 0.0, 1.0);
            return (float) (WormholeImpactLevers.DARKEN_MAX_ALPHA * progress * progress);
        }
        double release = 1.0 - sinceImpact / WormholeImpactLevers.DARKEN_RELEASE_TICKS;
        return release <= 0 ? 0.0f : (float) (WormholeImpactLevers.DARKEN_MAX_ALPHA * release);
    }

    /** Vignette: swells linearly before the impact, then fades linearly. */
    private static float vignetteAlpha(double sinceImpact) {
        double amount;
        if (sinceImpact < 0) {
            amount = 1.0 + sinceImpact / WormholeImpactLevers.VIGNETTE_BUILD_TICKS;
        } else {
            amount = 1.0 - sinceImpact / WormholeImpactLevers.VIGNETTE_FADE_TICKS;
        }
        return (float) (WormholeImpactLevers.VIGNETTE_MAX_ALPHA * Mth.clamp(amount, 0.0, 1.0));
    }

    /** Flash: full on the impact, decaying quadratically. Nothing before. */
    private static float flashAlpha(double sinceImpact) {
        if (sinceImpact < 0 || sinceImpact > WormholeImpactLevers.FLASH_TICKS) return 0.0f;
        double decay = 1.0 - sinceImpact / WormholeImpactLevers.FLASH_TICKS;
        return (float) (WormholeImpactLevers.FLASH_MAX_ALPHA * decay * decay);
    }

    /**
     * 1 within {@link WormholeImpactLevers#FULL_STRENGTH_RADIUS} of the wormhole, 0 beyond
     * {@link WormholeImpactLevers#MAX_RADIUS}, linear in between.
     */
    private float strengthAt(Vec3 point) {
        Vec3 viewer = viewerPosition();
        if (viewer == null || point == null) return 0.0f;

        double distance = viewer.distanceTo(point);
        if (distance <= WormholeImpactLevers.FULL_STRENGTH_RADIUS) return 1.0f;
        if (distance >= WormholeImpactLevers.MAX_RADIUS) return 0.0f;
        return (float) (1.0 - (distance - WormholeImpactLevers.FULL_STRENGTH_RADIUS)
                / (WormholeImpactLevers.MAX_RADIUS - WormholeImpactLevers.FULL_STRENGTH_RADIUS));
    }

    // ---- drawing ---------------------------------------------------------

    /**
     * Pink glow along the four edges. Top and bottom are vertical gradients; GuiGraphics only
     * has vertical gradients, so the sides are stacks of thin strips of decreasing opacity.
     */
    private static void drawVignette(GuiGraphics graphics, int width, int height, float alpha) {
        int depth = Math.max(1, Math.round(Math.min(width, height) * WormholeImpactLevers.VIGNETTE_DEPTH));
        int edge = argb(alpha, WormholeImpactLevers.VIGNETTE_RGB);
        int fade = TRANSPARENT | (WormholeImpactLevers.VIGNETTE_RGB & 0xFFFFFF);

        graphics.fillGradient(0, 0, width, depth, edge, fade);
        graphics.fillGradient(0, height - depth, width, height, fade, edge);

        int strip = Math.max(1, depth / VIGNETTE_SIDE_STRIPS);
        for (int i = 0; i < VIGNETTE_SIDE_STRIPS; i++) {
            float stripAlpha = alpha * (1.0f - (float) i / VIGNETTE_SIDE_STRIPS);
            int colour = argb(stripAlpha, WormholeImpactLevers.VIGNETTE_RGB);
            int x = i * strip;
            graphics.fill(x, 0, x + strip, height, colour);
            graphics.fill(width - x - strip, 0, width - x, height, colour);
        }
    }

    /**
     * Ring of dust racing outwards along the ground under the wormhole, with debris of the
     * ground's own block thrown up near the centre. Cloud particles keep their speed and slow
     * down by themselves, so one burst draws the whole expanding ring.
     */
    private static void spawnGroundWave(ClientLevel level, Vec3 origin) {
        int count = WormholeImpactLevers.GROUND_WAVE_PARTICLES;
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2 * i / count + level.random.nextDouble() * 0.1;
            double dirX = Math.cos(angle);
            double dirZ = Math.sin(angle);
            double x = origin.x + dirX * GROUND_WAVE_START_RADIUS;
            double z = origin.z + dirZ * GROUND_WAVE_START_RADIUS;

            int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(x), Mth.floor(z));
            if (origin.y - surface > WormholeImpactLevers.GROUND_WAVE_MAX_DROP) continue;

            double speed = WormholeImpactLevers.GROUND_WAVE_SPEED * (0.85 + level.random.nextDouble() * 0.3);
            level.addParticle(ParticleTypes.CLOUD, x, surface + 0.1, z,
                    dirX * speed, GROUND_WAVE_LIFT, dirZ * speed);

            BlockState ground = level.getBlockState(new BlockPos(Mth.floor(x), surface - 1, Mth.floor(z)));
            if (!ground.isAir()) {
                level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, ground), x, surface + 0.1, z,
                        dirX, GROUND_DEBRIS_LIFT, dirZ);
            }
        }
    }

    /** Packs an opacity (0 to 1) and an RGB colour into the ARGB int GuiGraphics expects. */
    private static int argb(float alpha, int rgb) {
        int a = Mth.clamp(Math.round(alpha * 255.0f), 0, 255);
        return (a << 24) | (rgb & 0xFFFFFF);
    }
}
