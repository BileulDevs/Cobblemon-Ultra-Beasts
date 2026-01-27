package dev.darcosse.ultrabeasts.fabric.registry;

import net.minecraft.sound.BlockSoundGroup;

public class ModBlockSoundGroups {
    public static final BlockSoundGroup PORTAL_SOUNDS = new BlockSoundGroup(
            1.0F, 1.0F,
            ModSounds.PORTAL_SPAWN,
            ModSounds.PORTAL_AMBIENT,
            ModSounds.PORTAL_SPAWN,
            ModSounds.PORTAL_SPAWN,
            ModSounds.PORTAL_AMBIENT
    );
}