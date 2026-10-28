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
    /** Intro of the Ultra-Space encounter cinematic, played client side only. */
    public static final ResourceLocation ENCOUNTER_INTRO_ID =
            ResourceLocation.fromNamespaceAndPath(UltraBeasts.MOD_ID, "encounter_intro");
    /**
     * Roar of the Ultra Beast at the end of the encounter cinematic, client side only.
     * Cobblemon 1.8.4 ships the Ultra Beasts' cries as silent placeholders, hence a sound of
     * the mod's own. A resource pack can replace it under this id.
     */
    public static final ResourceLocation ENCOUNTER_ROAR_ID =
            ResourceLocation.fromNamespaceAndPath(UltraBeasts.MOD_ID, "encounter_roar");

    public static final SoundEvent WORMHOLE_SPAWN =
            SoundEvent.createVariableRangeEvent(WORMHOLE_SPAWN_ID);
    public static final SoundEvent WORMHOLE_ANIMATION_SPAWN =
            SoundEvent.createVariableRangeEvent(WORMHOLE_ANIMATION_SPAWN_ID);
    public static final SoundEvent WORMHOLE_AMBIENT =
            SoundEvent.createVariableRangeEvent(WORMHOLE_AMBIENT_ID);
    public static final SoundEvent ENCOUNTER_INTRO =
            SoundEvent.createVariableRangeEvent(ENCOUNTER_INTRO_ID);
    public static final SoundEvent ENCOUNTER_ROAR =
            SoundEvent.createVariableRangeEvent(ENCOUNTER_ROAR_ID);

    /** Definitions only, never instantiated. */
    private ModSounds() {
    }
}
