package dev.darcosse.chimeras.fabric;

import com.cobblemon.mod.common.CobblemonEntities;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionTypes;
import net.minecraft.registry.RegistryKey;

import java.util.Random;

public class ChimerasPortalBlock extends Block {
    private static final VoxelShape SHAPE = createShape();

    // Variables statiques pour gérer l'instance unique
    private static BlockPos activePortalPos = null;
    private static ServerWorld activePortalWorld = null;
    private static long portalPlacementTime = 0;

    // Configuration
    private static final int LIFESPAN_TICKS = 1200; // 60 secondes (20 ticks/sec * 60)
    private static final int SOUND_INTERVAL = 100; // 5 secondes entre les sons
    private static final int SPAWN_CHANCE = 5000;

    public ChimerasPortalBlock(Settings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    /**
     * Méthode statique pour tenter de faire spawner un portail aléatoirement
     * Appelez cette méthode depuis un système de ticks global ou un événement
     */
    public static void tryRandomSpawn(ServerWorld world, Random random) {
        // Vérifier si on est dans l'overworld
        if (!world.getRegistryKey().equals(World.OVERWORLD)) {
            return;
        }

        // Vérifier s'il y a déjà un portail actif
        if (hasActivePortal(world)) {
            return;
        }

        // Vérifier la chance de spawn (1 chance sur SPAWN_CHANCE)
        if (random.nextInt(SPAWN_CHANCE) != 0) {
            System.out.println("chance tested");
            return;
        }

        System.out.println("Portal spawn triggered! (1/" + SPAWN_CHANCE + " chance)");

        // Trouver une position aléatoire valide
        BlockPos spawnPos = findValidSpawnLocation(world, random);
        if (spawnPos != null) {
            spawnPortal(world, spawnPos);
        }
    }


    /**
     * Vérifie s'il y a un portail actif dans le monde
     */
    public static boolean hasActivePortal(ServerWorld world) {
        if (activePortalPos == null || activePortalWorld != world) {
            return false;
        }

        // Vérifier si le bloc existe encore à cette position
        BlockState state = world.getBlockState(activePortalPos);
        if (!(state.getBlock() instanceof ChimerasPortalBlock)) {
            // Le portail n'existe plus, nettoyer les variables
            clearActivePortal();
            return false;
        }

        return true;
    }

    /**
     * Trouve une position valide pour faire spawner le portail
     */
    private static BlockPos findValidSpawnLocation(ServerWorld world, Random random) {
        // Obtenir des joueurs connectés pour spawner près d'eux
        if (world.getPlayers().isEmpty()) {
            return null;
        }

        // Choisir un joueur aléatoire
        var players = world.getPlayers();
        var randomPlayer = players.get(random.nextInt(players.size()));
        BlockPos playerPos = randomPlayer.getBlockPos();

        // Chercher dans un rayon de 20 blocs autour du joueur
        int searchRadius = 20;
        int attempts = 30;
        int minHeightAboveGround = 6; // 6 blocs d'air minimum sous le portail

        for (int i = 0; i < attempts; i++) {
            int x = playerPos.getX() + random.nextInt(searchRadius * 2) - searchRadius;
            int z = playerPos.getZ() + random.nextInt(searchRadius * 2) - searchRadius;

            // Trouver la surface du monde à cette position
            BlockPos surfacePos = world.getTopPosition(Heightmap.Type.WORLD_SURFACE, new BlockPos(x, world.getTopY(), z));

            // Calculer la position de spawn à 6+ blocs au-dessus de la surface
            BlockPos spawnPos = surfacePos.up(minHeightAboveGround);

            // Vérifier si la position est valide (6 blocs d'air en dessous + 2 au-dessus)
            if (isValidAirSpawnLocation(world, spawnPos, minHeightAboveGround)) {
                return spawnPos;
            }
        }

        return null;
    }

    /**
     * Vérifie si une position est valide pour le spawn en l'air
     */
    private static boolean isValidAirSpawnLocation(ServerWorld world, BlockPos pos, int airBlocksBelow) {
        // Vérifier l'espace en dessous (pour que le portail soit suspendu)
        for (int i = 1; i <= airBlocksBelow; i++) {
            BlockPos checkPos = pos.down(i);
            if (!world.getBlockState(checkPos).isAir()) {
                return false;
            }
        }

        // Vérifier l'espace au-dessus (pour que le portail ne soit pas obstrué)
        for (int i = 0; i < 3; i++) { // 3 blocs de hauteur pour le portail
            BlockPos checkPos = pos.up(i);
            if (!world.getBlockState(checkPos).isAir()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Fait spawner le portail à la position donnée
     */
    private static void spawnPortal(ServerWorld world, BlockPos pos) {
        // Placer le bloc
        world.setBlockState(pos, Chimeras.CHIMERAS_PORTAL_BLOCK.getDefaultState());

        // Effet visuel et sonore pour le spawn
        world.playSound(
                null,
                pos,
                SoundEvents.ENTITY_ENDER_DRAGON_GROWL,
                SoundCategory.BLOCKS,
                3.0f,  // Volume augmenté (porte naturellement plus loin)
                0.8f
        );

        // Enregistrer comme portail actif
        activePortalPos = pos;
        activePortalWorld = world;
        portalPlacementTime = world.getTime();
    }

    /**
     * Nettoie les variables du portail actif
     */
    private static void clearActivePortal() {
        activePortalPos = null;
        activePortalWorld = null;
        portalPlacementTime = 0;
    }

    private static VoxelShape createShape() {
        VoxelShape shape = VoxelShapes.empty();

        // Main frame elements (outer border)
        shape = VoxelShapes.union(shape, Block.createCuboidShape(1, 1, 14, 2, 14, 15)); // Left vertical
        shape = VoxelShapes.union(shape, Block.createCuboidShape(2, 1, 14, 14, 2, 15)); // Bottom horizontal
        shape = VoxelShapes.union(shape, Block.createCuboidShape(14, 1, 14, 15, 14, 15)); // Right vertical
        shape = VoxelShapes.union(shape, Block.createCuboidShape(1, 14, 14, 15, 15, 15)); // Top horizontal

        // Inner frame elements (second layer)
        shape = VoxelShapes.union(shape, Block.createCuboidShape(2, 2, 13, 3, 9, 14)); // Left inner vertical
        shape = VoxelShapes.union(shape, Block.createCuboidShape(3, 2, 13, 14, 3, 14)); // Bottom inner horizontal
        shape = VoxelShapes.union(shape, Block.createCuboidShape(13, 3, 13, 14, 13, 14)); // Right inner vertical
        shape = VoxelShapes.union(shape, Block.createCuboidShape(2, 13, 13, 14, 14, 14)); // Top inner horizontal
        shape = VoxelShapes.union(shape, Block.createCuboidShape(2, 9, 13, 3, 13, 14)); // Left inner vertical upper

        // Third layer frame
        shape = VoxelShapes.union(shape, Block.createCuboidShape(3, 3, 12, 12, 4, 13)); // Bottom third layer
        shape = VoxelShapes.union(shape, Block.createCuboidShape(3, 4, 12, 4, 12, 13)); // Left third layer
        shape = VoxelShapes.union(shape, Block.createCuboidShape(12, 3, 12, 13, 12, 13)); // Right third layer
        shape = VoxelShapes.union(shape, Block.createCuboidShape(3, 12, 12, 13, 13, 13)); // Top third layer

        // Fourth layer frame
        shape = VoxelShapes.union(shape, Block.createCuboidShape(4, 4, 11, 11, 5, 12)); // Bottom fourth layer
        shape = VoxelShapes.union(shape, Block.createCuboidShape(4, 5, 11, 5, 10, 12)); // Left fourth layer
        shape = VoxelShapes.union(shape, Block.createCuboidShape(11, 4, 11, 12, 11, 12)); // Right fourth layer
        shape = VoxelShapes.union(shape, Block.createCuboidShape(4, 11, 11, 12, 12, 12)); // Top fourth layer

        // Main portal structure (depth layers)
        shape = VoxelShapes.union(shape, Block.createCuboidShape(5, 5, 8, 11, 11, 11)); // Main portal area
        shape = VoxelShapes.union(shape, Block.createCuboidShape(6, 5, 8, 10, 6, 11)); // Bottom connector
        shape = VoxelShapes.union(shape, Block.createCuboidShape(5, 6, 8, 6, 10, 11)); // Left connector
        shape = VoxelShapes.union(shape, Block.createCuboidShape(10, 5, 8, 11, 10, 11)); // Right connector

        // Portal center elements (decorative inner parts)
        shape = VoxelShapes.union(shape, Block.createCuboidShape(6, 5, 8, 10, 11, 11)); // Center area

        // Front decorative elements (the complex cross-like structure)
        shape = VoxelShapes.union(shape, Block.createCuboidShape(6, 6, 4, 7, 7, 8)); // Main cross horizontal
        shape = VoxelShapes.union(shape, Block.createCuboidShape(6, 7, 4, 7, 9, 7)); // Cross vertical part
        shape = VoxelShapes.union(shape, Block.createCuboidShape(7, 6, 4, 9, 7, 8)); // Cross extension
        shape = VoxelShapes.union(shape, Block.createCuboidShape(9, 6, 4, 10, 9, 8)); // Right cross part
        shape = VoxelShapes.union(shape, Block.createCuboidShape(6, 9, 4, 10, 10, 8)); // Top cross part
        shape = VoxelShapes.union(shape, Block.createCuboidShape(7, 9, 4, 9, 10, 8)); // Top cross extension

        // Additional front projection
        shape = VoxelShapes.union(shape, Block.createCuboidShape(7, 7, 1, 9, 9, 4)); // Front projection block

        return shape;
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onBlockAdded(state, world, pos, oldState, notify);

        if (!world.isClient) {
            ServerWorld serverWorld = (ServerWorld) world;

            // Vérifier si on est dans l'overworld
            if (!world.getDimensionEntry().matchesKey(DimensionTypes.OVERWORLD)) {
                // Si pas dans l'overworld, supprimer le bloc immédiatement
                world.setBlockState(pos, Blocks.AIR.getDefaultState());
                return;
            }

            // Si il y a déjà un portail actif ailleurs, supprimer celui-ci
            if (hasActivePortal(serverWorld) && !pos.equals(activePortalPos)) {
                world.setBlockState(pos, Blocks.AIR.getDefaultState());
                return;
            }

            // Enregistrer comme portail actif
            activePortalPos = pos;
            activePortalWorld = serverWorld;
            portalPlacementTime = world.getTime();

            // Programmer les sons et la disparition
            world.scheduleBlockTick(pos, this, SOUND_INTERVAL);
        }
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        super.onStateReplaced(state, world, pos, newState, moved);

        if (!world.isClient && pos.equals(activePortalPos)) {
            // Nettoyer les variables si c'est le portail actif qui est supprimé
            clearActivePortal();
        }
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, net.minecraft.util.math.random.Random random) {
        // Vérifier si c'est encore le portail actif
        if (!pos.equals(activePortalPos)) {
            return;
        }

        long currentTime = world.getTime();
        long elapsed = currentTime - portalPlacementTime;

        // Vérifier si le portail a atteint sa durée de vie maximale
        if (elapsed >= LIFESPAN_TICKS) {
            // Jouer un son de disparition
            world.playSound(
                    null,
                    pos,
                    SoundEvents.BLOCK_PORTAL_AMBIENT,
                    SoundCategory.BLOCKS,
                    3.0f,
                    0.8f
            );

            // Supprimer le bloc
            world.setBlockState(pos, Blocks.AIR.getDefaultState());
            clearActivePortal();
            return;
        }

        // Jouer le son périodique
        world.playSound(
                null,
                pos,
                SoundEvents.ENTITY_PLAYER_LEVELUP,
                SoundCategory.BLOCKS,
                3.0f,
                1.0f
        );

        // Programmer le prochain tick
        world.scheduleBlockTick(pos, this, SOUND_INTERVAL);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player,
                                 BlockHitResult hit) {
        if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) {
            teleportToChimerasDimension(serverPlayer);

            world.setBlockState(pos, Blocks.AIR.getDefaultState());

            world.playSound(
                    null,
                    pos,
                    SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                    SoundCategory.BLOCKS,
                    1.0f,
                    1.0f
            );
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    @Override
    public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
        if (!world.isClient && entity instanceof ServerPlayerEntity serverPlayer) {
            teleportToChimerasDimension(serverPlayer);
        }
    }

    private void teleportToChimerasDimension(ServerPlayerEntity player) {
        ServerWorld chimerasWorld = player.getServer().getWorld(Chimeras.CHIMERAS_DIMENSION);
        if (chimerasWorld != null) {
            checkAndPlaceChimerasCore(chimerasWorld);
            killAllPokemonsOfWorld(chimerasWorld);
            player.teleport(chimerasWorld, -0, 83, -22, player.getYaw(), player.getPitch());
            player.sendMessage(Text.translatable("dimension.travel.chimeras"), false);
        }
    }

    public static void checkAndPlaceChimerasCore(ServerWorld world) {
        BlockPos centerPos = new BlockPos(0, 84, 0);

        boolean coreExists = false;
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos checkPos = centerPos.add(x, 0, z);
                if (world.getBlockState(checkPos).isOf(Chimeras.CHIMERAS_CORE_BLOCK)) {
                    coreExists = true;
                    break;
                }
            }
            if (coreExists) break;
        }

        if (!coreExists) {
            world.setBlockState(centerPos, Chimeras.CHIMERAS_CORE_BLOCK.getDefaultState());
        }
    }

    public static void killAllPokemonsOfWorld(ServerWorld chimerasWorld) {
        if (chimerasWorld != null) {
            chimerasWorld.getEntitiesByType(CobblemonEntities.POKEMON, pokemonEntity -> {
                pokemonEntity.discard();
                return false;
            });
        }
    }
}