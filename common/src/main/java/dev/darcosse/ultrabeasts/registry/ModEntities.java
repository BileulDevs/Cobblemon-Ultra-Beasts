package dev.darcosse.ultrabeasts.registry;

import dev.darcosse.ultrabeasts.entity.ReturnWormholeEntity;
import dev.darcosse.ultrabeasts.entity.WormholeEntity;
import dev.darcosse.ultrabeasts.entity.WormholeSpawnAnimation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/**
 * Entity type definitions shared by both loaders. Each loader registers them under the ids
 * below, then publishes the registered types into the static fields. The ids are part of
 * the save format: renaming one drops the matching entities from existing worlds.
 */
public final class ModEntities {

    public static final String WORMHOLE_ID = "wormhole";
    public static final String WORMHOLE_ANIMATION_ID = "wormhole_animation";
    public static final String RETURN_WORMHOLE_ID = "return_wormhole";

    /** Filled by the loader module during registration; null before that. */
    public static EntityType<WormholeEntity> WORMHOLE;
    public static EntityType<WormholeSpawnAnimation> WORMHOLE_ANIMATION;
    public static EntityType<ReturnWormholeEntity> RETURN_WORMHOLE;

    /** Definitions only, never instantiated. */
    private ModEntities() {
    }

    /** Builds the inbound wormhole type. 1x1 hitbox: small enough to need walking right into it. */
    public static EntityType<WormholeEntity> createWormhole() {
        return EntityType.Builder
                .<WormholeEntity>of(WormholeEntity::new, MobCategory.MISC)
                .sized(1.0f, 1.0f)
                .build(WORMHOLE_ID);
    }

    /** Builds the spawn animation type. Never rendered, so its size only matters for culling. */
    public static EntityType<WormholeSpawnAnimation> createWormholeAnimation() {
        return EntityType.Builder
                .<WormholeSpawnAnimation>of(WormholeSpawnAnimation::new, MobCategory.MISC)
                .sized(2.0f, 2.0f)
                .build(WORMHOLE_ANIMATION_ID);
    }

    /** Builds the exit portal type, same footprint as the inbound wormhole. */
    public static EntityType<ReturnWormholeEntity> createReturnWormhole() {
        return EntityType.Builder
                .<ReturnWormholeEntity>of(ReturnWormholeEntity::new, MobCategory.MISC)
                .sized(1.0f, 1.0f)
                .build(RETURN_WORMHOLE_ID);
    }
}
