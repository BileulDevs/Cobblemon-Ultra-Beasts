package dev.darcosse.ultrabeasts.fabric.registry;

import dev.darcosse.ultrabeasts.fabric.UltraBeasts;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModParticles {

    public static final SimpleParticleType WORMHOLE = FabricParticleTypes.simple();

    public static void register() {
        Registry.register(
                Registries.PARTICLE_TYPE,
                Identifier.of(UltraBeasts.MOD_ID, "wormhole"),
                WORMHOLE
        );
    }
}