package dev.darcosse.ultrabeasts.registry;

import dev.darcosse.ultrabeasts.UltraBeasts;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

/**
 * Sound event definitions shared by both loaders, which register them under these ids. The
 * ids must match the entries of {@code assets/cobblemon_ultrabeast/sounds.json}.
 */
public final class ModSounds {

    public static final ResourceLocation WORMHOLE_SPAWN_ID =
            ResourceLocation.fromNamespaceAndPath(UltraBeasts.MOD_ID, "wormhole_spawn");
    public static final ResourceLocation WORMHOLE_ANIMATION_SPAWN_ID =
            ResourceLocation.fromNamespaceAndPath(UltraBeasts.MOD_ID, "wormhole_animation_spawn");
    public static final ResourceLocation WORMHOLE_AMBIENT_ID =
            ResourceLocation.fromNamespaceAndPath(UltraBeasts.MOD_ID, "wormhole_ambient");

    public static final SoundEvent WORMHOLE_SPAWN =
            SoundEvent.createVariableRangeEvent(WORMHOLE_SPAWN_ID);
    public static final SoundEvent WORMHOLE_ANIMATION_SPAWN =
            SoundEvent.createVariableRangeEvent(WORMHOLE_ANIMATION_SPAWN_ID);
    public static final SoundEvent WORMHOLE_AMBIENT =
            SoundEvent.createVariableRangeEvent(WORMHOLE_AMBIENT_ID);

    /** Definitions only, never instantiated. */
    private ModSounds() {
    }
}
