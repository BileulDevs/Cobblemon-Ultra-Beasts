package dev.darcosse.chimeras.fabric;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.biome.*;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.noise.NoiseConfig;
import dev.darcosse.chimeras.fabric.ChimerasCoreBlock;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ChimerasChunkGenerator extends ChunkGenerator {

    public static final MapCodec<ChimerasChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource)
            ).apply(instance, ChimerasChunkGenerator::new)
    );

    public static final RegistryKey<Biome> CHIMERAS_BIOME_KEY = RegistryKey.of(
            RegistryKeys.BIOME, Identifier.of("cobblemon_chimeras", "chimeras_biome")
    );

    public ChimerasChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> getCodec() {
        return CODEC;
    }

    @Override
    public void carve(ChunkRegion region, long seed, NoiseConfig noiseConfig,
                      BiomeAccess biomeAccess, StructureAccessor structureAccessor,
                      Chunk chunk, GenerationStep.Carver carverStep) {
    }

    @Override
    public void buildSurface(ChunkRegion region, StructureAccessor structures,
                             NoiseConfig noiseConfig, Chunk chunk) {
    }

    @Override
    public void populateEntities(ChunkRegion region) {
    }

    @Override
    public int getWorldHeight() {
        return 384;
    }

    @Override
    public CompletableFuture<Chunk> populateNoise(Blender blender, NoiseConfig noiseConfig,
                                                  StructureAccessor structureAccessor,
                                                  Chunk chunk) {
        return CompletableFuture.supplyAsync(() -> generateIsland(chunk));
    }

    @Override
    public int getSeaLevel() {
        return 0;
    }

    @Override
    public int getMinimumY() {
        return -64;
    }

    private Chunk generateIsland(Chunk chunk) {
        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;
        int islandRadius = 32;
        int centerY = 80;
        int maxThickness = 20;
        int flatTopY = 82;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = getMinimumY(); y < 90; y++) {
                    chunk.setBlockState(new BlockPos(x, y, z), Blocks.AIR.getDefaultState(), false);
                }
            }
        }

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunkX * 16 + x;
                int worldZ = chunkZ * 16 + z;
                double distanceFromCenter = Math.sqrt(worldX * worldX + worldZ * worldZ);

                if (distanceFromCenter <= islandRadius) {
                    double edgeFactor = 1.0 - (distanceFromCenter / islandRadius);

                    int thickness = (int) (maxThickness * edgeFactor);
                    int calculatedTopY = centerY + thickness / 2;

                    int topY = Math.min(calculatedTopY, flatTopY);
                    int bottomY = centerY - thickness / 2;

                    int pointDepth = (int) (thickness * 0.8 * edgeFactor);
                    bottomY -= pointDepth;

                    for (int y = bottomY; y <= topY; y++) {
                        BlockState blockState;

                        if (y >= topY) {
                            blockState = Blocks.POLISHED_BLACKSTONE.getDefaultState();
                        }
                        else if (y >= bottomY + thickness / 4) {
                            blockState = Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.getDefaultState();
                        }
                        else {
                            blockState = Blocks.BLACKSTONE.getDefaultState();
                        }

                        chunk.setBlockState(new BlockPos(x, y, z), blockState, false);
                    }

                    if (distanceFromCenter < islandRadius * 0.5 && Math.random() < 0.03) {
                        int crystalHeight = 2 + (int)(Math.random() * 3);
                        for (int i = 1; i <= crystalHeight; i++) {
                            if (i <= 1) {
                                chunk.setBlockState(new BlockPos(x, flatTopY + i, z), Blocks.PRISMARINE.getDefaultState(), false);
                            } else {
                                chunk.setBlockState(new BlockPos(x, flatTopY + i, z), Blocks.SEA_LANTERN.getDefaultState(), false);
                            }
                        }
                    }

                    if (worldX == 0 && worldZ == 0) {
                        BlockPos pillar = new BlockPos(x, flatTopY + 1, z);
                        chunk.setBlockState(pillar, Blocks.QUARTZ_PILLAR.getDefaultState(), false);

                        BlockPos chimerasCore = new BlockPos(x, flatTopY + 2, z);
                        chunk.setBlockState(chimerasCore, Chimeras.CHIMERAS_CORE_BLOCK.getDefaultState(), false);
                    }
                }

                if (distanceFromCenter > islandRadius + 10 && distanceFromCenter < islandRadius + 25) {
                    if (Math.random() < 0.002) { // Très rare
                        int smallIslandSize = 2 + (int)(Math.random() * 3);
                        int floatingY = centerY + 10 + (int)(Math.random() * 20);

                        for (int dx = -smallIslandSize; dx <= smallIslandSize; dx++) {
                            for (int dz = -smallIslandSize; dz <= smallIslandSize; dz++) {
                                for (int dy = 0; dy <= smallIslandSize; dy++) {
                                    if (x + dx >= 0 && x + dx < 16 && z + dz >= 0 && z + dz < 16) {
                                        double smallDist = Math.sqrt(dx*dx + dz*dz + dy*dy);
                                        if (smallDist <= smallIslandSize) {
                                            chunk.setBlockState(new BlockPos(x + dx, floatingY - dy, z + dz),
                                                    Blocks.END_STONE.getDefaultState(), false);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return chunk;
    }

    @Override
    public void generateFeatures(StructureWorldAccess world, Chunk chunk, StructureAccessor structureAccessor) {
        // Ne rien faire - empêche la génération des structures de l'End
        // (piliers d'obsidienne, plateforme de spawn, etc.)
    }

    @Override
    public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) {
        double distanceFromCenter = Math.sqrt(x * x + z * z);
        if (distanceFromCenter <= 32) { // Rayon ajusté à 32 comme dans generateIsland
            double edgeFactor = 1.0 - (distanceFromCenter / 32);
            edgeFactor = Math.pow(edgeFactor, 0.5);
            int thickness = (int) (4 * edgeFactor); // Même calcul que dans generateIsland
            return 82 + thickness / 2; // Hauteur maximale de 82
        }
        return getMinimumY();
    }

    @Override
    public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
        int height = getHeight(x, z, Heightmap.Type.WORLD_SURFACE_WG, world, noiseConfig);
        int sampleHeight = Math.max(0, height - getMinimumY() + 1);
        BlockState[] states = new BlockState[sampleHeight];

        // Remplir l'échantillon avec les bons blocs
        for (int i = 0; i < sampleHeight; i++) {
            states[i] = Blocks.END_STONE.getDefaultState();
        }

        return new VerticalBlockSample(getMinimumY(), states);
    }

    @Override
    public void getDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {
        text.add("ChimerasDimension Generator");
        text.add("Island Radius: 64 blocks");
    }


    // -20 tp 
}