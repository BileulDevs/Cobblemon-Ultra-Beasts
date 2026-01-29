package dev.darcosse.ultrabeasts.fabric.registry;

import dev.darcosse.ultrabeasts.fabric.UltraBeasts;
import dev.darcosse.ultrabeasts.fabric.dimension.VoidChunkGenerator;
import net.minecraft.registry.Registry;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class ModGenerators {

    public static void initialize() {
        Registry.register(
                Registries.CHUNK_GENERATOR,
                Identifier.of(UltraBeasts.MOD_ID, "void_generator"),
                VoidChunkGenerator.CODEC
        );

        UltraBeasts.LOGGER.info("Registering Generators for " + UltraBeasts.MOD_ID);
    }
}