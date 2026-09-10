package dev.darcosse.ultrabeasts.client.renderer;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * RenderType.create() is not public, and widening it would mean pinning an
 * exact descriptor for every version bump. entityTranslucent() is the public
 * factory that already matches what this effect needs: entity translucent
 * shader, alpha blending, and no back-face culling (that is precisely what
 * separates it from entityTranslucentCull).
 *
 * It does leave the lightmap and overlay enabled, unlike the hand-built type,
 * but the renderers set light to full bright (0x00F000F0) and overlay to 0
 * on every vertex, so the rendered result is the same.
 */
public final class ModRenderTypes {

    private ModRenderTypes() {
    }

    public static RenderType wormhole(ResourceLocation texture) {
        return RenderType.entityTranslucent(texture);
    }
}
