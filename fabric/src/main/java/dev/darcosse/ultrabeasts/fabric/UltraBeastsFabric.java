package dev.darcosse.ultrabeasts.fabric;

import dev.darcosse.ultrabeasts.UltraBeasts;
import dev.darcosse.ultrabeasts.dimension.VoidChunkGenerator;
import dev.darcosse.ultrabeasts.handler.UltraSpaceDamageHandler;
import dev.darcosse.ultrabeasts.handler.UltraSpaceSession;
import dev.darcosse.ultrabeasts.handler.UnbreakableBlocksHandler;
import dev.darcosse.ultrabeasts.handler.VoidFallHandler;
import dev.darcosse.ultrabeasts.network.EncounterCinematicPayload;
import dev.darcosse.ultrabeasts.registry.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;

/**
 * Fabric entry point: registers the mod's content in vanilla registries, starts the common
 * code, and wires its handlers to Fabric API events. Runs on both sides.
 */
public class UltraBeastsFabric implements ModInitializer {

    /** Builds an identifier in the mod's namespace. */
    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(UltraBeasts.MOD_ID, path);
    }

    /**
     * Content is registered before {@code UltraBeasts.init}, so the static fields the common
     * code reads are filled by the time it starts.
     */
    @Override
    public void onInitialize() {
        registerParticles();
        registerSounds();
        registerEntities();
        registerChunkGenerator();
        registerPayloads();

        UltraBeasts.init(new FabricPlatformAdapter());

        registerEvents();
    }

    /** Registers the particle types and publishes them in {@link ModParticles}. */
    private void registerParticles() {
        ModParticles.WORMHOLE = Registry.register(BuiltInRegistries.PARTICLE_TYPE,
                id(ModParticles.WORMHOLE_ID), ModParticles.create());
        ModParticles.RETURN_WORMHOLE = Registry.register(BuiltInRegistries.PARTICLE_TYPE,
                id(ModParticles.RETURN_WORMHOLE_ID), ModParticles.create());
        ModParticles.SPARK = Registry.register(BuiltInRegistries.PARTICLE_TYPE,
                id(ModParticles.SPARK_ID), ModParticles.create());

        UltraBeasts.LOGGER.info("Registering Particles for {}", UltraBeasts.MOD_ID);
    }

    /** Registers the sound events declared in {@link ModSounds}. */
    private void registerSounds() {
        Registry.register(BuiltInRegistries.SOUND_EVENT, ModSounds.WORMHOLE_SPAWN_ID, ModSounds.WORMHOLE_SPAWN);
        Registry.register(BuiltInRegistries.SOUND_EVENT, ModSounds.WORMHOLE_ANIMATION_SPAWN_ID, ModSounds.WORMHOLE_ANIMATION_SPAWN);
        Registry.register(BuiltInRegistries.SOUND_EVENT, ModSounds.WORMHOLE_AMBIENT_ID, ModSounds.WORMHOLE_AMBIENT);
        Registry.register(BuiltInRegistries.SOUND_EVENT, ModSounds.ENCOUNTER_INTRO_ID, ModSounds.ENCOUNTER_INTRO);
        Registry.register(BuiltInRegistries.SOUND_EVENT, ModSounds.ENCOUNTER_ROAR_ID, ModSounds.ENCOUNTER_ROAR);

        UltraBeasts.LOGGER.info("Registering Sound Events for {}", UltraBeasts.MOD_ID);
    }

    /** Registers the entity types and publishes them in {@link ModEntities}. */
    private void registerEntities() {
        ModEntities.WORMHOLE = Registry.register(BuiltInRegistries.ENTITY_TYPE,
                id(ModEntities.WORMHOLE_ID), ModEntities.createWormhole());
        ModEntities.WORMHOLE_ANIMATION = Registry.register(BuiltInRegistries.ENTITY_TYPE,
                id(ModEntities.WORMHOLE_ANIMATION_ID), ModEntities.createWormholeAnimation());
        ModEntities.RETURN_WORMHOLE = Registry.register(BuiltInRegistries.ENTITY_TYPE,
                id(ModEntities.RETURN_WORMHOLE_ID), ModEntities.createReturnWormhole());

        UltraBeasts.LOGGER.info("Registering Entities for {}", UltraBeasts.MOD_ID);
    }

    /** Registers the void chunk generator referenced by the dimension JSON. */
    private void registerChunkGenerator() {
        Registry.register(BuiltInRegistries.CHUNK_GENERATOR,
                id("void_generator"), VoidChunkGenerator.CODEC);

        UltraBeasts.LOGGER.info("Registering Generators for {}", UltraBeasts.MOD_ID);
    }

    /**
     * Declares the payload types. Must run on both sides: a dedicated server needs the codec
     * to encode, the client to decode. The client receiver is in the client entry point.
     */
    private void registerPayloads() {
        PayloadTypeRegistry.playS2C().register(EncounterCinematicPayload.TYPE, EncounterCinematicPayload.CODEC);
    }

    /** Connects the loader-independent handlers to Fabric API events. */
    private void registerEvents() {
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> ModCommands.register(dispatcher));

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ModEvents.onServerTick(server);
            VoidFallHandler.onServerTick(server);
        });

        ServerLifecycleEvents.SERVER_STARTED.register(ModEvents::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(ModEvents::onServerStopping);

        ServerPlayConnectionEvents.JOIN.register(
                (handler, sender, server) -> UltraSpaceSession.onPlayerJoin(handler.player));

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UltraSpaceSession.onPlayerDisconnect(handler.player);
            VoidFallHandler.onPlayerDisconnect(handler.player);
        });

        PlayerBlockBreakEvents.BEFORE.register(
                (world, player, pos, state, blockEntity) -> UnbreakableBlocksHandler.allowBlockBreak(world));

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) ->
                UnbreakableBlocksHandler.shouldCancelUseBlock(player, world, hand)
                        ? InteractionResult.FAIL
                        : InteractionResult.PASS);

        ServerLivingEntityEvents.ALLOW_DAMAGE.register(
                (entity, source, amount) -> UltraSpaceDamageHandler.allowDamage(entity, source));

        UltraBeasts.LOGGER.info("Registering Handlers for {}", UltraBeasts.MOD_ID);
    }
}
