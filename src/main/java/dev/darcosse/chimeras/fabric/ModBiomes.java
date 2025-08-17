package dev.darcosse.chimeras.fabric;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;

public class ModBiomes {
    public static final RegistryKey<Biome> CHIMERAS_BIOME_KEY = RegistryKey.of(
            RegistryKeys.BIOME, Identifier.of("cobblemon_chimeras", "chimeras_biome")
    );

    public static void register() {
        BiomeModifications.create(Identifier.of("cobblemon_chimeras", "add_chimeras_biome"))
                .add(ModificationPhase.ADDITIONS,
                        BiomeSelectors.foundInOverworld(),
                        (biomeSelectionContext, biomeModificationContext) -> {
                        });
    }
}
