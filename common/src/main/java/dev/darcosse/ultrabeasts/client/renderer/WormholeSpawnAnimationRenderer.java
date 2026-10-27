package dev.darcosse.ultrabeasts.client.renderer;

import dev.darcosse.ultrabeasts.UltraBeasts;
import dev.darcosse.ultrabeasts.entity.WormholeSpawnAnimation;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer of the spawn animation entity, which draws nothing itself: everything visible
 * comes from the particles and block displays it creates. Exists because every entity type
 * needs a registered renderer. Client side only.
 */
public class WormholeSpawnAnimationRenderer extends EntityRenderer<WormholeSpawnAnimation> {

    /** Never actually sampled, since nothing is drawn; returned to satisfy the contract. */
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(UltraBeasts.MOD_ID, "textures/entity/wormhole.png");

    /** Called by the loader's renderer registration. */
    public WormholeSpawnAnimationRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(WormholeSpawnAnimation entity) {
        return TEXTURE;
    }
}
