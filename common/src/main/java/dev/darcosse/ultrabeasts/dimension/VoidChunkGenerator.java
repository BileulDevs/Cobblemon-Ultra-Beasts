package dev.darcosse.ultrabeasts.dimension;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Chunk generator for Ultra-Space: generates nothing at all. Every block of the dimension
 * comes from the structure templates placed by {@code UltraSpaceStructureManager}.
 * Registered by each loader under {@code cobblemon_ultrabeast:void_generator}.
 */
public class VoidChunkGenerator extends ChunkGenerator {

    /** Bottom of the generated column. Nothing is generated, so it only shapes the empty column. */
    private static final int MIN_Y = 0;

    /**
     * Height of the generated column. Same height as the dimension type (384), although its
     * min_y is -64 rather than {@link #MIN_Y}; harmless while nothing is generated.
     */
    private static final int GEN_DEPTH = 384;

    /** Reused by getBaseColumn: an all-null column is what "nothing here" looks like. */
    private static final BlockState[] EMPTY_COLUMN = new BlockState[GEN_DEPTH - MIN_Y];

    /** Creates the generator; the biome source still decides the (single) biome. */
    public VoidChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    /** Codec referenced by the dimension JSON. Only the biome source is serialised. */
    public static final MapCodec<VoidChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource)
            ).apply(instance, VoidChunkGenerator::new)
    );

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    /**
     * A freshly created chunk is already full of air, so there is nothing to do.
     * <p>
     * The previous implementation looped 16 x 16 x 384 and called setBlockState
     * for every single position — 98 304 no-op writes per chunk, on the world
     * generation threads.
     */
    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState,
                                                        StructureManager structureManager, ChunkAccess chunk) {
        return CompletableFuture.completedFuture(chunk);
    }

    /** An empty column for every caller that samples the terrain. */
    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState randomState) {
        return new NoiseColumn(MIN_Y, EMPTY_COLUMN);
    }

    /** No caves in a void. */
    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState randomState,
                             BiomeManager biomeManager, StructureManager structureManager,
                             ChunkAccess chunk, GenerationStep.Carving carving) {
    }

    /** No surface layer in a void. */
    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structureManager,
                             RandomState randomState, ChunkAccess chunk) {
    }

    /** No initial mobs: the only creature is the structure's Ultra Beast. */
    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
    }

    /** Height of the generated column. */
    @Override
    public int getGenDepth() {
        return GEN_DEPTH;
    }

    /** No sea: the value only matters to features, and none are generated. */
    @Override
    public int getSeaLevel() {
        return 0;
    }

    /** Bottom of the generated column. */
    @Override
    public int getMinY() {
        return MIN_Y;
    }

    /** Every column is empty, so the surface is at the very bottom. */
    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState randomState) {
        return MIN_Y;
    }

    /** Nothing worth showing on the F3 screen. */
    @Override
    public void addDebugScreenInfo(List<String> info, RandomState randomState, BlockPos pos) {
    }
}
