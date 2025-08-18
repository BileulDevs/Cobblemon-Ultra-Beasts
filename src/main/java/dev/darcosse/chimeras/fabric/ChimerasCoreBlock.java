package dev.darcosse.chimeras.fabric;

import com.cobblemon.mod.common.CobblemonEntities;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.particle.ParticleTypes;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class ChimerasCoreBlock extends Block {

    // Base
    private static final VoxelShape BASE_SHAPE = Block.createCuboidShape(
            6.5, 2, 6,   // from
            10.5, 5.5, 10 // to
    );

    // Core (middle crystal)
    private static final VoxelShape CORE_SHAPE = Block.createCuboidShape(
            4, 4.5, 6.5,
            12, 13.5, 9.5
    );

    // Tip (top crystal)
    private static final VoxelShape TIP_SHAPE = Block.createCuboidShape(
            6, 10, 7,
            10, 16, 9
    );

    // Shard 1 (left side)
    private static final VoxelShape SHARD1_SHAPE = Block.createCuboidShape(
            2, 5, 7,
            6, 9, 8
    );

    // Shard 2 (right side)
    private static final VoxelShape SHARD2_SHAPE = Block.createCuboidShape(
            10, 8, 7,
            14, 12, 8.5
    );

    // Combine all
    private static final VoxelShape CRYSTAL_SHAPE = VoxelShapes.union(
            BASE_SHAPE,
            CORE_SHAPE,
            TIP_SHAPE,
            SHARD1_SHAPE,
            SHARD2_SHAPE
    );

    public ChimerasCoreBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onBlockAdded(state, world, pos, oldState, notify);

        if (!world.isClient) {
            world.scheduleBlockTick(pos, this, 100); // 100 ticks = 5 secondes
        }
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, net.minecraft.util.math.random.Random random) {
        // Joue un son de beacon
        world.playSound(
                null, // null = tout le monde entend
                pos,
                SoundEvents.BLOCK_BEACON_AMBIENT, // Son du beacon
                SoundCategory.BLOCKS,
                1.0f, // volume
                1.0f  // pitch
        );

        // Reprogramme un nouveau tick dans 5 secondes
        world.scheduleBlockTick(pos, this, 100);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return CRYSTAL_SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return CRYSTAL_SHAPE;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player,
                                 BlockHitResult hit) {
        if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) {
            summonChimera(world, pos);
            destroyChimeraCore(world, pos, serverPlayer);
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }



    private void summonChimera(World world, BlockPos pos) {
        List<Species> ultraBeasts = PokemonSpecies.INSTANCE.getSpecies()
                .stream()
                .filter(species -> species.create(1).isUltraBeast())
                .collect(Collectors.toList());

        if (ultraBeasts.isEmpty()) {
            return;
        }

        Random random = new Random();
        Species randomUltraBeast = ultraBeasts.get(random.nextInt(ultraBeasts.size()));

        Pokemon chimera = randomUltraBeast.create(80);

        chimera.sendOut(
                (ServerWorld) world,
                pos.toCenterPos().add(0, 1, 0),
                null,
                pokemonEntity -> {
                    return null;
                }
        );
    }

    private void destroyChimeraCore(World world, BlockPos pos, ServerPlayerEntity player) {
        player.playSoundToPlayer(
                SoundEvents.ENTITY_GENERIC_EXPLODE.value(),
                SoundCategory.BLOCKS,
                1.0F,
                1.0F
        );

        if (world instanceof ServerWorld serverWorld) {
            serverWorld.getPlayers().forEach(serverPlayer -> {
                if (serverPlayer.squaredDistanceTo(pos.getX(), pos.getY(), pos.getZ()) < 64 * 64) {
                    serverPlayer.playSoundToPlayer(
                            SoundEvents.ENTITY_GENERIC_EXPLODE.value(),
                            SoundCategory.BLOCKS,
                            1.0F,
                            1.0F
                    );
                }
            });

            serverWorld.spawnParticles(
                    ParticleTypes.EXPLOSION,
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    1,
                    0.0, 0.0, 0.0,
                    0.0
            );
        }

        world.breakBlock(pos, false);
    }
}