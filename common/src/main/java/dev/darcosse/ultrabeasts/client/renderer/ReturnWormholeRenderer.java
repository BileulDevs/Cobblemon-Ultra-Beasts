package dev.darcosse.ultrabeasts.client.renderer;

import dev.darcosse.ultrabeasts.entity.ReturnWormholeEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** Renders the exit portal in Ultra-Space. All drawing lives in {@link AbstractWormholeRenderer}. */
public class ReturnWormholeRenderer extends AbstractWormholeRenderer<ReturnWormholeEntity> {

    /** Called by the loader's renderer registration. */
    public ReturnWormholeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }
}
