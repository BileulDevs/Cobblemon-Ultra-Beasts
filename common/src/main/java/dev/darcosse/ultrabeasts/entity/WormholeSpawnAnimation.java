package dev.darcosse.ultrabeasts.entity;

import dev.darcosse.ultrabeasts.registry.ModEntities;
import dev.darcosse.ultrabeasts.registry.ModSounds;
import com.mojang.math.Transformation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class WormholeSpawnAnimation extends Entity {

    private static final int DESCENT_DURATION = 60;
    private static final int BLOCK_COLLECTION_DURATION = 60;
    private static final int ABSORPTION_DURATION = 60;
    private static final int TOTAL_DURATION = DESCENT_DURATION + BLOCK_COLLECTION_DURATION + ABSORPTION_DURATION;

    private static final int MAX_FLYING_BLOCKS = 80;
    public static final String BLOCK_TAG = "wormhole_animation_block";

    private int animationTick = 0;
    private BlockPos targetPos = BlockPos.ZERO;
    private boolean blocksSpawned = false;
    private boolean finalEntitySpawned = false;

    private final List<Display.BlockDisplay> flyingBlocks = new ArrayList<>();

    public WormholeSpawnAnimation(EntityType<? extends WormholeSpawnAnimation> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setInvulnerable(true);
    }

    public WormholeSpawnAnimation(EntityType<? extends WormholeSpawnAnimation> type, Level level, BlockPos pos) {
        this(type, level);
        this.targetPos = pos;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) return;

        animationTick++;

        if (animationTick <= DESCENT_DURATION) {
            handleDescent();
        } else if (animationTick <= TOTAL_DURATION) {
            if (!blocksSpawned) {
                spawnFlyingBlocks();
                blocksSpawned = true;
                this.level().playSound(null, targetPos, SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 4.0f, 1.0f);
            }
            animateFlyingBlocks();
        } else if (!finalEntitySpawned) {
            cleanupBlocks();
            spawnFinalWormhole();
        }
    }

    /**
     * Phase 1: the invisible entity descends, trailing opaque smoke.
     */
    private void handleDescent() {
        double progress = (double) animationTick / DESCENT_DURATION;
        double startY = targetPos.getY() + 20;
        double y = startY - (progress * 20);

        this.setPos(targetPos.getX() + 0.5, y, targetPos.getZ() + 0.5);

        if (this.level() instanceof ServerLevel sw) {
            sw.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, this.getX(), this.getY(), this.getZ(), 6, 0.05, 0.05, 0.05, 0.01);
            sw.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY(), this.getZ(), 2, 0.1, 0.1, 0.1, 0.02);
            sw.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY(), this.getZ(), 1, 0.1, 0.1, 0.1, 0.05);
        }
    }

    /**
     * Phase 2 (setup): spawn the surrounding blocks.
     */
    private void spawnFlyingBlocks() {
        if (!(this.level() instanceof ServerLevel sw)) return;

        List<BlockState> states = List.of(
                Blocks.STONE.defaultBlockState(),
                Blocks.DIRT.defaultBlockState(),
                Blocks.GRASS_BLOCK.defaultBlockState(),
                Blocks.SAND.defaultBlockState(),
                Blocks.COBBLESTONE.defaultBlockState()
        );

        for (int i = 0; i < MAX_FLYING_BLOCKS; i++) {
            double angle = this.random.nextDouble() * Math.PI * 2;
            double radius = 12 + this.random.nextDouble() * 15;
            double height = this.random.nextDouble() * 10 - 5;

            Display.BlockDisplay display = new Display.BlockDisplay(EntityType.BLOCK_DISPLAY, sw);
            display.addTag(BLOCK_TAG);
            display.setBlockState(states.get(this.random.nextInt(states.size())));

            display.setPos(
                    targetPos.getX() + 0.5 + Math.cos(angle) * radius,
                    targetPos.getY() + 1.5 + height,
                    targetPos.getZ() + 0.5 + Math.sin(angle) * radius
            );

            display.setNoGravity(true);
            display.setInvulnerable(true);

            float s = 0.4f + this.random.nextFloat() * 0.4f;
            display.setTransformation(new Transformation(null, null, new Vector3f(s, s, s), null));

            sw.addFreshEntity(display);
            flyingBlocks.add(display);
        }
    }

    /**
     * Phases 2 and 3: suck the blocks toward the centre.
     */
    private void animateFlyingBlocks() {
        Vec3 center = new Vec3(targetPos.getX() + 0.5, targetPos.getY() + 1.5, targetPos.getZ() + 0.5);

        int currentAbsorbTick = animationTick - DESCENT_DURATION;
        int totalAbsorbDuration = BLOCK_COLLECTION_DURATION + ABSORPTION_DURATION;
        double absorbProgress = Math.min(1.0, (double) currentAbsorbTick / totalAbsorbDuration);

        if (flyingBlocks.isEmpty() && this.level() instanceof ServerLevel sw) {
            sw.getEntities(EntityType.BLOCK_DISPLAY, e -> e.getTags().contains(BLOCK_TAG))
                    .forEach(flyingBlocks::add);
        }

        for (Display.BlockDisplay block : flyingBlocks) {
            if (block.isRemoved()) continue;

            Vec3 currentPos = block.position();
            Vec3 toCenter = center.subtract(currentPos);
            double distance = toCenter.length();

            double pullStrength = 0.05 + (absorbProgress * 0.35);

            Vec3 tangent = toCenter.cross(new Vec3(0, 1, 0)).normalize().scale(0.25 * (1.0 - absorbProgress));

            Vec3 nextPos = currentPos.add(toCenter.scale(pullStrength)).add(tangent);
            block.setPos(nextPos.x, nextPos.y, nextPos.z);

            long seed = block.getUUID().getMostSignificantBits();
            float rotSpeed = 0.1f + ((seed % 100) / 500f);
            Quaternionf quat = new Quaternionf().rotateXYZ(animationTick * rotSpeed, animationTick * (rotSpeed * 1.2f), animationTick * (rotSpeed * 0.8f));

            float currentScale = (float) (0.6f * (1.0 - (absorbProgress * 0.9)));

            block.setTransformationInterpolationDuration(1);
            block.setTransformationInterpolationDelay(0);
            block.setTransformation(new Transformation(null, quat, new Vector3f(currentScale, currentScale, currentScale), null));
        }

        if (this.level() instanceof ServerLevel sw) {
            sw.sendParticles(ParticleTypes.ENCHANT, center.x, center.y, center.z, 8, 1.2, 1.2, 1.2, 0.1);

            if (animationTick % 2 == 0) {
                sw.sendParticles(ParticleTypes.SQUID_INK, center.x, center.y, center.z, (int) (5 + (absorbProgress * 15)), 0.1, 0.1, 0.1, 0.02);
            }
        }
    }

    private void cleanupBlocks() {
        if (!(this.level() instanceof ServerLevel sw)) return;

        flyingBlocks.forEach(Entity::discard);
        flyingBlocks.clear();

        sw.getEntities(EntityType.BLOCK_DISPLAY, e -> e.getTags().contains(BLOCK_TAG))
                .forEach(Entity::discard);

        sw.sendParticles(ParticleTypes.FLASH, targetPos.getX() + 0.5, targetPos.getY() + 1.5, targetPos.getZ() + 0.5, 1, 0, 0, 0, 0);
    }

    private void spawnFinalWormhole() {
        finalEntitySpawned = true;
        if (this.level() instanceof ServerLevel sw) {
            WormholeEntity wormhole = new WormholeEntity(ModEntities.WORMHOLE, sw);
            wormhole.setPos(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5);
            sw.addFreshEntity(wormhole);
            WormholeEntity.setActiveWormhole(wormhole, sw.getGameTime());

            sw.playSound(null, targetPos, ModSounds.WORMHOLE_SPAWN, SoundSource.HOSTILE, 3.0f, 0.8f);
        }
        this.discard();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        this.animationTick = nbt.getInt("AnimationTick");
        this.blocksSpawned = nbt.getBoolean("BlocksSpawned");
        this.finalEntitySpawned = nbt.getBoolean("FinalSpawned");
        if (nbt.contains("TargetX")) {
            this.targetPos = new BlockPos(nbt.getInt("TargetX"), nbt.getInt("TargetY"), nbt.getInt("TargetZ"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        nbt.putInt("AnimationTick", animationTick);
        nbt.putBoolean("BlocksSpawned", blocksSpawned);
        nbt.putBoolean("FinalSpawned", finalEntitySpawned);
        nbt.putInt("TargetX", targetPos.getX());
        nbt.putInt("TargetY", targetPos.getY());
        nbt.putInt("TargetZ", targetPos.getZ());
    }
}
