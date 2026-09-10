package dev.darcosse.ultrabeasts.dimension;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import dev.darcosse.ultrabeasts.UltraBeasts;
import dev.darcosse.ultrabeasts.registry.ModDimensions;
import dev.darcosse.ultrabeasts.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UltraSpaceStructureManager {

    /**
     * Groups every point of interest belonging to a structure.
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
                    new BlockPos(26, 86, 20),
                    new BlockPos(26, 86, 76),
                    new BlockPos(26, 87, 83)
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
                    new BlockPos(24, 89, 12),
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
            ),
            "stakataka", new StructureConfig(
                    List.of(
                            new StructurePart("stakataka_part1", new BlockPos(0, 0, 0)),
                            new StructurePart("stakataka_part2", new BlockPos(0, 0, 48)),
                            new StructurePart("stakataka_part3", new BlockPos(48, 0, 0)),
                            new StructurePart("stakataka_part4", new BlockPos(48, 0, 48))
                    ),
                    new BlockPos(42, 82, 35),
                    new BlockPos(20, 84, 47),
                    new BlockPos(38, 85, 66)
            )
    );

    /**
     * Returns a structure configuration by name.
     */
    public static StructureConfig getConfig(String name) {
        return CONFIGS.get(name);
    }

    /**
     * Builds the whole structure and spawns the Ultra Beast.
     */
    public static void placeStructure(ServerLevel level, String structureKey) {
        if (!level.dimension().equals(ModDimensions.ULTRA_SPACE_DIMENSION)) return;

        removeStructure(level);

        StructureConfig config = CONFIGS.get(structureKey);
        if (config == null) return;

        BlockPos basePos = new BlockPos(0, 64, 0);
        UltraSpaceState state = UltraSpaceState.getServerState(level);
        state.structureBlocks.clear();

        for (StructurePart part : config.parts) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(UltraBeasts.MOD_ID, part.name);
            StructureTemplate template = level.getStructureManager().getOrCreate(id);

            BlockPos startPos = basePos.offset(part.offset);

            Vec3i sizeV = template.getSize();
            BlockPos size = new BlockPos(sizeV.getX(), sizeV.getY(), sizeV.getZ());

            BlockPos endPos = startPos.offset(size.getX() - 1, size.getY() - 1, size.getZ() - 1);

            for (BlockPos p : BlockPos.betweenClosed(startPos, endPos)) {
                state.structureBlocks.add(p.immutable());
            }

            template.placeInWorld(level, startPos, startPos, new StructurePlaceSettings(), level.getRandom(), 2);
        }

        state.setDirty();

        clearItems(level);
        spawnUltraBeast(level, structureKey, config.pokemonSpawn);
    }

    /**
     * Clears the structure area and any leftover Pokemon.
     */
    public static void removeStructure(ServerLevel level) {
        if (!level.dimension().equals(ModDimensions.ULTRA_SPACE_DIMENSION)) return;

        UltraSpaceState state = UltraSpaceState.getServerState(level);

        if (state.structureBlocks.isEmpty()) {
            return;
        }

        for (BlockPos pos : state.structureBlocks) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 128);
        }

        List<Entity> toRemove = new ArrayList<>();
        level.getAllEntities().forEach(entity -> {
            if (entity != null && !(entity instanceof Player)) {
                toRemove.add(entity);
            }
        });
        for (Entity entity : toRemove) {
            if (!entity.isRemoved()) entity.discard();
        }

        state.structureBlocks.clear();
        state.setDirty();

        UltraBeasts.LOGGER.info("Ultra-Space structure deleted.");
    }

    /**
     * Safely removes every Pokemon from the dimension.
     */
    public static void killAllPokemonOfWorld(ServerLevel level) {
        if (level == null || !level.dimension().equals(ModDimensions.ULTRA_SPACE_DIMENSION)) return;

        List<Entity> targets = new ArrayList<>();
        level.getAllEntities().forEach(e -> {
            if (e instanceof PokemonEntity) {
                targets.add(e);
            }
        });

        for (Entity pokemon : targets) {
            pokemon.discard();
        }
    }

    private static void spawnUltraBeast(ServerLevel level, String pokemon, BlockPos pos) {
        var species = PokemonSpecies.getByName(pokemon);
        if (species == null) return;

        Pokemon ultraBeast = species.create(60);
        ultraBeast.sendOut(
                level,
                pos.getCenter().add(0, 1, 0),
                null,
                pokemonEntity -> {
                    pokemonEntity.setNoAi(true);
                    return null;
                }
        );
    }

    /**
     * Picks a random structure among the registered configurations.
     */
    public static String getRandomStructureKey(RandomSource random) {
        List<String> keys = new ArrayList<>(CONFIGS.keySet());
        if (keys.isEmpty()) {
            return "poipole";
        }
        return keys.get(random.nextInt(keys.size()));
    }

    private static void clearItems(ServerLevel level) {
        if (!level.dimension().equals(ModDimensions.ULTRA_SPACE_DIMENSION)) return;

        level.getServer().execute(() -> {
            level.getEntities(ModEntities.RETURN_WORMHOLE, e -> e != null)
                    .forEach(Entity::discard);

            level.getEntities(EntityType.ITEM, item -> item != null)
                    .forEach(Entity::discard);

            UltraBeasts.LOGGER.info("Ultra-Space items cleared after structure placement.");
        });
    }

    public record StructurePart(String name, BlockPos offset) {}
}
