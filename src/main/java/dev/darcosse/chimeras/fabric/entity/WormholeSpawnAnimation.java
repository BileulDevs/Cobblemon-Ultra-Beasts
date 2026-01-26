package dev.darcosse.chimeras.fabric.entity;

import dev.darcosse.chimeras.fabric.Chimeras;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity.BlockDisplayEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.decoration.DisplayEntity;
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
import java.util.Random;

public class WormholeSpawnAnimation extends Entity {

    private static final int DESCENT_DURATION = 60;
    private static final int BLOCK_COLLECTION_DURATION = 40;
    private static final int ABSORPTION_DURATION = 100;
    private static final int TOTAL_DURATION =
            DESCENT_DURATION + BLOCK_COLLECTION_DURATION + ABSORPTION_DURATION;

    private static final int MAX_FLYING_BLOCKS = 200;

    private final Random random = new Random();

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

    public void setTargetPos(BlockPos pos) {
        this.targetPos = pos;
    }

    @Override
    public void tick() {
        super.tick();

        if (getWorld().isClient) return;

        animationTick++;

        if (animationTick <= DESCENT_DURATION) {
            handleDescent();
        } else if (animationTick <= DESCENT_DURATION + BLOCK_COLLECTION_DURATION) {
            if (!blocksSpawned) {
                spawnFlyingBlocks();
                blocksSpawned = true;

                getWorld().playSound(
                        null, targetPos,
                        SoundEvents.ENTITY_WITHER_SPAWN,
                        SoundCategory.HOSTILE,
                        2.0f, 0.4f
                );
            }
            animateFlyingBlocks();
        } else if (animationTick <= TOTAL_DURATION) {
            animateFlyingBlocks();
        } else if (!finalEntitySpawned) {
            cleanupBlocks();
            spawnFinalWormhole();
        }
    }

    // ================= DESCENTE =================

    private void handleDescent() {
        double progress = (double) animationTick / DESCENT_DURATION;
        double startY = targetPos.getY() + 20;
        double y = startY - progress * 20;

        setPosition(targetPos.getX() + 0.5, y, targetPos.getZ() + 0.5);

        if (getWorld() instanceof ServerWorld sw) {
            // Particules de portail principales
            sw.spawnParticles(
                    ParticleTypes.PORTAL,
                    getX(), getY(), getZ(),
                    120,  // Beaucoup plus de particules
                    3.0,  // Zone plus large
                    3.0,
                    3.0,
                    0.8   // Plus de vitesse
            );

            // Ajouter des particules END_ROD pour un effet lumineux
            if (animationTick % 3 == 0) {
                sw.spawnParticles(
                        ParticleTypes.END_ROD,
                        getX(), getY(), getZ(),
                        20,
                        2.0,
                        2.0,
                        2.0,
                        0.1
                );
            }
        }
    }

    // ================= BLOCS VISUELS =================

    private void spawnFlyingBlocks() {
        if (!(getWorld() instanceof ServerWorld sw)) return;

        List<BlockState> blocks = List.of(
                Blocks.STONE.getDefaultState(),
                Blocks.DIRT.getDefaultState(),
                Blocks.GRASS_BLOCK.getDefaultState(),
                Blocks.COBBLESTONE.getDefaultState(),
                Blocks.SAND.getDefaultState()
        );

        for (int i = 0; i < MAX_FLYING_BLOCKS; i++) {

            double angle = random.nextDouble() * Math.PI * 2;
            double radius = 15 + random.nextDouble() * 20;
            double height = random.nextDouble() * 8 - 4;

            double x = targetPos.getX() + 0.5 + Math.cos(angle) * radius;
            double y = targetPos.getY() + 1.5 + height;
            double z = targetPos.getZ() + 0.5 + Math.sin(angle) * radius;

            BlockDisplayEntity display = new BlockDisplayEntity(
                    EntityType.BLOCK_DISPLAY, sw
            );

            display.setBlockState(blocks.get(random.nextInt(blocks.size())));
            display.setPosition(x, y, z);
            display.setNoGravity(true);
            display.setInvulnerable(true);

            float scale = 0.6f + random.nextFloat() * 0.4f;
            display.setTransformation(new AffineTransformation(
                    new Vector3f(),
                    null,
                    new Vector3f(scale, scale, scale),
                    null
            ));

            sw.spawnEntity(display);
            flyingBlocks.add(display);
        }
    }

    // ================= ANIMATION =================

    private void animateFlyingBlocks() {
        Vec3d center = getPos();

        int absorbTick = animationTick - DESCENT_DURATION - BLOCK_COLLECTION_DURATION;
        double progress = Math.max(0, (double) absorbTick / ABSORPTION_DURATION);

        for (BlockDisplayEntity block : flyingBlocks) {
            if (block.isRemoved()) continue;

            Vec3d pos = block.getPos();
            Vec3d toCenter = center.subtract(pos);
            double dist = Math.max(toCenter.length(), 0.5);

            Vec3d radial = toCenter.normalize();
            Vec3d tangent = radial.crossProduct(new Vec3d(0, 1, 0)).normalize();

            double pull = 0.08 + progress * 0.45;

            Vec3d newPos = pos
                    .add(tangent.multiply(0.35))
                    .add(radial.multiply(pull * (20.0 / dist)));

            block.setPosition(newPos.x, newPos.y, newPos.z);
        }

        if (getWorld() instanceof ServerWorld sw && animationTick % 2 == 0) {
            sw.spawnParticles(
                    ParticleTypes.REVERSE_PORTAL,
                    center.x, center.y, center.z,
                    (int) (40 * (1 + progress)),
                    0.4, 0.4, 0.4,
                    0.6
            );
        }
    }

    // ================= CLEANUP =================

    private void cleanupBlocks() {
        if (!(getWorld() instanceof ServerWorld sw)) return;

        for (BlockDisplayEntity block : flyingBlocks) {
            if (!block.isRemoved()) {
                sw.spawnParticles(
                        ParticleTypes.POOF,
                        block.getX(), block.getY(), block.getZ(),
                        8, 0.2, 0.2, 0.2, 0.05
                );
                block.discard();
            }
        }
        flyingBlocks.clear();
    }

    // ================= FINAL =================

    private void spawnFinalWormhole() {
        finalEntitySpawned = true;

        WormholeEntity wormhole = new WormholeEntity(
                ModEntities.WORMHOLE,
                (ServerWorld) getWorld()
        );
        wormhole.setPosition(
                targetPos.getX() + 0.5,
                targetPos.getY(),
                targetPos.getZ() + 0.5
        );

        getWorld().spawnEntity(wormhole);
        WormholeEntity.setActiveWormhole(wormhole, getWorld().getTime());

        getWorld().playSound(
                null, targetPos,
                SoundEvents.BLOCK_BEACON_ACTIVATE,
                SoundCategory.HOSTILE,
                3.0f, 0.8f
        );

        discard();
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {}

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {}

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {}
}
