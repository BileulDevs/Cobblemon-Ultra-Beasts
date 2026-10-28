package dev.darcosse.ultrabeasts.client.cinematic;

import dev.darcosse.ultrabeasts.network.EncounterCinematicPayload;
import dev.darcosse.ultrabeasts.registry.ModDimensions;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * The arrival in Ultra-Space for a player who turned the encounter cinematic off in
 * {@code client.json}: black while the terrain arrives, then a fade to the player's view.
 * <p>
 * Stands in for the blindness the server applied up to 4.0. The server cannot know a client
 * setting, so it no longer applies any effect on arrival; without this, a player with the
 * cinematic off would watch the structure's chunks pop in.
 * <p>
 * A screen, like the cinematic, so the player does not walk off a ledge while blind. Client
 * only.
 */
public final class ArrivalFadeScreen extends Screen {

    /** Shortest black hold, even when the chunks are already there. */
    private static final long HOLD_MIN_MS = 600L;

    /** Longest black hold while waiting for chunks; past it, the fade starts anyway. */
    private static final long HOLD_MAX_MS = 5_000L;

    /** Fade from black to the player's view. */
    private static final long FADE_MS = 700L;

    private final BlockPos arrivalChunkProbe;
    private final long openedAt;
    /** When the fade began, or -1 while still holding black. */
    private long fadingAt = -1L;

    private ArrivalFadeScreen(BlockPos arrivalChunkProbe) {
        super(Component.empty());
        this.arrivalChunkProbe = arrivalChunkProbe;
        this.openedAt = Util.getMillis();
    }

    /**
     * Payload handler used instead of the cinematic: opens the fade if the player is still
     * in Ultra-Space.
     */
    public static void open(EncounterCinematicPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null
                || !minecraft.level.dimension().equals(ModDimensions.ULTRA_SPACE_DIMENSION)) {
            return;
        }
        // The chunk under the beast stands for the structure: the farthest point of interest.
        minecraft.setScreen(new ArrivalFadeScreen(BlockPos.containing(payload.beastPos())));
    }

    /** Starts the fade once the chunks are in, and closes once it is over. */
    @Override
    public void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !minecraft.level.dimension().equals(ModDimensions.ULTRA_SPACE_DIMENSION)) {
            onClose();
            return;
        }
        long now = Util.getMillis();
        if (fadingAt < 0) {
            long held = now - openedAt;
            if (held >= HOLD_MAX_MS || (held >= HOLD_MIN_MS && chunksReady(minecraft.level))) {
                fadingAt = now;
            }
        } else if (now - fadingAt >= FADE_MS) {
            onClose();
        }
    }

    /** Whether the chunk of the farthest point of interest has reached the client. */
    private boolean chunksReady(ClientLevel level) {
        var player = Minecraft.getInstance().player;
        return level.hasChunkAt(arrivalChunkProbe) && (player == null || level.hasChunkAt(player.blockPosition()));
    }

    /** Black, then fading out. */
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        float darkness = fadingAt < 0 ? 1.0F
                : 1.0F - Math.min(1.0F, (Util.getMillis() - fadingAt) / (float) FADE_MS);
        int alpha = Math.round(darkness * 255.0F);
        if (alpha > 0) {
            graphics.fill(0, 0, width, height, alpha << 24);
        }
    }

    /** Nothing: vanilla's blur would show instead of plain black. */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    /** Escape does not cut the fade short: it lasts a second or two at most. */
    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    /** The world keeps running underneath. */
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
