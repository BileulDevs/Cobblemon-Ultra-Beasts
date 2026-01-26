package dev.darcosse.chimeras.fabric.registry;

import dev.darcosse.chimeras.fabric.Chimeras;
import dev.darcosse.chimeras.fabric.ChimerasChunkGenerator;
import net.minecraft.registry.Registry;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class ChimerasRegistry {

    public static void initialize() {
        Registry.register(
                Registries.CHUNK_GENERATOR,
                Identifier.of(Chimeras.MOD_ID, "chimeras_generator"),
                ChimerasChunkGenerator.CODEC
        );
    }
}