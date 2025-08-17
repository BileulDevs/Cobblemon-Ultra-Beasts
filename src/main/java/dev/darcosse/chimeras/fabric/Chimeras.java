package dev.darcosse.chimeras.fabric;

import net.fabricmc.api.ModInitializer;
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
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

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
                    .sounds(BlockSoundGroup.GLASS)
                    .luminance(10)
    );

    public static final Block CHIMERAS_CORE_BLOCK = new ChimerasCoreBlock(
            FabricBlockSettings.create()
                    .mapColor(MapColor.BLACK)
                    .strength(50.0f, 1200.0f)
                    .sounds(BlockSoundGroup.GLASS)
                    .luminance(10)
    );

    @Override
    public void onInitialize() {

        ChimerasRegistry.initialize();

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
            content.add(CHIMERAS_PORTAL_BLOCK);
            content.add(CHIMERAS_CORE_BLOCK);
        });
    }
}
