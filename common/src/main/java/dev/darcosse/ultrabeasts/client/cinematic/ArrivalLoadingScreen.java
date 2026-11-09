package dev.darcosse.ultrabeasts.client.cinematic;

import dev.darcosse.ultrabeasts.UltraBeasts;
import dev.darcosse.ultrabeasts.network.EncounterCinematicPayload;
import dev.darcosse.ultrabeasts.registry.ModDimensions;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.core.SectionPos;

import java.util.function.BooleanSupplier;

/**
 * Minecraft's own "Loading terrain" screen, kept up on arrival in Ultra-Space until the
 * encounter structure is fully drawn, then handed over to the cinematic or the plain fade.
 * <p>
 * It replaces the vanilla instance the dimension change opened, so the player sees a single,
 * familiar loading screen that simply lasts a little longer, instead of a second waiting
 * screen of the mod's own. The work itself is {@link StructurePreloader}'s: chunks, light and
 * meshes of the whole structure, whatever the camera will look at.
 * <p>
 * Client only. Cost: one {@link StructurePreloader#update()} per tick while the screen is up,
 * nothing afterwards.
 */
public final class ArrivalLoadingScreen extends ReceivingLevelScreen {

    /**
     * Longest the screen is held once the payload arrived. Kept below vanilla's own 30 s cap
     * so it is this one that ends a stuck load, with a warning in the log. Raising it lets a
     * very slow client finish the build at the cost of a longer stare at the loading screen;
     * lowering it trades that for pieces of structure popping in during the fly-over.
     */
    private static final long HOLD_MAX_MS = 15_000L;

    /** What runs once the screen closes: the cinematic or the fade. */
    private final Runnable next;
    /** Whether {@link #next} already ran: {@link #onClose()} may be reached more than once. */
    private boolean nextStarted;

    private ArrivalLoadingScreen(Readiness readiness, Runnable next) {
        // OTHER is the plain "Loading terrain" on the menu background, the one a dimension
        // change through a command or a mod shows; the portal variants would look out of place.
        super(readiness, ReceivingLevelScreen.Reason.OTHER);
        this.next = next;
    }

    /**
     * Payload handler on arrival: puts the held loading screen in place of the vanilla one,
     * or runs {@code next} straight away if the player is no longer in Ultra-Space (it then
     * gives up on its own, as it checks the dimension too).
     */
    public static void open(EncounterCinematicPayload payload, Runnable next) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        if (!minecraft.level.dimension().equals(ModDimensions.ULTRA_SPACE_DIMENSION)) {
            next.run();
            return;
        }
        // The player's position now is where the cinematic will start from: the preloader
        // orders the sections by distance to it, like the cinematic's own plan.
        StructurePreloader preloader = new StructurePreloader(payload.structureMin(), payload.structureMax(),
                payload.beastPos(), minecraft.player.position());
        minecraft.setScreen(new ArrivalLoadingScreen(new Readiness(preloader), next));
    }

    /** Closes the loading screen, then starts what comes after it, once. */
    @Override
    public void onClose() {
        super.onClose();
        if (!nextStarted) {
            nextStarted = true;
            next.run();
        }
    }

    /**
     * Tells the loading screen when it may close: the player's own chunk is in, like vanilla
     * waits for, and the structure is fully drawn. Polled once per tick by
     * {@link ReceivingLevelScreen#tick()}.
     */
    private static final class Readiness implements BooleanSupplier {

        private final StructurePreloader preloader;
        private final long openedAt = Util.getMillis();

        private Readiness(StructurePreloader preloader) {
            this.preloader = preloader;
        }

        /** Advances the preparation, and reports whether the screen may close. */
        @Override
        public boolean getAsBoolean() {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || minecraft.level == null
                    || !minecraft.level.dimension().equals(ModDimensions.ULTRA_SPACE_DIMENSION)) {
                // Left during the load: nothing to wait for here any more.
                return true;
            }
            long held = Util.getMillis() - openedAt;
            boolean playerChunk = minecraft.level.hasChunk(
                    SectionPos.blockToSectionCoord(minecraft.player.getBlockX()),
                    SectionPos.blockToSectionCoord(minecraft.player.getBlockZ()));
            // The structure is advanced even before the player's chunk is in: the two arrive
            // independently, and the build is the long part.
            boolean built = preloader.update();
            if (playerChunk && built) {
                UltraBeasts.LOGGER.info("Arrival: structure ready after {} ms ({})", held, preloader.summary());
                return true;
            }
            if (held >= HOLD_MAX_MS) {
                UltraBeasts.LOGGER.warn("Arrival: structure not ready after {} ms ({}), showing it anyway",
                        held, preloader.summary());
                return true;
            }
            return false;
        }
    }
}
