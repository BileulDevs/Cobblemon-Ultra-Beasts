package dev.darcosse.chimeras.fabric.registry;

import dev.darcosse.chimeras.fabric.Chimeras;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class ModSounds {
    public static final Identifier PORTAL_SPAWN_ID = Identifier.of(Chimeras.MOD_ID, "portal_spawn");
    public static final SoundEvent PORTAL_SPAWN = SoundEvent.of(PORTAL_SPAWN_ID);

    public static final Identifier PORTAL_AMBIENT_ID = Identifier.of(Chimeras.MOD_ID, "portal_ambient");
    public static final SoundEvent PORTAL_AMBIENT = SoundEvent.of(PORTAL_AMBIENT_ID);

    public static final Identifier CORE_USED_ID = Identifier.of(Chimeras.MOD_ID, "core_used");
    public static final SoundEvent CORE_USED = SoundEvent.of(CORE_USED_ID);

    public static void registerSounds() {
        Registry.register(Registries.SOUND_EVENT, PORTAL_SPAWN_ID, PORTAL_SPAWN);
        Registry.register(Registries.SOUND_EVENT, PORTAL_AMBIENT_ID, PORTAL_AMBIENT);
        Registry.register(Registries.SOUND_EVENT, CORE_USED_ID, CORE_USED);
    }
}
