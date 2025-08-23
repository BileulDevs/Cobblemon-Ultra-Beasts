package dev.darcosse.chimeras.fabric.entity;

import dev.darcosse.chimeras.fabric.Chimeras;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModEntities {
    public static final EntityType<WormholeEntity> WORMHOLE = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(Chimeras.MOD_ID, "wormhole"),
            FabricEntityTypeBuilder.create(SpawnGroup.MISC, WormholeEntity::new)
                    .dimensions(EntityDimensions.fixed(2.0f, 2.0f))
                    .build()
    );

    public static void init() {

    }
}

