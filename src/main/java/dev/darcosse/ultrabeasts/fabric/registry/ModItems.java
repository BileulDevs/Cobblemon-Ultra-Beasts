package dev.darcosse.ultrabeasts.fabric.registry;

import dev.darcosse.ultrabeasts.fabric.UltraBeasts;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {
    public static void registerItems() {
        Registry.register(
                Registries.ITEM,
                Identifier.of(UltraBeasts.MOD_ID, "chimeras_core"),
                new BlockItem(ModBlocks.CHIMERAS_CORE_BLOCK, new Item.Settings())
        );

        UltraBeasts.LOGGER.info("Items registered!");
    }
}
