package dev.darcosse.chimeras.fabric.registry;

import net.minecraft.sound.BlockSoundGroup;

public class ModBlockSoundGroups {
    public static final BlockSoundGroup PORTAL_SOUNDS = new BlockSoundGroup(
            1.0F, 1.0F,   // volume, pitch
            ModSounds.PORTAL_SPAWN, // breakSound
            ModSounds.PORTAL_AMBIENT, // stepSound
            ModSounds.PORTAL_SPAWN,    // placeSound
            ModSounds.PORTAL_SPAWN,    // hitSound
            ModSounds.PORTAL_AMBIENT  // fallSound
    );
}