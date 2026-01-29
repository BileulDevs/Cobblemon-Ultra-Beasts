package dev.darcosse.ultrabeasts.fabric.registry;

import dev.darcosse.ultrabeasts.fabric.UltraBeasts;
import dev.darcosse.ultrabeasts.fabric.entity.ReturnWormholeEntity;
import dev.darcosse.ultrabeasts.fabric.entity.WormholeEntity;
import dev.darcosse.ultrabeasts.fabric.entity.WormholeSpawnAnimation;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public class ModEntities {

    public static final EntityType<WormholeEntity> WORMHOLE = FabricEntityTypeBuilder
            .create(SpawnGroup.MISC, WormholeEntity::new)
            .dimensions(EntityDimensions.fixed(1.0f, 1.0f))
            .build();

    public static final EntityType<WormholeSpawnAnimation> WORMHOLE_ANIMATION = FabricEntityTypeBuilder
            .create(SpawnGroup.MISC, (EntityType<WormholeSpawnAnimation> type, World world) -> new WormholeSpawnAnimation(type, world))
            .dimensions(EntityDimensions.fixed(2.0f, 2.0f))
            .build();

    public static final EntityType<ReturnWormholeEntity> RETURN_WORMHOLE = FabricEntityTypeBuilder
            .create(SpawnGroup.MISC, ReturnWormholeEntity::new)
            .dimensions(EntityDimensions.fixed(1.0f, 1.0f))
            .build();

    public static void initialize() {
        Registry.register(
                Registries.ENTITY_TYPE,
                Identifier.of(UltraBeasts.MOD_ID, "wormhole"),
                WORMHOLE
        );
        Registry.register(
                Registries.ENTITY_TYPE,
                Identifier.of(UltraBeasts.MOD_ID, "wormhole_animation"),
                WORMHOLE_ANIMATION
        );

        Registry.register(
                Registries.ENTITY_TYPE,
                Identifier.of(UltraBeasts.MOD_ID, "return_wormhole"),
                RETURN_WORMHOLE
        );

        UltraBeasts.LOGGER.info("Registering Entities for " + UltraBeasts.MOD_ID);
    }
}
