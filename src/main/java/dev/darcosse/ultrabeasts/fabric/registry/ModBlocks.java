package dev.darcosse.ultrabeasts.fabric.registry;

import dev.darcosse.ultrabeasts.fabric.UltraBeasts;
import dev.darcosse.ultrabeasts.fabric.blocks.ChimerasCoreBlock;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public class ModBlocks {

    public static final Block CHIMERAS_CORE_BLOCK = new ChimerasCoreBlock(
            FabricBlockSettings.create()
                    .mapColor(MapColor.BLACK)
                    .strength(50.0f, 1200.0f)
                    .sounds(BlockSoundGroup.HEAVY_CORE)
                    .luminance(15)
    );

    public static void registerBlocks() {
        Registry.register(Registries.BLOCK, Identifier.of(UltraBeasts.MOD_ID, "chimeras_core"), CHIMERAS_CORE_BLOCK);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS)
                .register(entries -> entries.add(CHIMERAS_CORE_BLOCK));

        UltraBeasts.LOGGER.info("Registering blocks");
    }
}
