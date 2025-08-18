package dev.darcosse.chimeras.fabric;

import com.cobblemon.mod.common.CobblemonEntities;
import dev.darcosse.chimeras.fabric.config.ConfigManager;
import dev.darcosse.chimeras.fabric.registry.ModSounds;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionTypes;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class ChimerasPortalBlock extends Block {
    private static final VoxelShape SHAPE = createShape();

    private static BlockPos activePortalPos = null;
    private static ServerWorld activePortalWorld = null;
    private static long portalPlacementTime = 0;

    private static final int LIFESPAN_TICKS = 1200;
    private static final int SOUND_INTERVAL = 200;

    public static final Map<UUID, BlockPos> savedPositions = new HashMap<>();

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
        if (!world.getRegistryKey().equals(World.OVERWORLD)) {
            return;
        }

        if (hasActivePortal(world)) {
            return;
        }

        if (random.nextInt(ConfigManager.getWormholeSpawnChance()) != 0) {
            return;
        }

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

        BlockState state = world.getBlockState(activePortalPos);
        if (!(state.getBlock() instanceof ChimerasPortalBlock)) {
            clearActivePortal();
            return false;
        }

        return true;
    }

    /**
     * Trouve une position valide pour faire spawner le portail
     */
    private static BlockPos findValidSpawnLocation(ServerWorld world, Random random) {
        if (world.getPlayers().isEmpty()) {
            return null;
        }

        var players = world.getPlayers();
        var randomPlayer = players.get(random.nextInt(players.size()));
        BlockPos playerPos = randomPlayer.getBlockPos();

        int searchRadius = 20;
        int attempts = 30;
        int minHeightAboveGround = 6;

        for (int i = 0; i < attempts; i++) {
            int x = playerPos.getX() + random.nextInt(searchRadius * 2) - searchRadius;
            int z = playerPos.getZ() + random.nextInt(searchRadius * 2) - searchRadius;

            BlockPos surfacePos = world.getTopPosition(Heightmap.Type.WORLD_SURFACE, new BlockPos(x, world.getTopY(), z));

            BlockPos spawnPos = surfacePos.up(minHeightAboveGround);

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
        for (int i = 1; i <= airBlocksBelow; i++) {
            BlockPos checkPos = pos.down(i);
            if (!world.getBlockState(checkPos).isAir()) {
                return false;
            }
        }

        for (int i = 0; i < 3; i++) {
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
        world.setBlockState(pos, Chimeras.CHIMERAS_PORTAL_BLOCK.getDefaultState());

        world.playSound(
                null,
                pos,
                ModSounds.PORTAL_SPAWN,
                SoundCategory.BLOCKS,
                6.0f,
                0.8f
        );

        activePortalPos = pos;
        activePortalWorld = world;
        portalPlacementTime = world.getTime();

        ServerPlayerEntity nearestPlayer = (ServerPlayerEntity) world.getClosestPlayer(
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                64.0,
                false
        );

        if (nearestPlayer != null) {
            nearestPlayer.sendMessage(
                    Text.translatable("message.cobblemon_chimeras.portal_spawn"),
                    true
            );
        }
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

        // Élément 1
        shape = VoxelShapes.union(shape, Block.createCuboidShape(2.5, 1, 6, 10.5, 5.5, 10));

        // Élément 2
        shape = VoxelShapes.union(shape, Block.createCuboidShape(4, 1.5, 6.5, 13, 13.5, 9.5));

        // Élément 3
        shape = VoxelShapes.union(shape, Block.createCuboidShape(6, 10, 7, 15, 15, 9));

        // Élément 4
        shape = VoxelShapes.union(shape, Block.createCuboidShape(1, 7, 7, 5, 16, 8));

        // Élément 5
        shape = VoxelShapes.union(shape, Block.createCuboidShape(10, 3, 7, 15, 8, 8.5));

        return shape;
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onBlockAdded(state, world, pos, oldState, notify);

        if (!world.isClient) {
            ServerWorld serverWorld = (ServerWorld) world;

            if (!world.getDimensionEntry().matchesKey(DimensionTypes.OVERWORLD)) {
                world.setBlockState(pos, Blocks.AIR.getDefaultState());
                return;
            }

            if (hasActivePortal(serverWorld) && !pos.equals(activePortalPos)) {
                world.setBlockState(pos, Blocks.AIR.getDefaultState());
                return;
            }

            activePortalPos = pos;
            activePortalWorld = serverWorld;
            portalPlacementTime = world.getTime();

            world.scheduleBlockTick(pos, this, SOUND_INTERVAL);
        }
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        super.onStateReplaced(state, world, pos, newState, moved);

        if (!world.isClient && pos.equals(activePortalPos)) {
            clearActivePortal();
        }
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, net.minecraft.util.math.random.Random random) {
        if (!pos.equals(activePortalPos)) {
            return;
        }

        long currentTime = world.getTime();
        long elapsed = currentTime - portalPlacementTime;

        if (elapsed >= LIFESPAN_TICKS) {
            world.playSound(
                    null,
                    pos,
                    ModSounds.PORTAL_AMBIENT,
                    SoundCategory.BLOCKS,
                    3.0f,
                    0.8f
            );

            world.setBlockState(pos, Blocks.AIR.getDefaultState());
            clearActivePortal();
            return;
        }

        world.playSound(
                null,
                pos,
                ModSounds.PORTAL_AMBIENT,
                SoundCategory.BLOCKS,
                3.0f,
                1.0f
        );

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
            if (player.getWorld().getRegistryKey().equals(World.OVERWORLD)) {
                savedPositions.put(player.getUuid(), player.getBlockPos());
            }

            checkAndPlaceChimerasCore(chimerasWorld);
            killAllPokemonsOfWorld(chimerasWorld);
            grantChimerasAdvancement(player);

            player.teleport(chimerasWorld, 0.5, 83, -19.5, player.getYaw(), player.getPitch());
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

    private void grantChimerasAdvancement(ServerPlayerEntity player) {
        Identifier advancementId = Identifier.of("cobblemon_chimeras", "enter_chimeras_dimension");

        AdvancementEntry advancement = player.getServer().getAdvancementLoader().get(advancementId);

        if (advancement != null) {
            AdvancementProgress progress = player.getAdvancementTracker().getProgress(advancement);

            if (!progress.isDone()) {
                for (String criterion : progress.getUnobtainedCriteria()) {
                    player.getAdvancementTracker().grantCriterion(advancement, criterion);
                }
            }
        }
    }
}