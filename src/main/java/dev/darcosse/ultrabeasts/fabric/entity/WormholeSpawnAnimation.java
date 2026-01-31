package dev.darcosse.ultrabeasts.fabric.entity;

import dev.darcosse.ultrabeasts.fabric.registry.ModEntities;
import dev.darcosse.ultrabeasts.fabric.registry.ModSounds;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.decoration.DisplayEntity.BlockDisplayEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.AffineTransformation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3f;
import org.joml.Quaternionf;

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
    private BlockPos targetPos = BlockPos.ORIGIN;
    private boolean blocksSpawned = false;
    private boolean finalEntitySpawned = false;

    private final List<BlockDisplayEntity> flyingBlocks = new ArrayList<>();

    public WormholeSpawnAnimation(EntityType<? extends WormholeSpawnAnimation> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setInvulnerable(true);
    }

    public WormholeSpawnAnimation(EntityType<? extends WormholeSpawnAnimation> type, World world, BlockPos pos) {
        this(type, world);
        this.targetPos = pos;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.getWorld().isClient) return;

        animationTick++;

        if (animationTick <= DESCENT_DURATION) {
            handleDescent();
        } else if (animationTick <= TOTAL_DURATION) {
            if (!blocksSpawned) {
                spawnFlyingBlocks();
                blocksSpawned = true;
                this.getWorld().playSound(null, targetPos, ModSounds.WORMHOLE_ANIMATION_SPAWN, SoundCategory.HOSTILE, 2.0f, 0.4f);
            }
            animateFlyingBlocks();
        } else if (!finalEntitySpawned) {
            cleanupBlocks();
            spawnFinalWormhole();
        }
    }

    /**
     * Phase 1 : Descente de l'entité invisible avec une traînée de fumée opaque.
     */
    private void handleDescent() {
        double progress = (double) animationTick / DESCENT_DURATION;
        double startY = targetPos.getY() + 20;
        double y = startY - (progress * 20);

        this.setPosition(targetPos.getX() + 0.5, y, targetPos.getZ() + 0.5);

        if (this.getWorld() instanceof ServerWorld sw) {
            sw.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, this.getX(), this.getY(), this.getZ(), 6, 0.05, 0.05, 0.05, 0.01);
            sw.spawnParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY(), this.getZ(), 2, 0.1, 0.1, 0.1, 0.02);
            sw.spawnParticles(ParticleTypes.FLAME, this.getX(), this.getY(), this.getZ(), 1, 0.1, 0.1, 0.1, 0.05);
        }
    }

    /**
     * Phase 2 (Initialisation) : Spawn des blocs alentours.
     */
    private void spawnFlyingBlocks() {
        if (!(this.getWorld() instanceof ServerWorld sw)) return;

        List<BlockState> states = List.of(
                Blocks.STONE.getDefaultState(),
                Blocks.DIRT.getDefaultState(),
                Blocks.GRASS_BLOCK.getDefaultState(),
                Blocks.SAND.getDefaultState(),
                Blocks.COBBLESTONE.getDefaultState()
        );

        for (int i = 0; i < MAX_FLYING_BLOCKS; i++) {
            double angle = this.random.nextDouble() * Math.PI * 2;
            double radius = 12 + this.random.nextDouble() * 15;
            double height = this.random.nextDouble() * 10 - 5;

            BlockDisplayEntity display = new BlockDisplayEntity(EntityType.BLOCK_DISPLAY, sw);
            display.addCommandTag(BLOCK_TAG);
            display.setBlockState(states.get(this.random.nextInt(states.size())));

            display.setPosition(
                    targetPos.getX() + 0.5 + Math.cos(angle) * radius,
                    targetPos.getY() + 1.5 + height,
                    targetPos.getZ() + 0.5 + Math.sin(angle) * radius
            );

            display.setNoGravity(true);
            display.setInvulnerable(true);

            float s = 0.4f + this.random.nextFloat() * 0.4f;
            display.setTransformation(new AffineTransformation(null, null, new Vector3f(s, s, s), null));

            sw.spawnEntity(display);
            flyingBlocks.add(display);
        }
    }

    /**
     * Phase 2 & 3 : Animation d'aspiration vers le centre.
     */
    private void animateFlyingBlocks() {
        Vec3d center = new Vec3d(targetPos.getX() + 0.5, targetPos.getY() + 1.5, targetPos.getZ() + 0.5);

        int currentAbsorbTick = animationTick - DESCENT_DURATION;
        int totalAbsorbDuration = BLOCK_COLLECTION_DURATION + ABSORPTION_DURATION;
        double absorbProgress = Math.min(1.0, (double) currentAbsorbTick / totalAbsorbDuration);

        if (flyingBlocks.isEmpty() && this.getWorld() instanceof ServerWorld sw) {
            sw.getEntitiesByType(EntityType.BLOCK_DISPLAY, e -> e.getCommandTags().contains(BLOCK_TAG))
                    .forEach(flyingBlocks::add);
        }

        for (BlockDisplayEntity block : flyingBlocks) {
            if (block.isRemoved()) continue;

            Vec3d currentPos = block.getPos();
            Vec3d toCenter = center.subtract(currentPos);
            double distance = toCenter.length();

            double pullStrength = 0.05 + (absorbProgress * 0.35);

            Vec3d tangent = toCenter.crossProduct(new Vec3d(0, 1, 0)).normalize().multiply(0.25 * (1.0 - absorbProgress));

            Vec3d nextPos = currentPos.add(toCenter.multiply(pullStrength)).add(tangent);
            block.setPosition(nextPos.x, nextPos.y, nextPos.z);

            long seed = block.getUuid().getMostSignificantBits();
            float rotSpeed = 0.1f + ((seed % 100) / 500f);
            Quaternionf quat = new Quaternionf().rotateXYZ(animationTick * rotSpeed, animationTick * (rotSpeed * 1.2f), animationTick * (rotSpeed * 0.8f));

            float currentScale = (float) (0.6f * (1.0 - (absorbProgress * 0.9)));

            block.setInterpolationDuration(1);
            block.setStartInterpolation(0);
            block.setTransformation(new AffineTransformation(null, quat, new Vector3f(currentScale, currentScale, currentScale), null));
        }

        if (this.getWorld() instanceof ServerWorld sw) {
            sw.spawnParticles(ParticleTypes.ENCHANT, center.x, center.y, center.z, 8, 1.2, 1.2, 1.2, 0.1);

            if (animationTick % 2 == 0) {
                sw.spawnParticles(ParticleTypes.SQUID_INK, center.x, center.y, center.z, (int)(5 + (absorbProgress * 15)), 0.1, 0.1, 0.1, 0.02);
            }
        }
    }

    private void cleanupBlocks() {
        if (!(this.getWorld() instanceof ServerWorld sw)) return;

        flyingBlocks.forEach(Entity::discard);
        flyingBlocks.clear();

        sw.getEntitiesByType(EntityType.BLOCK_DISPLAY, e -> e.getCommandTags().contains(BLOCK_TAG))
                .forEach(Entity::discard);

        sw.spawnParticles(ParticleTypes.FLASH, targetPos.getX() + 0.5, targetPos.getY() + 1.5, targetPos.getZ() + 0.5, 1, 0, 0, 0, 0);
    }

    private void spawnFinalWormhole() {
        finalEntitySpawned = true;
        if (this.getWorld() instanceof ServerWorld sw) {
            WormholeEntity wormhole = new WormholeEntity(ModEntities.WORMHOLE, sw);
            wormhole.setPosition(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5);
            sw.spawnEntity(wormhole);
            WormholeEntity.setActiveWormhole(wormhole, sw.getTime());

            sw.playSound(null, targetPos, ModSounds.WORMHOLE_SPAWN, SoundCategory.HOSTILE, 3.0f, 0.8f);
        }
        this.discard();
    }

    @Override protected void initDataTracker(DataTracker.Builder builder) {}

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.animationTick = nbt.getInt("AnimationTick");
        this.blocksSpawned = nbt.getBoolean("BlocksSpawned");
        this.finalEntitySpawned = nbt.getBoolean("FinalSpawned");
        if (nbt.contains("TargetX")) {
            this.targetPos = new BlockPos(nbt.getInt("TargetX"), nbt.getInt("TargetY"), nbt.getInt("TargetZ"));
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("AnimationTick", animationTick);
        nbt.putBoolean("BlocksSpawned", blocksSpawned);
        nbt.putBoolean("FinalSpawned", finalEntitySpawned);
        nbt.putInt("TargetX", targetPos.getX());
        nbt.putInt("TargetY", targetPos.getY());
        nbt.putInt("TargetZ", targetPos.getZ());
    }
}