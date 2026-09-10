package dev.darcosse.ultrabeasts.registry;

import dev.darcosse.ultrabeasts.UltraBeasts;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public class ModDimensions {
    public static final ResourceKey<Level> ULTRA_SPACE_DIMENSION = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(UltraBeasts.MOD_ID, "ultra_space")
    );
}
