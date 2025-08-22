package dev.darcosse.chimeras.fabric.renderer;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

public class ModRenderLayers {
    public static RenderLayer wormhole(Identifier texture) {
        return RenderLayer.of("wormhole",
                VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
                VertexFormat.DrawMode.QUADS,
                256,
                RenderLayer.MultiPhaseParameters.builder()
                        .program(RenderPhase.ENTITY_TRANSLUCENT_PROGRAM) // comme translucent
                        .texture(new RenderPhase.Texture(texture, false, false))
                        .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY) // alpha blending normal
                        .cull(RenderPhase.DISABLE_CULLING) // pas de face culling
                        .lightmap(RenderPhase.DISABLE_LIGHTMAP) // ignore la lumière du monde
                        .overlay(RenderPhase.DISABLE_OVERLAY_COLOR)
                        .build(false));
    }
}