package dev.darcosse.ultrabeasts.mixin.client;

import dev.darcosse.ultrabeasts.client.cinematic.EncounterCamera;
import dev.darcosse.ultrabeasts.client.fx.WormholeImpactEffects;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets the encounter cinematic film Ultra-Space with the game's own camera: once vanilla has
 * placed the camera at the player's eyes, a cinematic under way moves it to its shot
 * ({@link EncounterCamera}). Same technique as the Cobblemon Safari flight cinematic.
 * <p>
 * Also adds the wormhole impact's shake ({@link WormholeImpactEffects}) to the view.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow
    protected abstract void setPosition(Vec3 position);

    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    /**
     * Overrides the camera during a shot, or shakes it after a wormhole impact. {@code require = 0}: if another mod rewrote this
     * method, the cinematic is simply seen from the player's eyes instead of the game
     * refusing to start.
     */
    @Inject(method = "setup", at = @At("TAIL"), require = 0)
    private void ultrabeasts$encounterShot(BlockGetter level, Entity entity, boolean detached,
                                           boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {
        EncounterCamera.Frame frame = EncounterCamera.frame(partialTick);
        if (frame != null) {
            // Rotation first, as vanilla does: a mod hooking setPosition may read it.
            setRotation(frame.yaw(), frame.pitch());
            setPosition(frame.position());
            // A shot owns the camera: no shake on top of it.
            return;
        }

        float[] shake = WormholeImpactEffects.cameraShake(partialTick);
        if (shake != null) {
            // Only the camera turns, not the player: aim and movement are unaffected, and
            // nothing has to be undone when the shake ends.
            Camera self = (Camera) (Object) this;
            setRotation(self.getYRot() + shake[0], self.getXRot() + shake[1]);
        }
    }
}
