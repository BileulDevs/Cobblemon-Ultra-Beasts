package dev.darcosse.ultrabeasts.dimension;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import dev.darcosse.ultrabeasts.UltraBeasts;
import dev.darcosse.ultrabeasts.entity.ReturnWormholeEntity;
import dev.darcosse.ultrabeasts.registry.ModDimensions;
import dev.darcosse.ultrabeasts.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Builds and tears down the single Ultra-Space structure, and owns everything placed with
 * it: the exit portal and the resident Ultra Beast.
 * <p>
 * Each Ultra Beast has a hand-built structure, split into structure-block templates of at
 * most 48 blocks per side. Server side only.
 */
public final class UltraSpaceStructureManager {

    /** Air, resolved once instead of on every cleared block. */
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    /**
     * Flags for clearing the structure.
     * <p>
     * UPDATE_KNOWN_SHAPE skips the neighbour shape updates: every neighbour is being removed
     * too, so they are pure cost (six per block). UPDATE_SUPPRESS_DROPS stops torches, carpets
     * and the like from popping as items when their support goes. No UPDATE_CLIENTS: the
     * structure is only removed once nobody needs to see it.
     * <p>
     * Up to 4.0 the value was 128, which is not a flag in 1.21.1 and behaved like 0: neighbour
     * shape updates ran for every cleared block and attached blocks dropped items.
     */
    private static final int CLEAR_FLAGS = Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;

    /** Flags for placing templates: notify clients only (unchanged from 4.0). */
    private static final int PLACE_FLAGS = Block.UPDATE_CLIENTS;

    /** Level of the resident Ultra Beast. Higher makes the capture harder. */
    private static final int ULTRA_BEAST_LEVEL = 60;

    /** World position of every template's offset origin. Changing it moves all structures. */
    private static final BlockPos STRUCTURE_ORIGIN = new BlockPos(0, 64, 0);

    /** Height of the exit portal entity above its recorded block position. */
    private static final double RETURN_PORTAL_HEIGHT = 2.0;

    /**
     * Every point of interest of one structure.
     *
     * @param parts             templates making up the structure, with their offsets
     * @param pokemonSpawn      where the Ultra Beast is sent out
     * @param playerSpawn       where the player arrives
     * @param returnPortalSpawn where the exit portal is placed
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
     * Structure keys, copied once: picking a random structure used to copy the key set into
     * a new list on every wormhole entry.
     */
    private static final List<String> STRUCTURE_KEYS = List.copyOf(CONFIGS.keySet());

    /** Static utility, never instantiated. */
    private UltraSpaceStructureManager() {
    }

    /** Returns the configuration of a structure, or null for an unknown key. */
    public static StructureConfig getConfig(String name) {
        return CONFIGS.get(name);
    }

    /**
     * Replaces whatever stands in Ultra-Space with the given structure, then spawns its exit
     * portal and its Ultra Beast. The key doubles as the Cobblemon species name.
     */
    public static void placeStructure(ServerLevel level, String structureKey) {
        if (!isUltraSpace(level)) return;

        removeStructure(level);

        StructureConfig config = CONFIGS.get(structureKey);
        if (config == null) return;

        UltraSpaceState state = UltraSpaceState.getServerState(level);
        state.clearRegions();

        for (StructurePart part : config.parts) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(UltraBeasts.MOD_ID, part.name);
            StructureTemplate template = level.getStructureManager().getOrCreate(id);

            BlockPos startPos = STRUCTURE_ORIGIN.offset(part.offset);

            Vec3i size = template.getSize();
            BlockPos endPos = startPos.offset(size.getX() - 1, size.getY() - 1, size.getZ() - 1);

            // One bounding box per part instead of one entry per block.
            state.addRegion(new UltraSpaceState.Region(startPos, endPos));

            template.placeInWorld(level, startPos, startPos, new StructurePlaceSettings(), level.getRandom(), PLACE_FLAGS);
        }

        state.setReturnPortalPos(config.returnPortalSpawn());
        state.setDirty();

        clearItems(level);
        spawnReturnPortal(level, config.returnPortalSpawn());
        spawnUltraBeast(level, structureKey, config.pokemonSpawn);
    }

    /**
     * Clears the structure volume and every non-player entity in Ultra-Space, and forgets the
     * exit portal position.
     * <p>
     * Runs on the server thread when a player leaves, so its cost is a visible lag spike:
     * see {@link #clearRegion} for how it is kept down.
     */
    public static void removeStructure(ServerLevel level) {
        if (!isUltraSpace(level)) return;

        UltraSpaceState state = UltraSpaceState.getServerState(level);

        state.setReturnPortalPos(null);

        if (!state.hasStructure()) {
            return;
        }

        for (UltraSpaceState.Region region : state.getRegions()) {
            clearRegion(level, region);
        }

        discardEntities(level, entity -> !(entity instanceof Player));

        state.clearRegions();

        UltraBeasts.LOGGER.info("Ultra-Space structure deleted.");
    }

    /**
     * Sets every non-air block of one region to air, skipping empty chunk sections.
     * <p>
     * The previous version read every single position: for celesteela, 8 parts of 48³, that
     * is about 885 000 block reads per exit, most of them air in a void dimension. Sections
     * (16³) whose palette holds only air are now skipped whole, and the remaining reads go
     * to the section directly instead of through the level.
     */
    private static void clearRegion(ServerLevel level, UltraSpaceState.Region region) {
        BlockPos min = region.min();
        BlockPos max = region.max();

        // Mutable cursor: avoids allocating a BlockPos per cleared block.
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int chunkX = SectionPos.blockToSectionCoord(min.getX()); chunkX <= SectionPos.blockToSectionCoord(max.getX()); chunkX++) {
            for (int chunkZ = SectionPos.blockToSectionCoord(min.getZ()); chunkZ <= SectionPos.blockToSectionCoord(max.getZ()); chunkZ++) {
                LevelChunk chunk = level.getChunk(chunkX, chunkZ);

                int minX = Math.max(min.getX(), SectionPos.sectionToBlockCoord(chunkX));
                int maxX = Math.min(max.getX(), SectionPos.sectionToBlockCoord(chunkX, 15));
                int minZ = Math.max(min.getZ(), SectionPos.sectionToBlockCoord(chunkZ));
                int maxZ = Math.min(max.getZ(), SectionPos.sectionToBlockCoord(chunkZ, 15));

                for (int sectionY = SectionPos.blockToSectionCoord(min.getY()); sectionY <= SectionPos.blockToSectionCoord(max.getY()); sectionY++) {
                    int sectionIndex = chunk.getSectionIndexFromSectionY(sectionY);
                    if (sectionIndex < 0 || sectionIndex >= chunk.getSectionsCount()) continue;

                    LevelChunkSection section = chunk.getSection(sectionIndex);
                    if (section.hasOnlyAir()) continue;

                    int minY = Math.max(min.getY(), SectionPos.sectionToBlockCoord(sectionY));
                    int maxY = Math.min(max.getY(), SectionPos.sectionToBlockCoord(sectionY, 15));

                    for (int x = minX; x <= maxX; x++) {
                        for (int y = minY; y <= maxY; y++) {
                            for (int z = minZ; z <= maxZ; z++) {
                                // Section-local coordinates are the low 4 bits of world ones.
                                if (section.getBlockState(x & 15, y & 15, z & 15).isAir()) continue;

                                cursor.set(x, y, z);
                                // Through the level, not the section: this removes block
                                // entities and keeps heightmaps and lighting consistent.
                                level.setBlock(cursor, AIR, CLEAR_FLAGS);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Puts the exit portal back if it went missing while a structure is still standing.
     * <p>
     * Entities do not survive the server-start clean-up, and a chunk can unload with nobody
     * nearby. Without this, a player reconnecting inside a closed structure such as kartana
     * has no way out at all: those layouts do not expose the void, so falling out is not an
     * option either.
     */
    public static void ensureReturnPortal(ServerLevel level) {
        if (!isUltraSpace(level)) return;

        UltraSpaceState state = UltraSpaceState.getServerState(level);
        if (!state.hasStructure()) return;

        BlockPos pos = state.getReturnPortalPos();
        if (pos == null) return;

        if (!level.getEntities(ModEntities.RETURN_WORMHOLE, e -> !e.isRemoved()).isEmpty()) {
            return;
        }

        UltraBeasts.LOGGER.info("Exit portal was missing, restoring it at {}.", pos);
        spawnReturnPortal(level, pos);
    }

    /** Spawns the exit portal at {@code pos}, replacing any existing one. */
    public static void spawnReturnPortal(ServerLevel level, BlockPos pos) {
        level.getEntities(ModEntities.RETURN_WORMHOLE, e -> true).forEach(Entity::discard);

        ReturnWormholeEntity portal = new ReturnWormholeEntity(ModEntities.RETURN_WORMHOLE, level);
        portal.setPos(pos.getX() + 0.5, pos.getY() + RETURN_PORTAL_HEIGHT, pos.getZ() + 0.5);
        level.addFreshEntity(portal);
    }

    /** Removes every Pokémon entity from Ultra-Space. Does nothing in any other level. */
    public static void killAllPokemonOfWorld(ServerLevel level) {
        if (level == null || !isUltraSpace(level)) return;

        discardEntities(level, entity -> entity instanceof PokemonEntity);
    }

    /** Whether the level is the Ultra-Space dimension. Every public entry point checks it. */
    private static boolean isUltraSpace(ServerLevel level) {
        return level.dimension().equals(ModDimensions.ULTRA_SPACE_DIMENSION);
    }

    /**
     * Discards every live entity of the level matching the filter. Collects first, then
     * discards: mutating the entity list while iterating it is not safe.
     */
    private static void discardEntities(ServerLevel level, Predicate<Entity> filter) {
        List<Entity> targets = new ArrayList<>();

        for (Entity entity : level.getAllEntities()) {
            if (entity != null && !entity.isRemoved() && filter.test(entity)) {
                targets.add(entity);
            }
        }

        for (Entity entity : targets) {
            entity.discard();
        }
    }

    /**
     * Sends out the structure's Ultra Beast as a frozen, invulnerable wild Pokémon that does
     * not count towards Cobblemon's spawn cap.
     */
    private static void spawnUltraBeast(ServerLevel level, String pokemon, BlockPos pos) {
        var species = PokemonSpecies.getByName(pokemon);
        if (species == null) {
            UltraBeasts.LOGGER.warn("Unknown species '{}', no Ultra Beast spawned.", pokemon);
            return;
        }

        Pokemon ultraBeast = species.create(ULTRA_BEAST_LEVEL);
        ultraBeast.sendOut(
                level,
                pos.getCenter().add(0, 1, 0),
                null,
                pokemonEntity -> {
                    pokemonEntity.setNoAi(true);
                    pokemonEntity.setNoGravity(true);
                    pokemonEntity.setPersistenceRequired();
                    pokemonEntity.setInvulnerable(true);
                    pokemonEntity.setCountsTowardsSpawnCap(false);

                    return null;
                }
        );
    }

    /** Picks a random structure key among the registered configurations. */
    public static String getRandomStructureKey(RandomSource random) {
        // CONFIGS is a non-empty constant, so the old "poipole" fallback for an empty key set
        // could never run.
        return STRUCTURE_KEYS.get(random.nextInt(STRUCTURE_KEYS.size()));
    }

    /**
     * Discards loose items left over from the previous instance.
     * <p>
     * Deliberately does NOT touch the exit portal: it is spawned right after this call, and
     * removing it here would race with that.
     */
    private static void clearItems(ServerLevel level) {
        if (!isUltraSpace(level)) return;

        level.getEntities(EntityType.ITEM, e -> true).forEach(Entity::discard);

        UltraBeasts.LOGGER.info("Ultra-Space items cleared after structure placement.");
    }

    /**
     * One structure-block template of a structure.
     *
     * @param name   template path under {@code data/cobblemon_ultrabeast/structure}
     * @param offset position relative to {@link #STRUCTURE_ORIGIN}
     */
    public record StructurePart(String name, BlockPos offset) {}
}
