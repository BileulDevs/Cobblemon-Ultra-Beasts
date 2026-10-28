package dev.darcosse.ultrabeasts.fabric.client;

import dev.darcosse.ultrabeasts.client.UltraBeastsClient;
import dev.darcosse.ultrabeasts.client.particle.ModParticleProviders;
import dev.darcosse.ultrabeasts.client.renderer.ReturnWormholeRenderer;
import dev.darcosse.ultrabeasts.client.renderer.WormholeRenderer;
import dev.darcosse.ultrabeasts.client.renderer.WormholeSpawnAnimationRenderer;
import dev.darcosse.ultrabeasts.registry.ModEntities;
import dev.darcosse.ultrabeasts.network.CinematicClientBridge;
import dev.darcosse.ultrabeasts.network.EncounterCinematicPayload;
import dev.darcosse.ultrabeasts.registry.ModParticles;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

/** Fabric client entry point: registers entity renderers, particle providers and payload receivers. */
public class UltraBeastsFabricClient implements ClientModInitializer {

    /** Runs after the common initializer, so the entity and particle types are registered. */
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.WORMHOLE, WormholeRenderer::new);
        EntityRendererRegistry.register(ModEntities.WORMHOLE_ANIMATION, WormholeSpawnAnimationRenderer::new);
        EntityRendererRegistry.register(ModEntities.RETURN_WORMHOLE, ReturnWormholeRenderer::new);

        ParticleFactoryRegistry.getInstance().register(ModParticles.WORMHOLE, ModParticleProviders.Wormhole::new);
        ParticleFactoryRegistry.getInstance().register(ModParticles.RETURN_WORMHOLE, ModParticleProviders.ReturnWormhole::new);
        ParticleFactoryRegistry.getInstance().register(ModParticles.SPARK, ModParticleProviders.Spark::new);

        UltraBeastsClient.init();
        // Fabric calls receivers on the network thread: the screen must open on the client one.
        ClientPlayNetworking.registerGlobalReceiver(EncounterCinematicPayload.TYPE, (payload, context) ->
                context.client().execute(() -> CinematicClientBridge.onCinematic(payload)));
    }
}
