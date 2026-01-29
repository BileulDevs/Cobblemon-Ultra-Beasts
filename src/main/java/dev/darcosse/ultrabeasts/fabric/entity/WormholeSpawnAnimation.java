package dev.darcosse.ultrabeasts.fabric.entity;

import dev.darcosse.ultrabeasts.fabric.registry.ModEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.decoration.DisplayEntity.BlockDisplayEntity;
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

import java.util.ArrayList;
import java.util.List;

public class WormholeSpawnAnimation extends Entity {
    private static final int DESCENT_DURATION = 60;
    private static final int BLOCK_COLLECTION_DURATION = 40;
    private static final int ABSORPTION_DURATION = 100;
    private static final int TOTAL_DURATION = DESCENT_DURATION + BLOCK_COLLECTION_DURATION + ABSORPTION_DURATION;

    private static final int MAX_FLYING_BLOCKS = 200;
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
        } else if (animationTick <= DESCENT_DURATION + BLOCK_COLLECTION_DURATION) {
            if (!blocksSpawned) {
                spawnFlyingBlocks();
                blocksSpawned = true;
                this.getWorld().playSound(null, targetPos, SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 2.0f, 0.4f);
            }
            animateFlyingBlocks();
        } else if (animationTick <= TOTAL_DURATION) {
            animateFlyingBlocks();
        } else if (!finalEntitySpawned) {
            cleanupBlocks();
            spawnFinalWormhole();
        }
    }

    private void handleDescent() {
        double progress = (double) animationTick / DESCENT_DURATION;
        double startY = targetPos.getY() + 20;
        double y = startY - progress * 20;

        this.setPosition(targetPos.getX() + 0.5, y, targetPos.getZ() + 0.5);

        if (this.getWorld() instanceof ServerWorld sw) {
            sw.spawnParticles(ParticleTypes.PORTAL, this.getX(), this.getY(), this.getZ(), 80, 2.0, 2.0, 2.0, 0.5);
            if (animationTick % 3 == 0) {
                sw.spawnParticles(ParticleTypes.END_ROD, this.getX(), this.getY(), this.getZ(), 10, 1.0, 1.0, 1.0, 0.1);
            }
        }
    }

    private void spawnFlyingBlocks() {
        if (!(this.getWorld() instanceof ServerWorld sw)) return;

        List<BlockState> states = List.of(
                Blocks.STONE.getDefaultState(),
                Blocks.DIRT.getDefaultState(),
                Blocks.GRASS_BLOCK.getDefaultState(),
                Blocks.COBBLESTONE.getDefaultState(),
                Blocks.SAND.getDefaultState()
        );

        for (int i = 0; i < MAX_FLYING_BLOCKS; i++) {
            double angle = this.random.nextDouble() * Math.PI * 2;
            double radius = 15 + this.random.nextDouble() * 20;
            double height = this.random.nextDouble() * 8 - 4;

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

            float s = 0.5f + this.random.nextFloat() * 0.5f;
            display.setTransformation(new AffineTransformation(null, null, new Vector3f(s, s, s), null));

            sw.spawnEntity(display);
            flyingBlocks.add(display);
        }
    }

    private void animateFlyingBlocks() {
        Vec3d center = this.getPos();
        int absorbTick = animationTick - DESCENT_DURATION - BLOCK_COLLECTION_DURATION;
        double progress = Math.max(0, (double) absorbTick / ABSORPTION_DURATION);

        if (flyingBlocks.isEmpty() && this.getWorld() instanceof ServerWorld sw) {
            sw.getEntitiesByType(EntityType.BLOCK_DISPLAY, e -> e.getCommandTags().contains(BLOCK_TAG))
                    .forEach(flyingBlocks::add);
        }

        for (BlockDisplayEntity block : flyingBlocks) {
            if (block.isRemoved()) continue;

            Vec3d pos = block.getPos();
            Vec3d toCenter = center.subtract(pos);
            double dist = Math.max(toCenter.length(), 0.5);
            Vec3d radial = toCenter.normalize();
            Vec3d tangent = radial.crossProduct(new Vec3d(0, 1, 0)).normalize();
            double pull = 0.08 + progress * 0.45;
            Vec3d newPos = pos.add(tangent.multiply(0.35)).add(radial.multiply(pull * (15.0 / dist)));
            block.setPosition(newPos.x, newPos.y, newPos.z);

            long seed = block.getUuid().getMostSignificantBits();
            float randomSpeedX = ((seed % 100) / 100f) * 0.5f + 0.5f;
            float randomSpeedY = (((seed >> 8) % 100) / 100f) * 0.5f + 0.5f;
            float randomSpeedZ = (((seed >> 16) % 100) / 100f) * 0.5f + 0.5f;

            float direction = (seed % 2 == 0) ? 1.0f : -1.0f;

            float rotX = animationTick * 0.15f * randomSpeedX * direction;
            float rotY = animationTick * 0.20f * randomSpeedY * direction;
            float rotZ = animationTick * 0.10f * randomSpeedZ * direction;

            org.joml.Quaternionf quaternion = new org.joml.Quaternionf()
                    .rotateX(rotX)
                    .rotateY(rotY)
                    .rotateZ(rotZ);

            block.setInterpolationDuration(1);
            block.setStartInterpolation(0);

            Vector3f currentScale = new Vector3f(0.7f, 0.7f, 0.7f);

            block.setTransformation(new AffineTransformation(
                    null,
                    quaternion,
                    currentScale,
                    null
            ));
        }

        if (this.getWorld() instanceof ServerWorld sw && animationTick % 2 == 0) {
            sw.spawnParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z, (int) (30 * (1 + progress)), 0.3, 0.3, 0.3, 0.5);
        }
    }

    private void cleanupBlocks() {
        if (!(this.getWorld() instanceof ServerWorld sw)) return;

        for (BlockDisplayEntity block : flyingBlocks) {
            if (block.isAlive()) {
                sw.spawnParticles(ParticleTypes.POOF, block.getX(), block.getY(), block.getZ(), 5, 0.1, 0.1, 0.1, 0.05);
                block.discard();
            }
        }
        flyingBlocks.clear();

        sw.getEntitiesByType(EntityType.BLOCK_DISPLAY, e -> e.getCommandTags().contains(BLOCK_TAG))
                .forEach(Entity::discard);
    }

    private void spawnFinalWormhole() {
        finalEntitySpawned = true;
        if (this.getWorld() instanceof ServerWorld sw) {
            WormholeEntity wormhole = new WormholeEntity(ModEntities.WORMHOLE, sw);
            wormhole.setPosition(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5);
            sw.spawnEntity(wormhole);
            WormholeEntity.setActiveWormhole(wormhole, sw.getTime());
            sw.playSound(null, targetPos, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.HOSTILE, 3.0f, 0.8f);
        }
        this.discard();
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {}

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