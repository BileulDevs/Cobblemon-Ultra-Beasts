package dev.darcosse.ultrabeasts.client.cinematic;

import com.cobblemon.mod.common.client.entity.PokemonClientDelegate;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.mojang.blaze3d.platform.InputConstants;
import dev.darcosse.ultrabeasts.UltraBeasts;
import dev.darcosse.ultrabeasts.dimension.UltraSpaceStructureManager.CinematicStyle;
import dev.darcosse.ultrabeasts.network.EncounterCinematicPayload;
import dev.darcosse.ultrabeasts.registry.ModDimensions;
import dev.darcosse.ultrabeasts.registry.ModSounds;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.List;

/**
 * The encounter cinematic played on arrival in Ultra-Space, between two black bands:
 * <ol>
 *   <li>black, until the structure's chunks have reached the client;</li>
 *   <li>the structure: a fly-over for open layouts, a walkthrough at head height for
 *   enclosed ones (see {@link CinematicStyle}), with the dimension's name;</li>
 *   <li>a cut through black to a close-up dollying toward the Ultra Beast;</li>
 *   <li>the cry: the beast roars, plays its cry animation if it has one, and the camera
 *   zooms on its head with a shake while its name appears;</li>
 *   <li>a fade back to the player's own eyes.</li>
 * </ol>
 * The game camera films the world behind this transparent screen ({@link EncounterCamera});
 * the HUD and the hand are hidden meanwhile. Space, Enter or Escape skip to the fade out.
 * <p>
 * Runs entirely on the client clock: the server only sends the opening order. A screen
 * rather than an overlay so the player cannot walk or open their inventory meanwhile.
 * <p>
 * Client only.
 */
public final class EncounterCinematicScreen extends Screen implements EncounterCamera.Director {

    // ---- timeline, in milliseconds ---------------------------------------

    /**
     * Shortest black hold before the first shot, even when the chunks are already there: the
     * teleport just happened and the world needs a moment to render its first frames.
     */
    private static final long HOLD_MIN_MS = 800L;

    /**
     * Longest black hold while waiting for chunks. Past it the cinematic plays anyway, on
     * whatever has arrived, rather than leaving the player in the dark.
     */
    private static final long HOLD_MAX_MS = 5_000L;

    /** Fade from black into the first shot. */
    private static final long FADE_IN_MS = 800L;

    /**
     * Length of the structure shot (fly-over or walkthrough), fades included. Shorter makes
     * the fly-over's 120° turn feel hurried.
     */
    private static final long INTRO_MS = 6_500L;

    /** Fade to black and back between the structure shot and the close-up (each way). */
    private static final long SHOT_FADE_MS = 450L;

    /** Length of the dolly toward the beast, from the cut to the cry. */
    private static final long DOLLY_MS = 3_500L;

    /** Blend from the dolly to the head shot, once the cry starts. Longer loses the punch. */
    private static final long HEAD_ZOOM_MS = 450L;

    /** How long the shake lasts after the cry starts. */
    private static final long SHAKE_MS = 700L;

    /** How long the head shot holds after the cry starts, before the fade out. */
    private static final long CRY_HOLD_MS = 2_400L;

    /** Delay before the heading appears after the cut, and the fade-in time of all words. */
    private static final long HEADING_DELAY_MS = 600L;
    private static final long WORDS_FADE_MS = 400L;

    /** Delay between the cry and the beast's name, so the name lands on the roar's peak. */
    private static final long NAME_DELAY_MS = 250L;

    /** Fade out: black covers the shot, then lifts off the player's own view. */
    private static final long FADE_OUT_MS = 800L;

    /** When the cry starts, from the start of the structure shot. */
    private static final long CRY_AT_MS = INTRO_MS + DOLLY_MS;

    /** When the cinematic ends by itself, from the start of the structure shot. */
    private static final long END_AT_MS = CRY_AT_MS + CRY_HOLD_MS;

    // ---- cry -------------------------------------------------------------

    /**
     * Roar pitch for the smallest and the largest beast: big ones growl lower. The bounds
     * keep the sound recognisable; outside 0.5 to 2 Minecraft clamps them anyway.
     */
    private static final float ROAR_PITCH_SMALL = 1.25F;
    private static final float ROAR_PITCH_LARGE = 0.7F;

    /** Beast sizes, in blocks, mapped to the two pitches above (poipole to guzzlord). */
    private static final double ROAR_SIZE_SMALL = 1.5;
    private static final double ROAR_SIZE_LARGE = 14.0;

    // ---- layout ----------------------------------------------------------

    /** Height of each black band, as a share of the screen height. */
    private static final float BAND_SHARE = 0.12F;

    /** Scale of the beast's name. 2 reads as a title without overflowing the band. */
    private static final float SPECIES_SCALE = 2.0F;

    /** Gap between the heading and the beast's name, in GUI pixels. */
    private static final int LINE_GAP = 4;

    private static final int BLACK = 0xFF000000;
    private static final int HEADING_COLOR = 0xD8D8D8;
    private static final int SPECIES_COLOR = 0xFFB000;
    private static final int PLACE_COLOR = 0xE0C8FF;
    private static final int HINT_COLOR = 0x909090;

    /**
     * Below this alpha, text is not drawn: the font treats an alpha under 4 as opaque, which
     * would flash the text at full strength at the start of every fade.
     */
    private static final int MIN_TEXT_ALPHA = 5;

    // ---- collision -------------------------------------------------------

    /** Radius around the spawn point searched for the beast, in blocks. It does not move. */
    private static final double BEAST_SEARCH_RADIUS = 4.0;

    /** Gap kept between the camera and a wall it would otherwise touch, in blocks. */
    private static final double WALL_MARGIN = 0.6;

    /**
     * Least share of a shot's distance kept when a wall stands right behind the camera.
     * Below it the camera would sit inside the beast's model.
     */
    private static final double MIN_FREE_FRACTION = 0.35;

    /** Height of a standing player's eyes, where the walkthrough's collision rays start. */
    private static final double PLAYER_EYE_HEIGHT = 1.62;

    private static final Component SKIP_HINT = Component.translatable("cinematic.cobblemon_ultrabeast.skip");
    private static final Component ENCOUNTER_HEADING =
            Component.translatable("cinematic.cobblemon_ultrabeast.encounter.heading");
    private static final Component PLACE_NAME = Component.translatable("dimension.cobblemon_ultrabeast.ultra_space");

    private final EncounterCinematicPayload payload;
    private final EncounterPlan plan;
    private final Component speciesName;
    /** Where the player stood when the cinematic opened; they cannot move meanwhile. */
    private final Vec3 viewerPos;

    private final long openedAt;
    /** When the structure shot began, or -1 while still holding black. */
    private long rollingAt = -1L;
    /** When the fade out began, or -1 while still playing. */
    private long closingAt = -1L;
    /** Darkness when the fade out began, so it starts where the shot stood. */
    private float closingFrom;

    /** The player's own "hide GUI" (F1) setting, put back when the cinematic ends. */
    private final boolean previousHideGui;
    private boolean hidingGui;

    private SimpleSoundInstance introSound;
    private SimpleSoundInstance roarSound;

    // Walkthrough path, resolved when the structure shot starts (chunks are in by then).
    private Vec3 walkStart;
    private Vec3 walkEnd;

    // Close-up, resolved on its first frame: the beast may reach the client late.
    private boolean closeUpResolved;
    private double beastSize = EncounterPlan.FALLBACK_BEAST_SIZE;
    private double beastWidth = EncounterPlan.FALLBACK_BEAST_SIZE;
    private double closeUpFree = 1.0;
    private double headFree = 1.0;
    private PokemonEntity beast;

    /** Whether the cry was triggered: it must happen once, on the first frame past its time. */
    private boolean cried;

    private EncounterCinematicScreen(EncounterCinematicPayload payload, Vec3 viewerPos) {
        super(Component.translatable("cinematic.cobblemon_ultrabeast.encounter.heading"));
        this.payload = payload;
        this.viewerPos = viewerPos;
        this.plan = new EncounterPlan(payload.structureMin(), payload.structureMax(), payload.beastPos(), viewerPos);
        this.speciesName = Component.translatable(payload.speciesKey());
        this.openedAt = Util.getMillis();

        Minecraft minecraft = Minecraft.getInstance();
        this.previousHideGui = minecraft.options.hideGui;
        // Hides the HUD, the hand and the block outline; screens still draw.
        minecraft.options.hideGui = true;
        this.hidingGui = true;
    }

    /**
     * Payload handler, installed through {@code CinematicClientBridge}: opens the cinematic
     * if the player is still in Ultra-Space.
     */
    public static void open(EncounterCinematicPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null
                || !minecraft.level.dimension().equals(ModDimensions.ULTRA_SPACE_DIMENSION)) {
            // Left already (another wormhole, a command): the shots would film the wrong world.
            return;
        }
        if (minecraft.screen instanceof EncounterCinematicScreen old) {
            // Before the new screen reads the F1 setting: the old one still hides the HUD.
            old.giveViewBack();
        }
        EncounterCinematicScreen screen = new EncounterCinematicScreen(payload, minecraft.player.position());
        minecraft.setScreen(screen);
        EncounterCamera.start(screen);
    }

    // ---- timeline --------------------------------------------------------

    /** Starts the structure shot once the chunks are in, and ends the cinematic on time. */
    private void advance(long now) {
        if (rollingAt < 0) {
            long held = now - openedAt;
            if (held >= HOLD_MAX_MS || (held >= HOLD_MIN_MS && chunksReady())) {
                rollingAt = now;
                resolveWalkthrough();
                introSound = playUi(ModSounds.ENCOUNTER_INTRO, 1.0F);
            }
            return;
        }
        if (closingAt < 0 && now - rollingAt >= END_AT_MS) {
            beginClose(now);
        }
    }

    /** Whether the structure's centre and the beast's chunk have reached the client. */
    private boolean chunksReady() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return false;
        }
        BlockPos min = payload.structureMin();
        BlockPos max = payload.structureMax();
        BlockPos centre = new BlockPos((min.getX() + max.getX()) / 2, min.getY(), (min.getZ() + max.getZ()) / 2);
        return level.hasChunkAt(centre) && level.hasChunkAt(BlockPos.containing(payload.beastPos()));
    }

    /** Plays a sound unattenuated: the camera is far from where it would be placed. */
    private static SimpleSoundInstance playUi(net.minecraft.sounds.SoundEvent sound, float pitch) {
        SimpleSoundInstance instance = SimpleSoundInstance.forUI(sound, pitch, 1.0F);
        Minecraft.getInstance().getSoundManager().play(instance);
        return instance;
    }

    /** Starts the fade out; the screen closes once it is over. */
    private void beginClose(long now) {
        if (closingAt < 0) {
            closingFrom = darkness(now);
            closingAt = now;
        }
    }

    /** How dark the veil over the shot is, 0 to 1, before the fade out. */
    private float darkness(long now) {
        if (rollingAt < 0) {
            return 1.0F;
        }
        long t = now - rollingAt;
        if (t < FADE_IN_MS) {
            return 1.0F - t / (float) FADE_IN_MS;
        }
        if (t >= INTRO_MS - SHOT_FADE_MS && t < INTRO_MS) {
            return (t - (INTRO_MS - SHOT_FADE_MS)) / (float) SHOT_FADE_MS;
        }
        if (t >= INTRO_MS && t < INTRO_MS + SHOT_FADE_MS) {
            return 1.0F - (t - INTRO_MS) / (float) SHOT_FADE_MS;
        }
        return 0.0F;
    }

    /** Progress of the fade out, 0 to 1, or -1 while still playing. */
    private float closingProgress(long now) {
        return closingAt < 0 ? -1.0F : Math.min(1.0F, (now - closingAt) / (float) FADE_OUT_MS);
    }

    // ---- camera ----------------------------------------------------------

    /**
     * This frame's shot. Null during the black hold and the second half of the fade out,
     * where the player's own view is shown (under black, so the swap is not seen).
     */
    @Override
    public EncounterCamera.Shot shot(float partialTick) {
        long now = Util.getMillis();
        if (rollingAt < 0 || closingProgress(now) >= 0.5F) {
            return null;
        }
        long t = now - rollingAt;
        if (t < INTRO_MS) {
            double progress = t / (double) INTRO_MS;
            if (payload.style() == CinematicStyle.WALKTHROUGH && walkStart != null) {
                return EncounterPlan.walkthrough(progress, walkStart, walkEnd, beastAim(partialTick));
            }
            return plan.orbit(progress);
        }

        resolveCloseUp();
        Vec3 aim = beastAim(partialTick);
        double dolly = Math.min(1.0, (t - INTRO_MS) / (double) DOLLY_MS);
        EncounterCamera.Shot closeUp = EncounterPlan.closeUp(dolly, aim, viewerPos, beastSize, closeUpFree);
        if (t < CRY_AT_MS) {
            return closeUp;
        }

        if (!cried) {
            cried = true;
            cry();
        }
        long sinceCry = t - CRY_AT_MS;
        return EncounterPlan.headZoom(
                sinceCry / (double) HEAD_ZOOM_MS,
                sinceCry / (double) SHAKE_MS,
                sinceCry / 1000.0,
                closeUp, beastHead(partialTick), viewerPos, beastWidth, headFree);
    }

    /** The close-up's aim point: the beast if it is on the client, its spawn point otherwise. */
    private Vec3 beastAim(float partialTick) {
        if (beast != null && !beast.isRemoved()) {
            return EncounterPlan.aimPoint(beast.getPosition(partialTick), beast.getBbHeight());
        }
        return EncounterPlan.aimPoint(payload.beastPos(), beastSize);
    }

    /**
     * The beast's head, taken as its eyes. The models declare no head locator, and Cobblemon
     * places the eyes of these species at 85% of their height (none sets its own eye height),
     * which lands on or near the head for every one of them.
     */
    private Vec3 beastHead(float partialTick) {
        if (beast != null && !beast.isRemoved()) {
            return beast.getEyePosition(partialTick);
        }
        return payload.beastPos().add(0.0, beastSize * 0.85, 0.0);
    }

    /** The roar, the cry animation when the model has one, and nothing else on the server. */
    private void cry() {
        double share = Mth.clamp((beastSize - ROAR_SIZE_SMALL) / (ROAR_SIZE_LARGE - ROAR_SIZE_SMALL), 0.0, 1.0);
        float pitch = (float) Mth.lerp(share, ROAR_PITCH_SMALL, ROAR_PITCH_LARGE);
        roarSound = playUi(ModSounds.ENCOUNTER_ROAR, pitch);

        // Client side only: Cobblemon's server-side cry() would play it for every nearby
        // player and goes through the network for nothing. Only celesteela and xurkitree
        // define a cry animation; for the others this does nothing.
        if (beast != null && !beast.isRemoved() && beast.getDelegate() instanceof PokemonClientDelegate delegate) {
            delegate.cry();
        }
    }

    /**
     * Finds the beast for the walkthrough's aim and lays out its path. The glide starts just
     * behind and above the arrival point and stops short of the beast, both pulled back
     * from any wall in the way: a cave's ceiling or bend shortens the shot, never films
     * the inside of a block.
     */
    private void resolveWalkthrough() {
        if (payload.style() != CinematicStyle.WALKTHROUGH) {
            return;
        }
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        findBeast(level);

        Vec3 dir = EncounterPlan.horizontalDirection(viewerPos, payload.beastPos());
        Vec3 eye = viewerPos.add(0.0, PLAYER_EYE_HEIGHT, 0.0);

        Vec3 raised = clipTowards(level, eye, viewerPos.add(0.0, EncounterPlan.WALK_EYE_HEIGHT, 0.0));
        walkStart = clipTowards(level, raised, raised.subtract(dir.scale(EncounterPlan.WALK_BACK)));

        double reach = Math.sqrt(Math.pow(payload.beastPos().x - viewerPos.x, 2)
                + Math.pow(payload.beastPos().z - viewerPos.z, 2)) * EncounterPlan.WALK_SHARE;
        walkEnd = clipTowards(level, raised, raised.add(dir.scale(reach)));
    }

    /**
     * Finds the beast and checks for walls on the camera's paths, once. Done on the first
     * close-up frame rather than at opening: the beast reaches the client a few ticks after
     * the structure, and the structure shot leaves it the time to.
     */
    private void resolveCloseUp() {
        if (closeUpResolved) {
            return;
        }
        closeUpResolved = true;

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        findBeast(level);
        if (beast == null) {
            UltraBeasts.LOGGER.warn("Encounter cinematic: Ultra Beast not found near {}, filming its spawn point.",
                    payload.beastPos());
        }

        Vec3 aim = beastAim(1.0F);
        closeUpFree = freeFraction(level, aim, EncounterPlan.closeUpStart(aim, viewerPos, beastSize));
        Vec3 head = beastHead(1.0F);
        headFree = freeFraction(level, head, EncounterPlan.headStart(head, viewerPos, beastWidth));
    }

    /** Looks up the beast near its spawn point, keeping the nearest Pokémon found. */
    private void findBeast(ClientLevel level) {
        if (beast != null && !beast.isRemoved()) {
            return;
        }
        Vec3 spawn = payload.beastPos();
        List<PokemonEntity> found = level.getEntitiesOfClass(PokemonEntity.class,
                new AABB(spawn, spawn).inflate(BEAST_SEARCH_RADIUS));
        PokemonEntity nearest = null;
        double best = Double.MAX_VALUE;
        for (PokemonEntity candidate : found) {
            double distance = candidate.distanceToSqr(spawn);
            if (distance < best) {
                best = distance;
                nearest = candidate;
            }
        }
        beast = nearest;
        if (beast != null) {
            beastSize = Math.max(beast.getBbWidth(), beast.getBbHeight());
            beastWidth = beast.getBbWidth();
        }
    }

    /**
     * Share of the segment from {@code from} to {@code to} that is free of blocks, minus a
     * margin, never below {@link #MIN_FREE_FRACTION}.
     */
    private static double freeFraction(ClientLevel level, Vec3 from, Vec3 to) {
        BlockHitResult hit = clip(level, from, to);
        if (hit.getType() == HitResult.Type.MISS) {
            return 1.0;
        }
        double length = from.distanceTo(to);
        return Math.max(MIN_FREE_FRACTION, (from.distanceTo(hit.getLocation()) - WALL_MARGIN) / length);
    }

    /** The point of the segment where the camera may stand: {@code to}, or short of a wall. */
    private static Vec3 clipTowards(ClientLevel level, Vec3 from, Vec3 to) {
        BlockHitResult hit = clip(level, from, to);
        if (hit.getType() == HitResult.Type.MISS) {
            return to;
        }
        double length = from.distanceTo(to);
        double free = Math.max(0.0, from.distanceTo(hit.getLocation()) - WALL_MARGIN);
        return from.lerp(to, length < 1.0E-6 ? 0.0 : free / length);
    }

    /** A ray against block shapes as they are drawn: glass and leaves stop the camera too. */
    private static BlockHitResult clip(ClientLevel level, Vec3 from, Vec3 to) {
        return level.clip(new ClipContext(from, to, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE,
                CollisionContext.empty()));
    }

    // ---- input and lifecycle ---------------------------------------------

    /** Space, Enter or Escape skip to the fade out. Every other key is swallowed. */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputConstants.KEY_SPACE || keyCode == InputConstants.KEY_RETURN
                || keyCode == InputConstants.KEY_NUMPADENTER || keyCode == InputConstants.KEY_ESCAPE) {
            beginClose(Util.getMillis());
        }
        return true;
    }

    /** Escape skips rather than closing at once: the cut would show the swap of views. */
    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    /** The world keeps running: in single player, pausing would freeze the shots. */
    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Closes once the fade out is over, or as soon as the player is no longer in Ultra-Space. */
    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !mc.level.dimension().equals(ModDimensions.ULTRA_SPACE_DIMENSION)) {
            onClose();
            return;
        }
        if (closingProgress(Util.getMillis()) >= 1.0F) {
            onClose();
        }
    }

    /** Puts the camera, the HUD and the silence back, however the screen goes. */
    @Override
    public void removed() {
        giveViewBack();
        super.removed();
    }

    /**
     * Hands the camera back, restores the F1 setting and stops the intro. Idempotent. The
     * roar is left to finish: cutting it on a skip sounds like a glitch.
     */
    private void giveViewBack() {
        EncounterCamera.stop();
        if (hidingGui) {
            Minecraft.getInstance().options.hideGui = previousHideGui;
            hidingGui = false;
        }
        if (introSound != null) {
            Minecraft.getInstance().getSoundManager().stop(introSound);
            introSound = null;
        }
    }

    // ---- drawing ---------------------------------------------------------

    /** Nothing: vanilla's blur would cover the shot. Everything is drawn in {@link #render}. */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    /** The bands, the words, and the veil over all of it. */
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        long now = Util.getMillis();
        advance(now);

        float closing = closingProgress(now);
        if (closing >= 0.5F) {
            // Second half of the fade out: lifting off the player's own view.
            drawVeil(graphics, (1.0F - closing) * 2.0F);
            return;
        }

        int band = Math.round(height * BAND_SHARE);
        graphics.fill(0, 0, width, band, BLACK);
        graphics.fill(0, height - band, width, height, BLACK);

        float veil = closing < 0 ? darkness(now) : closingFrom + (1.0F - closingFrom) * closing * 2.0F;
        drawVeil(graphics, veil);

        // Words stay above the veil, so they read through the fades; the fade out hides them.
        float wordsAlpha = closing < 0 ? 1.0F : 1.0F - closing * 2.0F;
        drawText(graphics, SKIP_HINT, width / 2, (band - font.lineHeight) / 2, HINT_COLOR, wordsAlpha, 1.0F);
        drawWords(graphics, now, band, wordsAlpha);
    }

    /**
     * The place name during the structure shot; during the close-up, the heading, then the
     * beast's name once it cries.
     */
    private void drawWords(GuiGraphics graphics, long now, int band, float alpha) {
        if (rollingAt < 0) {
            return;
        }
        long t = now - rollingAt;
        int lineHeight = font.lineHeight;
        if (t < INTRO_MS - SHOT_FADE_MS) {
            float fade = Math.min(1.0F, t / (float) FADE_IN_MS);
            int top = height - band + (band - lineHeight) / 2;
            drawText(graphics, PLACE_NAME, width / 2, top, PLACE_COLOR, alpha * fade, 1.0F);
            return;
        }
        long sinceHeading = t - INTRO_MS - HEADING_DELAY_MS;
        if (sinceHeading < 0) {
            return;
        }
        int speciesHeight = Math.round(lineHeight * SPECIES_SCALE);
        int top = height - band + (band - lineHeight - LINE_GAP - speciesHeight) / 2;
        drawText(graphics, ENCOUNTER_HEADING, width / 2, top, HEADING_COLOR,
                alpha * Math.min(1.0F, sinceHeading / (float) WORDS_FADE_MS), 1.0F);

        long sinceName = t - CRY_AT_MS - NAME_DELAY_MS;
        if (sinceName >= 0) {
            drawText(graphics, speciesName, width / 2, top + lineHeight + LINE_GAP, SPECIES_COLOR,
                    alpha * Math.min(1.0F, sinceName / (float) WORDS_FADE_MS), SPECIES_SCALE);
        }
    }

    /** Centred text at an alpha and a scale; skipped when nearly transparent. */
    private void drawText(GuiGraphics graphics, Component text, int centreX, int top, int rgb, float alpha,
                          float scale) {
        int a = Math.round(Math.max(0.0F, Math.min(1.0F, alpha)) * 255.0F);
        if (a < MIN_TEXT_ALPHA) {
            return;
        }
        graphics.pose().pushPose();
        graphics.pose().translate(centreX, top, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawCenteredString(font, text, 0, 0, (a << 24) | rgb);
        graphics.pose().popPose();
    }

    /** Black over everything, at a darkness from 0 to 1. */
    private void drawVeil(GuiGraphics graphics, float darkness) {
        int alpha = Math.round(Math.max(0.0F, Math.min(1.0F, darkness)) * 255.0F);
        if (alpha > 0) {
            graphics.fill(0, 0, width, height, alpha << 24);
        }
    }
}
