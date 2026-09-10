package dev.darcosse.ultrabeasts.fabric;

import dev.darcosse.ultrabeasts.UltraBeasts;
import dev.darcosse.ultrabeasts.dimension.VoidChunkGenerator;
import dev.darcosse.ultrabeasts.handler.FallingDamageHandler;
import dev.darcosse.ultrabeasts.handler.UnbreakableBlocksHandler;
import dev.darcosse.ultrabeasts.handler.VoidFallHandler;
import dev.darcosse.ultrabeasts.registry.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;

public class UltraBeastsFabric implements ModInitializer {

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(UltraBeasts.MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        registerParticles();
        registerSounds();
        registerEntities();
        registerChunkGenerator();

        UltraBeasts.init(new FabricPlatformAdapter());

        registerEvents();
    }

    private void registerParticles() {
        ModParticles.WORMHOLE = Registry.register(BuiltInRegistries.PARTICLE_TYPE,
                id(ModParticles.WORMHOLE_ID), ModParticles.create());
        ModParticles.RETURN_WORMHOLE = Registry.register(BuiltInRegistries.PARTICLE_TYPE,
                id(ModParticles.RETURN_WORMHOLE_ID), ModParticles.create());
        ModParticles.SPARK = Registry.register(BuiltInRegistries.PARTICLE_TYPE,
                id(ModParticles.SPARK_ID), ModParticles.create());

        UltraBeasts.LOGGER.info("Registering Particles for " + UltraBeasts.MOD_ID);
    }

    private void registerSounds() {
        Registry.register(BuiltInRegistries.SOUND_EVENT, ModSounds.WORMHOLE_SPAWN_ID, ModSounds.WORMHOLE_SPAWN);
        Registry.register(BuiltInRegistries.SOUND_EVENT, ModSounds.WORMHOLE_ANIMATION_SPAWN_ID, ModSounds.WORMHOLE_ANIMATION_SPAWN);
        Registry.register(BuiltInRegistries.SOUND_EVENT, ModSounds.WORMHOLE_AMBIENT_ID, ModSounds.WORMHOLE_AMBIENT);

        UltraBeasts.LOGGER.info("Registering Sound Events for " + UltraBeasts.MOD_ID);
    }

    private void registerEntities() {
        ModEntities.WORMHOLE = Registry.register(BuiltInRegistries.ENTITY_TYPE,
                id(ModEntities.WORMHOLE_ID), ModEntities.createWormhole());
        ModEntities.WORMHOLE_ANIMATION = Registry.register(BuiltInRegistries.ENTITY_TYPE,
                id(ModEntities.WORMHOLE_ANIMATION_ID), ModEntities.createWormholeAnimation());
        ModEntities.RETURN_WORMHOLE = Registry.register(BuiltInRegistries.ENTITY_TYPE,
                id(ModEntities.RETURN_WORMHOLE_ID), ModEntities.createReturnWormhole());

        UltraBeasts.LOGGER.info("Registering Entities for " + UltraBeasts.MOD_ID);
    }

    private void registerChunkGenerator() {
        Registry.register(BuiltInRegistries.CHUNK_GENERATOR,
                id("void_generator"), VoidChunkGenerator.CODEC);

        UltraBeasts.LOGGER.info("Registering Generators for " + UltraBeasts.MOD_ID);
    }

    private void registerEvents() {
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> ModCommands.register(dispatcher));

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ModEvents.onServerTick(server);
            VoidFallHandler.onServerTick(server);
        });

        ServerLifecycleEvents.SERVER_STARTED.register(ModEvents::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(ModEvents::onServerStopping);

        ServerPlayConnectionEvents.DISCONNECT.register(
                (handler, server) -> VoidFallHandler.onPlayerDisconnect(handler.player));

        PlayerBlockBreakEvents.BEFORE.register(
                (world, player, pos, state, blockEntity) -> UnbreakableBlocksHandler.allowBlockBreak(world));

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) ->
                UnbreakableBlocksHandler.shouldCancelUseBlock(player, world, hand)
                        ? InteractionResult.FAIL
                        : InteractionResult.PASS);

        ServerLivingEntityEvents.ALLOW_DAMAGE.register(
                (entity, source, amount) -> FallingDamageHandler.allowDamage(entity, source));

        UltraBeasts.LOGGER.info("Registering Handlers for " + UltraBeasts.MOD_ID);
    }
}
