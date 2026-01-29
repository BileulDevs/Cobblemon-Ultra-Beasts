package dev.darcosse.ultrabeasts.fabric.registry;

import dev.darcosse.ultrabeasts.fabric.UltraBeasts;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class ModSounds {
    public static final Identifier WORMHOLE_SPAWN_ID = Identifier.of(UltraBeasts.MOD_ID, "wormhole_spawn");
    public static final Identifier WORMHOLE_ANIMATION_SPAWN_ID = Identifier.of(UltraBeasts.MOD_ID, "wormhole_animation_spawn");
    public static final Identifier WORMHOLE_AMBIENT_ID = Identifier.of(UltraBeasts.MOD_ID, "wormhole_ambient");

    public static final SoundEvent WORMHOLE_SPAWN = SoundEvent.of(WORMHOLE_SPAWN_ID);
    public static final SoundEvent WORMHOLE_ANIMATION_SPAWN = SoundEvent.of(WORMHOLE_ANIMATION_SPAWN_ID);
    public static final SoundEvent WORMHOLE_AMBIENT = SoundEvent.of(WORMHOLE_AMBIENT_ID);

    public static void initialize() {
        Registry.register(Registries.SOUND_EVENT,
                WORMHOLE_SPAWN_ID,
                WORMHOLE_SPAWN
        );
        Registry.register(Registries.SOUND_EVENT,
                WORMHOLE_ANIMATION_SPAWN_ID,
                WORMHOLE_ANIMATION_SPAWN
        );
        Registry.register(Registries.SOUND_EVENT,
                WORMHOLE_AMBIENT_ID,
                WORMHOLE_AMBIENT
        );

        UltraBeasts.LOGGER.info("Registering Sound Events for " + UltraBeasts.MOD_ID);
    }
}
