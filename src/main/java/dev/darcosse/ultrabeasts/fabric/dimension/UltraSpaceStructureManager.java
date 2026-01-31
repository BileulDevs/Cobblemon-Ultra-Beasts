package dev.darcosse.ultrabeasts.fabric.dimension;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import dev.darcosse.ultrabeasts.fabric.UltraBeasts;
import dev.darcosse.ultrabeasts.fabric.registry.ModDimensions;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UltraSpaceStructureManager {

    /**
     * Regroupe tous les points d'intérêt relatifs à une structure.
     */
    public record StructureConfig(
            List<StructurePart> parts,
            BlockPos pokemonSpawn,
            BlockPos playerSpawn,
            BlockPos returnPortalSpawn
    ) {}

    private static final Map<String, StructureConfig> CONFIGS = Map.of(
            "nihilego", new StructureConfig(
                    List.of(
                            new StructurePart("nihilego_part1", new BlockPos(0, 0, 0)),
                            new StructurePart("nihilego_part2", new BlockPos(0, 0, 48))
                    ),
                    new BlockPos(26, 86, 20),    // Position du Pokémon
                    new BlockPos(26, 86, 76),    // Arrivée du joueur
                    new BlockPos(26, 87, 83)     // Position du portail retour
            ),
            "kartana", new StructureConfig(
                    List.of(
                            new StructurePart("kartana_part1", new BlockPos(0, 0, 0)),
                            new StructurePart("kartana_part2", new BlockPos(0, 0, 48)),
                            new StructurePart("kartana_part3", new BlockPos(0, 0, 96)),
                            new StructurePart("kartana_part4", new BlockPos(48, 0, 0)),
                            new StructurePart("kartana_part5", new BlockPos(48, 0, 48)),
                            new StructurePart("kartana_part6", new BlockPos(48, 0, 96))
                    ),
                    new BlockPos(52, 89, 24),
                    new BlockPos(47, 87, 87),
                    new BlockPos(56, 87, 91)
            ),
            "blacephalon", new StructureConfig(
                    List.of(
                            new StructurePart("blacephalon_part1", new BlockPos(0, 0, 0)),
                            new StructurePart("blacephalon_part2", new BlockPos(0, 0, 48))
                    ),
                    new BlockPos(24, 86, 12),
                    new BlockPos(20, 87, 42),
                    new BlockPos(20, 87, 51)
            ),
            "pheromosa", new StructureConfig(
                    List.of(
                            new StructurePart("pheromosa_part1", new BlockPos(0, 0, 0)),
                            new StructurePart("pheromosa_part1_1", new BlockPos(0, 48, 0)),
                            new StructurePart("pheromosa_part2", new BlockPos(0, 0, 48)),
                            new StructurePart("pheromosa_part3", new BlockPos(0, 0, 96)),
                            new StructurePart("pheromosa_part4", new BlockPos(48, 0, 0)),
                            new StructurePart("pheromosa_part5", new BlockPos(48, 0, 48)),
                            new StructurePart("pheromosa_part6", new BlockPos(48, 0, 96))
                    ),
                    new BlockPos(37, 99, 29),
                    new BlockPos(40, 98, 95),
                    new BlockPos(50, 95, 108)
            ),
            "buzzwole", new StructureConfig(
                    List.of(
                            new StructurePart("buzzwole_part1", new BlockPos(0, 0, 0)),
                            new StructurePart("buzzwole_part1_1", new BlockPos(0, 48, 0)),
                            new StructurePart("buzzwole_part2", new BlockPos(0, 0, 48)),
                            new StructurePart("buzzwole_part2_1", new BlockPos(0, 48, 48)),
                            new StructurePart("buzzwole_part3", new BlockPos(48, 0, 0)),
                            new StructurePart("buzzwole_part3_1", new BlockPos(48, 48, 0)),
                            new StructurePart("buzzwole_part4", new BlockPos(48, 0, 48)),
                            new StructurePart("buzzwole_part4_1", new BlockPos(48, 48, 48))
                    ),
                    new BlockPos(31, 113, 12),
                    new BlockPos(38, 118, 63),
                    new BlockPos(29, 117, 66)
            ),
            "guzzlord", new StructureConfig(
                    List.of(
                            new StructurePart("guzzlord_part1", new BlockPos(0, 0, 0)),
                            new StructurePart("guzzlord_part2", new BlockPos(0, 0, 48)),
                            new StructurePart("guzzlord_part3", new BlockPos(48, 0, 0)),
                            new StructurePart("guzzlord_part4", new BlockPos(48, 0, 48))
                    ),
                    new BlockPos(43, 81, 38),
                    new BlockPos(40, 83, 85),
                    new BlockPos(56, 83, 91)
            ),
            "celesteela", new StructureConfig(
                    List.of(
                            new StructurePart("celesteela_part1", new BlockPos(0, 0, 0)),
                            new StructurePart("celesteela_part2", new BlockPos(0, 0, 48)),
                            new StructurePart("celesteela_part3", new BlockPos(0, 0, 96)),
                            new StructurePart("celesteela_part4", new BlockPos(0, 0, 144)),
                            new StructurePart("celesteela_part5", new BlockPos(48, 0, 0)),
                            new StructurePart("celesteela_part6", new BlockPos(48, 0, 48)),
                            new StructurePart("celesteela_part7", new BlockPos(48, 0, 96)),
                            new StructurePart("celesteela_part8", new BlockPos(48, 0, 144))
                    ),
                    new BlockPos(58, 109, 36),
                    new BlockPos(23, 100, 120),
                    new BlockPos(23, 101, 130)
            ),
            "xurkitree", new StructureConfig(
                    List.of(
                            new StructurePart("xurkitree_part1", new BlockPos(0, 0, 0)),
                            new StructurePart("xurkitree_part1_1", new BlockPos(0, 48, 0)),
                            new StructurePart("xurkitree_part2", new BlockPos(0, 0, 48)),
                            new StructurePart("xurkitree_part2_1", new BlockPos(0, 48, 48)),
                            new StructurePart("xurkitree_part3", new BlockPos(0, 0, 96)),
                            new StructurePart("xurkitree_part4", new BlockPos(48, 0, 0)),
                            new StructurePart("xurkitree_part4_1", new BlockPos(48, 48, 0)),
                            new StructurePart("xurkitree_part5", new BlockPos(48, 0, 48)),
                            new StructurePart("xurkitree_part6", new BlockPos(48, 0, 96))
                    ),
                    new BlockPos(40, 113, 34),
                    new BlockPos(34, 96, 114),
                    new BlockPos(51, 94, 118)
            ),
            "poipole", new StructureConfig(
                    List.of(
                            new StructurePart("poipole_part1", new BlockPos(0, 0, 0)),
                            new StructurePart("poipole_part2", new BlockPos(0, 0, 48)),
                            new StructurePart("poipole_part3", new BlockPos(48, 0, 0)),
                            new StructurePart("poipole_part4", new BlockPos(48, 0, 48))
                    ),
                    new BlockPos(35, 87, 33),
                    new BlockPos(40, 83, 57),
                    new BlockPos(50, 84, 60)
            )
    );

    private static final Map<ServerWorld, BlockPos> placedStructures = new HashMap<>();

    /**
     * Récupère la configuration d'une structure par son nom.
     */
    public static StructureConfig getConfig(String name) {
        return CONFIGS.get(name);
    }

    /**
     * Génère la structure complète et fait apparaître l'Ultra-Chimère.
     */
    public static void placeStructure(ServerWorld world, String structureKey) {
        if (!world.getRegistryKey().equals(ModDimensions.ULTRA_SPACE_DIMENSION)) {
            return;
        }

        if (placedStructures.containsKey(world)) return;

        StructureConfig config = CONFIGS.get(structureKey);
        if (config == null) {
            UltraBeasts.LOGGER.error("Structure config not found for: {}", structureKey);
            return;
        }

        BlockPos basePos = new BlockPos(0, 64, 0);

        for (StructurePart part : config.parts) {
            Identifier id = Identifier.of(UltraBeasts.MOD_ID, part.name);
            StructureTemplate template = world.getStructureTemplateManager().getTemplateOrBlank(id);

            StructurePlacementData data = new StructurePlacementData()
                    .setIgnoreEntities(false)
                    .setRotation(BlockRotation.NONE)
                    .setMirror(BlockMirror.NONE);

            BlockPos finalPos = basePos.add(part.offset);
            template.place(world, finalPos, finalPos, data, world.getRandom(), 2);
        }

        placedStructures.put(world, basePos);

        clearItems(world);

        spawnUltraBeast(world, structureKey, config.pokemonSpawn);

        UltraBeasts.LOGGER.info("Placed composite structure {} at {}", structureKey, basePos);
    }

    /**
     * Nettoie la zone de la structure et les Pokémon résiduels.
     */
    public static void removeStructure(ServerWorld world) {
        if (!world.getRegistryKey().equals(ModDimensions.ULTRA_SPACE_DIMENSION)) return;

        BlockPos pos = placedStructures.remove(world);
        if (pos == null) return;

        BlockPos start = pos.add(-32, -16, -32);
        BlockPos end = pos.add(160, 140, 160);

        for (BlockPos target : BlockPos.iterate(start, end)) {
            if (!world.isAir(target)) {
                world.setBlockState(target, Blocks.AIR.getDefaultState(), 2 | 16 | 128);
            }
        }

        killAllPokemonOfWorld(world);
    }

    /**
     * Supprime tous les Pokémon de la dimension de manière sécurisée.
     */
    public static void killAllPokemonOfWorld(ServerWorld world) {
        if (world == null || world.getPlayers().isEmpty() || !world.getRegistryKey().equals(ModDimensions.ULTRA_SPACE_DIMENSION)) return;

        List<PokemonEntity> toRemove = new ArrayList<>();
        world.iterateEntities().forEach(entity -> {
            if (entity instanceof PokemonEntity pokemon) {
                toRemove.add(pokemon);
            }
        });

        for (PokemonEntity p : toRemove) {
            p.discard();
        }
    }

    private static void spawnUltraBeast(ServerWorld world, String pokemon, BlockPos pos) {
        var species = PokemonSpecies.getByName(pokemon);
        if (species == null) return;

        Pokemon ultraBeast = species.create(60);
        ultraBeast.sendOut(
                world,
                pos.toCenterPos().add(0, 1, 0),
                null,
                pokemonEntity -> {
                    pokemonEntity.setAiDisabled(true);

                    return null;
                }
        );
    }

    /**
     * Choisit une structure au hasard parmi celles enregistrées dans la configuration.
     * @param random Le générateur de nombres aléatoires du monde ou du joueur.
     * @return La clé (String) de la structure choisie.
     */
    public static String getRandomStructureKey(Random random) {
        List<String> keys = new ArrayList<>(CONFIGS.keySet());
        if (keys.isEmpty()) {
            return "poipole";
        }
        //return keys.get(random.nextInt(keys.size()));
        return "poipole";
    }

    private static void clearItems(ServerWorld world) {
        if (!world.getRegistryKey().equals(ModDimensions.ULTRA_SPACE_DIMENSION)) return;
        world.getEntitiesByType(EntityType.ITEM, itemEntity ->  true)
                .forEach(Entity::discard);
    }

    public record StructurePart(String name, BlockPos offset) {}
}