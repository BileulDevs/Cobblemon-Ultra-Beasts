package dev.darcosse.ultrabeasts.client.renderer;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * Render type of the portal quads. Client side only.
 * <p>
 * RenderType.create() is not public, and widening it would mean pinning an exact descriptor
 * on every version bump. entityTranslucent() is the public factory that already matches what
 * this effect needs: entity translucent shader, alpha blending, and no back-face culling
 * (precisely what separates it from entityTranslucentCull).
 * <p>
 * It does keep the lightmap and the overlay enabled, unlike the hand-built type it replaced.
 * The renderer writes full-bright light, so lighting changes nothing; the overlay value it
 * writes, however, is the red damage row (see AbstractWormholeRenderer.OVERLAY_UV).
 */
public final class ModRenderTypes {

    /** Factory only, never instantiated. */
    private ModRenderTypes() {
    }

    /** Returns the translucent, double-sided render type for the given texture. */
    public static RenderType wormhole(ResourceLocation texture) {
        return RenderType.entityTranslucent(texture);
    }
}
