package dev.darcosse.ultrabeasts.neoforge.client;

import dev.darcosse.ultrabeasts.UltraBeasts;
import dev.darcosse.ultrabeasts.client.particle.ModParticleProviders;
import dev.darcosse.ultrabeasts.client.renderer.ReturnWormholeRenderer;
import dev.darcosse.ultrabeasts.client.renderer.WormholeRenderer;
import dev.darcosse.ultrabeasts.client.renderer.WormholeSpawnAnimationRenderer;
import dev.darcosse.ultrabeasts.neoforge.UltraBeastsNeoForge;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

/**
 * Registration goes through the DeferredHolders rather than the static fields
 * in ModEntities / ModParticles: those are only published in FMLCommonSetupEvent,
 * which fires AFTER RegisterRenderers on the mod bus. Reading them here would
 * hand a null key to the renderer map.
 *
 * The holders are resolved as soon as the registries are populated, so .get()
 * is safe at this point.
 */
@EventBusSubscriber(modid = UltraBeasts.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class UltraBeastsNeoForgeClient {

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(UltraBeastsNeoForge.WORMHOLE.get(), WormholeRenderer::new);
        event.registerEntityRenderer(UltraBeastsNeoForge.WORMHOLE_ANIMATION.get(), WormholeSpawnAnimationRenderer::new);
        event.registerEntityRenderer(UltraBeastsNeoForge.RETURN_WORMHOLE.get(), ReturnWormholeRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(UltraBeastsNeoForge.P_WORMHOLE.get(), ModParticleProviders.Wormhole::new);
        event.registerSpriteSet(UltraBeastsNeoForge.P_RETURN_WORMHOLE.get(), ModParticleProviders.ReturnWormhole::new);
        event.registerSpriteSet(UltraBeastsNeoForge.P_SPARK.get(), ModParticleProviders.Spark::new);
    }
}
