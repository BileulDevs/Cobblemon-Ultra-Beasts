package dev.darcosse.ultrabeasts.fabric.registry;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import static dev.darcosse.ultrabeasts.fabric.UltraBeasts.MOD_ID;

public class ModDimensions {
    public static final RegistryKey<World> ULTRA_SPACE_DIMENSION = RegistryKey.of(
            RegistryKeys.WORLD,
            Identifier.of(MOD_ID, "ultra_space")
    );
}
