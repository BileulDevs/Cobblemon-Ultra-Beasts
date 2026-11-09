package dev.darcosse.ultrabeasts.mixin.client;

import dev.darcosse.ultrabeasts.client.fx.WormholeImpactEffects;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws the wormhole's screen effects (darkening, vignette, flash) over the HUD.
 * <p>
 * A mixin rather than each loader's HUD event (Fabric's HudRenderCallback, NeoForge's GUI
 * layers): one hook in common code, same draw order on both loaders.
 */
@Mixin(Gui.class)
public abstract class GuiMixin {

    /**
     * Runs after the whole HUD, so the flash covers it. Left at the default
     * {@code require = 1} while the effects are being tested: if the target ever moves, the
     * game refuses to start with a clear mixin error instead of silently showing no overlay.
     * Set {@code require = 0} for a release, like {@link CameraMixin}.
     */
    @Inject(method = "render", at = @At("TAIL"))
    private void ultrabeasts$wormholeOverlay(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        // false: keep counting while the game is paused in single player, as the level clock
        // the envelopes read is frozen anyway.
        WormholeImpactEffects.renderOverlay(graphics, deltaTracker.getGameTimeDeltaPartialTick(false));
    }
}
