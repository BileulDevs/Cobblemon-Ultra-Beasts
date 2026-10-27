package dev.darcosse.ultrabeasts.neoforge;

import com.mojang.serialization.MapCodec;
import dev.darcosse.ultrabeasts.UltraBeasts;
import dev.darcosse.ultrabeasts.dimension.VoidChunkGenerator;
import dev.darcosse.ultrabeasts.handler.UltraSpaceDamageHandler;
import dev.darcosse.ultrabeasts.handler.UltraSpaceSession;
import dev.darcosse.ultrabeasts.handler.UnbreakableBlocksHandler;
import dev.darcosse.ultrabeasts.handler.VoidFallHandler;
import dev.darcosse.ultrabeasts.registry.ModCommands;
import dev.darcosse.ultrabeasts.registry.ModEntities;
import dev.darcosse.ultrabeasts.registry.ModEvents;
import dev.darcosse.ultrabeasts.registry.ModParticles;
import dev.darcosse.ultrabeasts.registry.ModSounds;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * NeoForge entry point: registers the mod's content through deferred registers, starts the
 * common code, and wires its handlers to NeoForge events. Runs on both sides.
 */
@Mod(UltraBeasts.MOD_ID)
public class UltraBeastsNeoForge {

    private static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, UltraBeasts.MOD_ID);

    private static final DeferredRegister<net.minecraft.core.particles.ParticleType<?>> PARTICLES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, UltraBeasts.MOD_ID);

    private static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, UltraBeasts.MOD_ID);

    private static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, UltraBeasts.MOD_ID);

    // Entities
    public static final DeferredHolder<EntityType<?>, EntityType<dev.darcosse.ultrabeasts.entity.WormholeEntity>>
            WORMHOLE = ENTITIES.register(ModEntities.WORMHOLE_ID, ModEntities::createWormhole);
    public static final DeferredHolder<EntityType<?>, EntityType<dev.darcosse.ultrabeasts.entity.WormholeSpawnAnimation>>
            WORMHOLE_ANIMATION = ENTITIES.register(ModEntities.WORMHOLE_ANIMATION_ID, ModEntities::createWormholeAnimation);
    public static final DeferredHolder<EntityType<?>, EntityType<dev.darcosse.ultrabeasts.entity.ReturnWormholeEntity>>
            RETURN_WORMHOLE = ENTITIES.register(ModEntities.RETURN_WORMHOLE_ID, ModEntities::createReturnWormhole);

    // Particles
    public static final DeferredHolder<net.minecraft.core.particles.ParticleType<?>, SimpleParticleType>
            P_WORMHOLE = PARTICLES.register(ModParticles.WORMHOLE_ID, ModParticles::create);
    public static final DeferredHolder<net.minecraft.core.particles.ParticleType<?>, SimpleParticleType>
            P_RETURN_WORMHOLE = PARTICLES.register(ModParticles.RETURN_WORMHOLE_ID, ModParticles::create);
    public static final DeferredHolder<net.minecraft.core.particles.ParticleType<?>, SimpleParticleType>
            P_SPARK = PARTICLES.register(ModParticles.SPARK_ID, ModParticles::create);

    // Sounds. Ids must match ModSounds and sounds.json.
    private static final DeferredHolder<SoundEvent, SoundEvent> S_SPAWN =
            SOUNDS.register("wormhole_spawn", () -> ModSounds.WORMHOLE_SPAWN);
    private static final DeferredHolder<SoundEvent, SoundEvent> S_ANIM_SPAWN =
            SOUNDS.register("wormhole_animation_spawn", () -> ModSounds.WORMHOLE_ANIMATION_SPAWN);
    private static final DeferredHolder<SoundEvent, SoundEvent> S_AMBIENT =
            SOUNDS.register("wormhole_ambient", () -> ModSounds.WORMHOLE_AMBIENT);

    // Chunk generator
    private static final DeferredHolder<MapCodec<? extends ChunkGenerator>, MapCodec<VoidChunkGenerator>>
            VOID_GENERATOR = CHUNK_GENERATORS.register("void_generator", () -> VoidChunkGenerator.CODEC);

    /** Called by NeoForge with the mod's event bus. */
    public UltraBeastsNeoForge(IEventBus modEventBus) {
        ENTITIES.register(modEventBus);
        PARTICLES.register(modEventBus);
        SOUNDS.register(modEventBus);
        CHUNK_GENERATORS.register(modEventBus);

        // DeferredHolder resolves lazily, but the common code reads plain static
        // fields, so publish them once registration has run.
        modEventBus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) -> {
            ModEntities.WORMHOLE = WORMHOLE.get();
            ModEntities.WORMHOLE_ANIMATION = WORMHOLE_ANIMATION.get();
            ModEntities.RETURN_WORMHOLE = RETURN_WORMHOLE.get();

            ModParticles.WORMHOLE = P_WORMHOLE.get();
            ModParticles.RETURN_WORMHOLE = P_RETURN_WORMHOLE.get();
            ModParticles.SPARK = P_SPARK.get();
        });

        UltraBeasts.init(new NeoForgePlatformAdapter());

        NeoForge.EVENT_BUS.register(this);
    }

    /** Registers {@code /ultrabeasts}. */
    @SubscribeEvent
    public void onCommandRegistration(RegisterCommandsEvent event) {
        ModCommands.register(event.getDispatcher());
    }

    /** Per-tick server work, after the vanilla tick (Post) like Fabric's END_SERVER_TICK. */
    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        ModEvents.onServerTick(event.getServer());
        VoidFallHandler.onServerTick(event.getServer());
    }

    /** Clean-up of the previous session's entities. */
    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        ModEvents.onServerStarted(event.getServer());
    }

    /** Clean-up before the world is saved. */
    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        ModEvents.onServerStopping(event.getServer());
    }

    /** Resumes or ends the Ultra-Space session of a player logging in there. */
    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UltraSpaceSession.onPlayerJoin(player);
        }
    }

    /** Starts the occupant's grace period and forgets their void-fall state. */
    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UltraSpaceSession.onPlayerDisconnect(player);
            VoidFallHandler.onPlayerDisconnect(player);
        }
    }

    /** Cancels block breaking in Ultra-Space. */
    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!UnbreakableBlocksHandler.allowBlockBreak(event.getPlayer().level())) {
            event.setCanceled(true);
        }
    }

    /** Cancels block placement in Ultra-Space. */
    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (UnbreakableBlocksHandler.shouldCancelUseBlock(
                event.getEntity(), event.getLevel(), event.getHand())) {
            event.setCanceled(true);
        }
    }

    /** Cancels all damage to players in Ultra-Space. */
    @SubscribeEvent
    public void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!UltraSpaceDamageHandler.allowDamage(event.getEntity(), event.getSource())) {
            event.setCanceled(true);
        }
    }
}
