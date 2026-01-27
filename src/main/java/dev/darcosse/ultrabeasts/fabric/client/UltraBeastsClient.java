package dev.darcosse.ultrabeasts.fabric.client;

import dev.darcosse.ultrabeasts.fabric.client.particle.WormholeParticleFactory;
import dev.darcosse.ultrabeasts.fabric.registry.ModEntities;
import dev.darcosse.ultrabeasts.fabric.registry.ModParticles;
import dev.darcosse.ultrabeasts.fabric.renderer.WormholeRenderer;
import dev.darcosse.ultrabeasts.fabric.renderer.WormholeSpawnAnimationRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class UltraBeastsClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.WORMHOLE, WormholeRenderer::new);
        EntityRendererRegistry.register(ModEntities.WORMHOLE_ANIMATION, WormholeSpawnAnimationRenderer::new);
        ParticleFactoryRegistry.getInstance().register(ModParticles.WORMHOLE, WormholeParticleFactory::new);
    }
}