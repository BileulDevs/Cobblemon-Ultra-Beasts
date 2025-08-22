package dev.darcosse.chimeras.fabric;

import dev.darcosse.chimeras.fabric.commands.ChimerasCommands;
import dev.darcosse.chimeras.fabric.config.ConfigManager;
import dev.darcosse.chimeras.fabric.entity.ModEntities;
import dev.darcosse.chimeras.fabric.entity.WormholeEntity;
import dev.darcosse.chimeras.fabric.events.EventsHandler;
import dev.darcosse.chimeras.fabric.handler.UnbreakableBlocksHandler;
import dev.darcosse.chimeras.fabric.handler.VoidFallHandler;
import dev.darcosse.chimeras.fabric.registry.ModBlockSoundGroups;
import dev.darcosse.chimeras.fabric.registry.ModSounds;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.MapColor;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Random;

public class Chimeras implements ModInitializer {
    public static final String MOD_ID = "cobblemon_chimeras";

    public static final RegistryKey<World> CHIMERAS_DIMENSION = RegistryKey.of(
            RegistryKeys.WORLD,
            Identifier.of(MOD_ID, "chimeras_dimension")
    );

    public static final RegistryKey<DimensionType> CHIMERAS_DIMENSION_TYPE = RegistryKey.of(
            RegistryKeys.DIMENSION_TYPE,
            Identifier.of(MOD_ID, "chimeras_dimension_type")
    );

    public static final Block CHIMERAS_PORTAL_BLOCK = new ChimerasPortalBlock(
            FabricBlockSettings.create()
                    .mapColor(MapColor.BLACK)
                    .strength(50.0f, 1200.0f)
                    .sounds(ModBlockSoundGroups.PORTAL_SOUNDS)
                    .luminance(15)
    );

    public static final Block CHIMERAS_CORE_BLOCK = new ChimerasCoreBlock(
            FabricBlockSettings.create()
                    .mapColor(MapColor.BLACK)
                    .strength(50.0f, 1200.0f)
                    .sounds(BlockSoundGroup.HEAVY_CORE)
                    .luminance(15)
    );

    private int tickCounter = 0;
    private static final int CHECK_INTERVAL = 200; // En tick (5s)

    private void onServerTick(MinecraftServer server) {
        tickCounter++;
        if (tickCounter % CHECK_INTERVAL != 0) {
            return;
        }

        server.getWorlds().forEach(world -> {
            Random javaRandom = new Random();
            WormholeEntity.tryRandomSpawn(world, javaRandom);
        });
    }

    public static Logger LOGGER = LogManager.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ConfigManager.loadConfig();

        ChimerasCommands.registerCommands();
        ModSounds.registerSounds();
        ModBiomes.register();
        ChimerasRegistry.initialize();
        VoidFallHandler.initialize();
        UnbreakableBlocksHandler.initialize();
        EventsHandler.initializeEvents();
        ModEntities.init();

        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);

        Registry.register(
                Registries.CHUNK_GENERATOR,
                Identifier.of(MOD_ID, "chimeras_chunk_generator"),
                ChimerasChunkGenerator.CODEC
        );

        Registry.register(
                Registries.BLOCK,
                Identifier.of(MOD_ID, "chimeras_portal"),
                CHIMERAS_PORTAL_BLOCK
        );

        Registry.register(
                Registries.BLOCK,
                Identifier.of(MOD_ID, "chimeras_core"),
                CHIMERAS_CORE_BLOCK
        );

        Registry.register(
                Registries.ITEM,
                Identifier.of(MOD_ID, "chimeras_portal"),
                new BlockItem(CHIMERAS_PORTAL_BLOCK, new Item.Settings())
        );

        Registry.register(
                Registries.ITEM,
                Identifier.of(MOD_ID, "chimeras_core"),
                new BlockItem(CHIMERAS_CORE_BLOCK, new Item.Settings())
        );

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register(content -> {
            content.add(CHIMERAS_CORE_BLOCK);
        });
    }
}
