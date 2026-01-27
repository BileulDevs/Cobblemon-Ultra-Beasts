package dev.darcosse.ultrabeasts.fabric.dimension;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import dev.darcosse.ultrabeasts.fabric.UltraBeasts;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UltraSpaceStructureManager {
    private static final Identifier[] STRUCTURES = new Identifier[]{
            Identifier.of(UltraBeasts.MOD_ID, "buzzwole")
    };

    private static final Map<String, BlockPos> spawnPositions = Map.of(
      "buzzwole", new BlockPos(0, 66, 0)
    );

    private static final Map<ServerWorld, BlockPos> placedStructures = new HashMap<>();

    /**
     * Place une structure aléatoire pour ce joueur/dimension
     */
    public static void placeStructure(ServerWorld world) {
        if (placedStructures.containsKey(world)) return;

        Random random = world.getRandom();
        Identifier chosen = STRUCTURES[random.nextInt(STRUCTURES.length)];
        String pokemon = chosen.getPath();

        StructureTemplate template = world.getStructureTemplateManager().getTemplateOrBlank(chosen);
        BlockPos pos = new BlockPos(0, 64, 0);

        StructurePlacementData data = new StructurePlacementData()
                .setIgnoreEntities(false)
                .setRotation(BlockRotation.NONE)
                .setMirror(BlockMirror.NONE);

        template.place(world, pos, pos, data, random, 2);
        placedStructures.put(world, pos);

        spawnUltraBeast(world, pokemon, spawnPositions.get(pokemon));

        UltraBeasts.LOGGER.info("Placed structure {} at {}", chosen, pos);
    }

    /**
     * Supprime la structure de cette dimension
     */
    public static void removeStructure(ServerWorld world) {
        BlockPos pos = placedStructures.remove(world);
        if (pos == null) return;

        int width = 32;
        int height = 32;
        int length = 32;

        for (int x = 0; x < width; x++)
            for (int y = 0; y < height; y++)
                for (int z = 0; z < length; z++)
                    world.setBlockState(pos.add(x, y, z), net.minecraft.block.Blocks.AIR.getDefaultState());

        UltraBeasts.LOGGER.info("Removed structure {} at {}", pos, pos);
    }

    /**
     * Fait apparaitre le Pokémon correspondant
     */
    private static void spawnUltraBeast(ServerWorld world, String pokemon, BlockPos pos) {
        Pokemon ultraBeast = PokemonSpecies.getByName(pokemon).create(60);

        ultraBeast.sendOut(
                world,
                pos.toCenterPos().add(0, 1, 0),
                null,
                pokemonEntity -> {
                    return null;
                }
        );
    }
}
