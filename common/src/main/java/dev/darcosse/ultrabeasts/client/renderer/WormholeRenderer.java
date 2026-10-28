package dev.darcosse.ultrabeasts.client.renderer;

import dev.darcosse.ultrabeasts.entity.WormholeEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** Renders the inbound wormhole. All drawing lives in {@link AbstractWormholeRenderer}. */
public class WormholeRenderer extends AbstractWormholeRenderer<WormholeEntity> {

    /** Called by the loader's renderer registration. */
    public WormholeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }
}
