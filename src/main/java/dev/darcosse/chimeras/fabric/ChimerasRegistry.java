package dev.darcosse.chimeras.fabric;

import net.minecraft.registry.Registry;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class ChimerasRegistry {

    public static void initialize() {
        Registry.register(
                Registries.CHUNK_GENERATOR,
                Identifier.of("cobblemon_chimeras", "chimeras_generator"),
                ChimerasChunkGenerator.CODEC
        );
    }
}