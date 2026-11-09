package dev.darcosse.ultrabeasts.fx;

/**
 * Compile-time switches and tuning of the impact the inbound wormhole makes when it opens:
 * camera shake, screen flash, coloured vignette, ground shockwave, darkening, sound and title.
 * <p>
 * Meant for testing in game: flip a switch, rebuild, look. Every effect is independent of the
 * others. Everything here is read on the client, except {@link #IMPACT_SOUND}, which the
 * server plays: on a dedicated server, the client switches only need the client rebuilt, the
 * sound switch only the server.
 * <p>
 * The moment of the impact is chosen by the animation itself
 * ({@code WormholeSpawnAnimation.IMPACT_AT}); the build-up effects (darkening, vignette) start
 * with the animation and peak on that tick.
 */
public final class WormholeImpactLevers {

    // ---- switches --------------------------------------------------------

    /** Shakes the view of nearby players on impact. Rotation only: the player's aim is untouched. */
    public static final boolean CAMERA_SHAKE = true;

    /** White flash over the whole screen on impact. */
    public static final boolean SCREEN_FLASH = true;

    /** Pink glow on the screen edges, swelling until the impact then fading. */
    public static final boolean SCREEN_VIGNETTE = true;

    /** Ring of dust and debris racing outwards along the ground under the wormhole. */
    public static final boolean GROUND_WAVE = true;

    /** The screen darkens while the animation builds up, as if the wormhole drank the light. */
    public static final boolean DARKEN = true;

    /** Deep boom and thunder on impact. SERVER side: heard by every player in range. */
    public static final boolean IMPACT_SOUND = true;

    /** Large on-screen title on impact. */
    public static final boolean IMPACT_TITLE = true;

    // ---- reach -----------------------------------------------------------

    /** Within this distance of the wormhole, in blocks, every effect plays at full strength. */
    public static final double FULL_STRENGTH_RADIUS = 16.0;

    /**
     * Beyond this distance, in blocks, no effect plays at all; in between, they fade linearly.
     * Raising it past ~64 makes the effects play for players who cannot even see the wormhole
     * (entities are only sent within the tracking range).
     */
    public static final double MAX_RADIUS = 48.0;

    // ---- camera shake ----------------------------------------------------

    /**
     * Peak shake, in degrees of yaw. Above ~4 the view becomes hard to read and can be
     * unpleasant; below ~1 it is barely noticed.
     */
    public static final float SHAKE_MAX_DEGREES = 2.5f;

    /** Duration of the shake, in ticks. It decays quadratically over this time. */
    public static final int SHAKE_TICKS = 25;

    // ---- flash -----------------------------------------------------------

    /** Opacity of the flash at its peak, 0 to 1. At 1 the screen is fully white for a frame. */
    public static final float FLASH_MAX_ALPHA = 0.85f;

    /** Duration of the flash, in ticks. Shorter reads as a camera flash, longer as a blast. */
    public static final int FLASH_TICKS = 12;

    // ---- vignette --------------------------------------------------------

    /** Opacity of the vignette at the screen edge at its peak, 0 to 1. */
    public static final float VIGNETTE_MAX_ALPHA = 0.6f;

    /** Colour of the vignette, RGB. The pink of the wormhole's swirl. */
    public static final int VIGNETTE_RGB = 0xFF7AD9;

    /** Share of the screen's smaller side the vignette reaches inwards. Above ~0.4 it tints the centre. */
    public static final float VIGNETTE_DEPTH = 0.25f;

    /** How long before the impact the vignette starts to swell, in ticks. */
    public static final int VIGNETTE_BUILD_TICKS = 30;

    /** How long the vignette takes to fade after the impact, in ticks. */
    public static final int VIGNETTE_FADE_TICKS = 50;

    // ---- darkening -------------------------------------------------------

    /**
     * Opacity of the black veil at the impact, 0 to 1. It grows from nothing over the whole
     * build-up. Above ~0.7 the player loses sight of the animation itself.
     */
    public static final float DARKEN_MAX_ALPHA = 0.55f;

    /** How long the veil takes to lift after the impact, in ticks. */
    public static final int DARKEN_RELEASE_TICKS = 30;

    // ---- ground wave -----------------------------------------------------

    /** Dust puffs in the ring. Each costs one particle; debris add as many again. */
    public static final int GROUND_WAVE_PARTICLES = 48;

    /**
     * Initial outward speed of the dust, in blocks per tick. Cloud particles slow by 4 % per
     * tick, so the ring reaches about speed / 0.04 blocks: 0.45 gives ~11 blocks.
     */
    public static final double GROUND_WAVE_SPEED = 0.45;

    /** Ground further below the wormhole than this, in blocks, gets no wave (it floats over a cliff). */
    public static final int GROUND_WAVE_MAX_DROP = 8;

    // ---- title -----------------------------------------------------------

    /** Title fade-in, stay and fade-out, in ticks. */
    public static final int TITLE_FADE_IN_TICKS = 5;
    public static final int TITLE_STAY_TICKS = 40;
    public static final int TITLE_FADE_OUT_TICKS = 15;

    // ---- sound -----------------------------------------------------------

    /** Volume of the impact sounds. Above 1 only widens the distance they are heard at (16 blocks per unit). */
    public static final float IMPACT_SOUND_VOLUME = 4.0f;

    /** Constants only, never instantiated. */
    private WormholeImpactLevers() {
    }
}
