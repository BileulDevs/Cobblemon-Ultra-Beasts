package dev.darcosse.ultrabeasts.registry;

import dev.darcosse.ultrabeasts.entity.ReturnWormholeEntity;
import dev.darcosse.ultrabeasts.entity.WormholeEntity;
import dev.darcosse.ultrabeasts.entity.WormholeSpawnAnimation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/**
 * Entity type definitions. The actual registration is done by each loader,
 * which then fills the static fields below.
 */
public final class ModEntities {

    public static final String WORMHOLE_ID = "wormhole";
    public static final String WORMHOLE_ANIMATION_ID = "wormhole_animation";
    public static final String RETURN_WORMHOLE_ID = "return_wormhole";

    /** Filled by the platform module during registration. */
    public static EntityType<WormholeEntity> WORMHOLE;
    public static EntityType<WormholeSpawnAnimation> WORMHOLE_ANIMATION;
    public static EntityType<ReturnWormholeEntity> RETURN_WORMHOLE;

    private ModEntities() {
    }

    public static EntityType<WormholeEntity> createWormhole() {
        return EntityType.Builder
                .<WormholeEntity>of(WormholeEntity::new, MobCategory.MISC)
                .sized(1.0f, 1.0f)
                .build(WORMHOLE_ID);
    }

    public static EntityType<WormholeSpawnAnimation> createWormholeAnimation() {
        return EntityType.Builder
                .<WormholeSpawnAnimation>of(WormholeSpawnAnimation::new, MobCategory.MISC)
                .sized(2.0f, 2.0f)
                .build(WORMHOLE_ANIMATION_ID);
    }

    public static EntityType<ReturnWormholeEntity> createReturnWormhole() {
        return EntityType.Builder
                .<ReturnWormholeEntity>of(ReturnWormholeEntity::new, MobCategory.MISC)
                .sized(1.0f, 1.0f)
                .build(RETURN_WORMHOLE_ID);
    }
}
