package dev.darcosse.chimeras.fabric;

import dev.darcosse.chimeras.fabric.entity.ModEntities;
import dev.darcosse.chimeras.fabric.renderer.WormholeRenderer;
import dev.darcosse.chimeras.fabric.renderer.WormholeSpawnAnimationRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class ChimerasClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.WORMHOLE, WormholeRenderer::new);
        EntityRendererRegistry.register(ModEntities.WORMHOLE_ANIMATION, WormholeSpawnAnimationRenderer::new);
    }
}