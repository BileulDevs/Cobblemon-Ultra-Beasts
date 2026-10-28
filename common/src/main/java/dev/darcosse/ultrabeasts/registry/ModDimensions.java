package dev.darcosse.ultrabeasts.registry;

import dev.darcosse.ultrabeasts.UltraBeasts;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * Keys of the mod's dimensions. The dimension itself is data-driven, defined in
 * {@code data/cobblemon_ultrabeast/dimension/ultra_space.json}.
 */
public final class ModDimensions {

    /** Key of Ultra-Space. Must match the dimension JSON's file name. */
    public static final ResourceKey<Level> ULTRA_SPACE_DIMENSION = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(UltraBeasts.MOD_ID, "ultra_space")
    );

    /** Constants only, never instantiated. */
    private ModDimensions() {
    }
}
