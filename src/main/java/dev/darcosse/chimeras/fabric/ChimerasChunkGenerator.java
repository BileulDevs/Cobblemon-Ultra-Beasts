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

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ChimerasChunkGenerator extends ChunkGenerator {

    // Codec avec BiomeSource pour éviter les problèmes de sérialisation
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
        // Pas de génération de caves pour cette dimension
    }

    @Override
    public void buildSurface(ChunkRegion region, StructureAccessor structures,
                             NoiseConfig noiseConfig, Chunk chunk) {
        // Surface déjà générée dans populateNoise
    }

    @Override
    public void populateEntities(ChunkRegion region) {
        // Entités Cobblemon à implémenter plus tard
    }

    @Override
    public int getWorldHeight() {
        return 384; // Hauteur standard pour 1.21.1
    }

    @Override
    public CompletableFuture<Chunk> populateNoise(Blender blender, NoiseConfig noiseConfig,
                                                  StructureAccessor structureAccessor,
                                                  Chunk chunk) {
        return CompletableFuture.supplyAsync(() -> generateIsland(chunk));
    }

    @Override
    public int getSeaLevel() {
        return 0; // Pas de niveau de mer dans cette dimension
    }

    @Override
    public int getMinimumY() {
        return -64; // Standard pour 1.21.1
    }

    private Chunk generateIsland(Chunk chunk) {
        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;
        int islandRadius = 64;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunkX * 16 + x;
                int worldZ = chunkZ * 16 + z;
                double distanceFromCenter = Math.sqrt(worldX * worldX + worldZ * worldZ);

                if (distanceFromCenter <= islandRadius) {
                    // Amélioration de la génération avec plus de variation
                    int baseHeight = 60;
                    int maxHeight = 80;
                    double heightFactor = Math.cos(distanceFromCenter / islandRadius * Math.PI / 2);
                    int height = (int) (baseHeight + heightFactor * (maxHeight - baseHeight));

                    // Génération des couches de blocs
                    for (int y = getMinimumY(); y <= height; y++) {
                        BlockState blockState;
                        if (y < height - 3) {
                            blockState = Blocks.END_STONE.getDefaultState();
                        } else if (y < height) {
                            blockState = Blocks.END_STONE.getDefaultState();
                        } else {
                            blockState = Blocks.END_STONE.getDefaultState();
                        }

                        chunk.setBlockState(new BlockPos(x, y, z), blockState, false);
                    }

                    // Génération de végétation avec probabilité réduite
                    if (Math.random() < 0.02) {
                        BlockPos plantPos = new BlockPos(x, height + 1, z);
                        if (chunk.getBlockState(plantPos.down()).isOf(Blocks.END_STONE)) {
                            chunk.setBlockState(plantPos, Blocks.CHORUS_PLANT.getDefaultState(), false);
                        }
                    }

                    // Marqueur central amélioré
                    if (worldX >= -1 && worldX <= 1 && worldZ >= -1 && worldZ <= 1) {
                        for (int y = height + 1; y <= height + 5; y++) {
                            chunk.setBlockState(new BlockPos(x, y, z), Blocks.END_ROD.getDefaultState(), false);
                        }
                    }
                }
            }
        }

        // IMPORTANT: Ne pas assigner de biomes manuellement - laisser la BiomeSource s'en charger
        return chunk;
    }

    @Override
    public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) {
        double distanceFromCenter = Math.sqrt(x * x + z * z);
        if (distanceFromCenter <= 64) {
            double heightFactor = Math.cos(distanceFromCenter / 64 * Math.PI / 2);
            return (int) (60 + heightFactor * 20);
        }
        return getMinimumY(); // Retourner la hauteur minimale au lieu de world.getBottomY()
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
}