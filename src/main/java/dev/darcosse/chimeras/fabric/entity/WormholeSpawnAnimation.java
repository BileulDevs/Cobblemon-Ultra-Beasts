package dev.darcosse.chimeras.fabric.entity;

import dev.darcosse.chimeras.fabric.Chimeras;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WormholeSpawnAnimation extends Entity {
    private int animationTick = 0;
    private static final int DESCENT_DURATION = 60; // 3 secondes de descente
    private static final int BLOCK_COLLECTION_DURATION = 40; // 2 secondes pour collecter les blocs
    private static final int ABSORPTION_DURATION = 100; // 5 secondes pour aspirer les blocs
    private static final int TOTAL_DURATION = DESCENT_DURATION + BLOCK_COLLECTION_DURATION + ABSORPTION_DURATION;

    private BlockPos targetPos;
    private boolean finalEntitySpawned = false;
    private List<FallingBlockEntity> flyingBlocks = new ArrayList<>();
    private boolean blocksSpawned = false;
    private static final int BLOCK_SAMPLE_RADIUS = 50;
    private static final int MAX_FLYING_BLOCKS = 200; // Nombre de blocs volants

    private final Random random = new Random();

    // Constructeur requis par Fabric
    public WormholeSpawnAnimation(EntityType<? extends WormholeSpawnAnimation> type, World world) {
        super(type, world);
        this.targetPos = BlockPos.ORIGIN;
        this.noClip = true;
        this.setInvulnerable(true);
    }

    // Constructeur utilitaire avec position
    public WormholeSpawnAnimation(EntityType<? extends WormholeSpawnAnimation> type, World world, BlockPos targetPos) {
        this(type, world);
        this.targetPos = targetPos;
    }

    public void setTargetPos(BlockPos pos) {
        this.targetPos = pos;
    }

    @Override
    public void tick() {
        super.tick();

        if (getWorld().isClient) {
            clientAnimation();
        } else {
            serverTick();
        }
    }

    private void serverTick() {
        animationTick++;

        if (animationTick == 1) {
            Chimeras.LOGGER.info("WormholeSpawnAnimation started at " + targetPos);
        }

        // Phase 1: Descente (0-60 ticks)
        if (animationTick <= DESCENT_DURATION) {
            handleDescent();
        }
        // Phase 2: Spawn des blocs volants (60-100 ticks)
        else if (animationTick <= DESCENT_DURATION + BLOCK_COLLECTION_DURATION) {
            if (!blocksSpawned) {
                spawnFlyingBlocks();
                blocksSpawned = true;

                // Son de début d'aspiration
                getWorld().playSound(
                        null,
                        targetPos,
                        SoundEvents.ENTITY_WITHER_SPAWN,
                        SoundCategory.HOSTILE,
                        2.0f,
                        0.3f
                );
            }
            // Anime les blocs pendant cette phase aussi
            animateFlyingBlocks();
        }
        // Phase 3: Absorption des blocs (100-200 ticks)
        else if (animationTick <= TOTAL_DURATION) {
            animateFlyingBlocks();
        }
        // Phase 4: Spawn du wormhole final
        else if (!finalEntitySpawned) {
            Chimeras.LOGGER.info("Spawning final wormhole at " + targetPos);
            cleanupBlocks();
            spawnFinalWormhole();
        }
    }

    private void handleDescent() {
        double progress = (double) animationTick / DESCENT_DURATION;
        double startY = targetPos.getY() + 20;
        double targetY = targetPos.getY();
        double currentY = startY - (progress * (startY - targetY));

        setPosition(targetPos.getX() + 0.5, currentY, targetPos.getZ() + 0.5);

        // Sons intermédiaires
        if (animationTick == 20) {
            getWorld().playSound(
                    null,
                    getBlockPos(),
                    SoundEvents.BLOCK_PORTAL_AMBIENT,
                    SoundCategory.HOSTILE,
                    1.5f,
                    0.6f
            );
        }

        if (animationTick == 40) {
            getWorld().playSound(
                    null,
                    getBlockPos(),
                    SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                    SoundCategory.HOSTILE,
                    2.0f,
                    0.4f
            );
        }

        // Particules de descente
        if (animationTick % 2 == 0) {
            ((ServerWorld) getWorld()).spawnParticles(
                    ParticleTypes.PORTAL,
                    getX(), getY(), getZ(),
                    20,
                    1.5, 1.5, 1.5,
                    0.5
            );

            ((ServerWorld) getWorld()).spawnParticles(
                    ParticleTypes.REVERSE_PORTAL,
                    getX(), getY(), getZ(),
                    10,
                    0.5, 0.5, 0.5,
                    0.1
            );
        }
    }

    private void spawnFlyingBlocks() {
        if (!(getWorld() instanceof ServerWorld serverWorld)) return;

        flyingBlocks.clear();

        Chimeras.LOGGER.info("Spawning flying blocks with multiple types...");

        // Liste fixe de blocs à utiliser
        List<BlockState> groundBlockTypes = new ArrayList<>();
        groundBlockTypes.add(Blocks.STONE.getDefaultState());
        groundBlockTypes.add(Blocks.DIRT.getDefaultState());
        groundBlockTypes.add(Blocks.GRASS_BLOCK.getDefaultState());
        groundBlockTypes.add(Blocks.COBBLESTONE.getDefaultState());
        groundBlockTypes.add(Blocks.SAND.getDefaultState());

        for (int i = 0; i < MAX_FLYING_BLOCKS; i++) {

            // Position aléatoire sur une sphère autour du centre
            double u = random.nextDouble();
            double v = random.nextDouble();
            double theta = u * 2 * Math.PI;
            double phi = Math.acos(2 * v - 1);

            double minRadius = 15;
            double maxRadius = 35;
            double radius = minRadius + Math.cbrt(random.nextDouble()) * (maxRadius - minRadius);

            double sinPhi = Math.sin(phi);
            double spawnX = targetPos.getX() + 0.5 + radius * sinPhi * Math.cos(theta);
            double spawnY = targetPos.getY() + radius * Math.cos(phi);
            double spawnZ = targetPos.getZ() + 0.5 + radius * sinPhi * Math.sin(theta);

            // Bloc choisi aléatoirement
            BlockState blockState = groundBlockTypes.get(random.nextInt(groundBlockTypes.size()));

            // Crée le FallingBlockEntity
            FallingBlockEntity fallingBlock = new FallingBlockEntity(EntityType.FALLING_BLOCK, serverWorld);

            // NBT correct sans .toString()
            NbtCompound nbt = new NbtCompound();
            nbt.put("BlockState", BlockState.CODEC.encodeStart(NbtOps.INSTANCE, blockState).getOrThrow());
            nbt.putBoolean("DropItem", false);
            nbt.putBoolean("HurtEntities", false);
            nbt.putInt("Time", 1);

            fallingBlock.readNbt(nbt);
            fallingBlock.setPosition(spawnX, spawnY, spawnZ);
            fallingBlock.dropItem = false;
            fallingBlock.velocityModified = true;

            // Vitesse tangentielle pour créer l'orbite
            double speed = 0.5;
            Vec3d radial = new Vec3d(spawnX - (targetPos.getX() + 0.5),
                    spawnY - targetPos.getY(),
                    spawnZ - (targetPos.getZ() + 0.5)).normalize();

            Vec3d tangent = radial.crossProduct(new Vec3d(0, 1, 0)).normalize();

            fallingBlock.setVelocity(
                    tangent.x * speed + (random.nextDouble() - 0.5) * 0.1,
                    0.1 + random.nextDouble() * 0.2,
                    tangent.z * speed + (random.nextDouble() - 0.5) * 0.1
            );

            serverWorld.spawnEntity(fallingBlock);
            flyingBlocks.add(fallingBlock);
        }

        Chimeras.LOGGER.info("Spawned " + flyingBlocks.size() + " flying blocks in orbit.");
    }

    private void animateFlyingBlocks() {
        if (!(getWorld() instanceof ServerWorld serverWorld)) return;

        int absorptionTick = animationTick - DESCENT_DURATION - BLOCK_COLLECTION_DURATION;
        double absorptionProgress = Math.max(0, (double) absorptionTick / ABSORPTION_DURATION);

        // Son continu d'aspiration
        if (animationTick % 20 == 0) {
            getWorld().playSound(
                    null,
                    targetPos,
                    SoundEvents.ENTITY_WARDEN_HEARTBEAT,
                    SoundCategory.HOSTILE,
                    1.5f,
                    0.5f + (float) absorptionProgress * 0.5f
            );
        }

        Vec3d center = getPos();
        List<FallingBlockEntity> toRemove = new ArrayList<>();

        for (FallingBlockEntity block : flyingBlocks) {
            if (block.isRemoved() || !block.isAlive()) {
                toRemove.add(block);
                continue;
            }

            // Empêche le bloc de se poser sur le sol
            if (block.isOnGround()) {
                block.setVelocity(block.getVelocity().multiply(0.0)); // bloque sur le sol
            }

            // Force d'aspiration progressive
            double pullStrength = (0.15 + absorptionProgress * 0.4) * (1.0 + (25.0 / Math.max(block.getPos().subtract(center).length(), 2.0)));

            Vec3d toCenter = center.subtract(block.getPos());
            Vec3d radial = toCenter.normalize();

            // Vecteur tangent aléatoire pour rotation sphérique
            Vec3d randomAxis = new Vec3d(Math.random() - 0.5, Math.random() - 0.5, Math.random() - 0.5).normalize();
            Vec3d tangent = radial.crossProduct(randomAxis);
            if (tangent.lengthSquared() < 0.0001) tangent = radial.crossProduct(new Vec3d(1, 0, 0));
            tangent = tangent.normalize().multiply(pullStrength * 0.4);

            // Vélocité combinée vers le centre + rotation
            Vec3d newVel = block.getVelocity()
                    .add(radial.multiply(pullStrength)) // aspiration
                    .add(tangent)
                    .multiply(0.96); // friction légère

            // Limite la vitesse max
            if (newVel.length() > 2.0) newVel = newVel.normalize().multiply(2.0);

            block.setVelocity(newVel);
            block.velocityModified = true;
        }

        // Particules autour du centre
        if (animationTick % 2 == 0) {
            serverWorld.spawnParticles(
                    ParticleTypes.REVERSE_PORTAL,
                    center.x, center.y, center.z,
                    (int)(30 * (1 + absorptionProgress)),
                    0.3, 0.3, 0.3,
                    0.5 + absorptionProgress * 0.5
            );
        }
    }

    private void cleanupBlocks() {
        // Supprime tous les blocs restants avec effet
        for (FallingBlockEntity block : flyingBlocks) {
            if (!block.isRemoved()) {
                ((ServerWorld) getWorld()).spawnParticles(
                        ParticleTypes.POOF,
                        block.getX(), block.getY(), block.getZ(),
                        10,
                        0.2, 0.2, 0.2,
                        0.05
                );
                block.discard();
            }
        }
        flyingBlocks.clear();
    }

    private void clientAnimation() {
        if (animationTick <= DESCENT_DURATION) {
            clientDescentAnimation();
        } else if (animationTick <= TOTAL_DURATION) {
            clientAbsorptionAnimation();
        }
    }

    private void clientDescentAnimation() {
        double progress = (double) animationTick / DESCENT_DURATION;
        double radius = 3.0 * (1 - progress * 0.5);

        // Spirale de particules "sombre"
        for (int i = 0; i < 15; i++) {
            double angle = (animationTick * 15 + i * 24) * Math.PI / 180;
            double spiralRadius = radius * (1 - (i / 15.0) * 0.5);

            double x = getX() + Math.cos(angle) * spiralRadius;
            double z = getZ() + Math.sin(angle) * spiralRadius;
            double y = getY() + (random.nextDouble() - 0.5) * 2;

            // Particule principale sombre
            getWorld().addParticle(
                    ParticleTypes.SMOKE,
                    x, y, z,
                    (getX() - x) * 0.05,
                    -0.05,
                    (getZ() - z) * 0.05
            );

            // Particule secondaire "cendre"
            if (i % 3 == 0) {
                getWorld().addParticle(
                        ParticleTypes.ASH,
                        x, y, z,
                        0, -0.02, 0
                );
            }
        }

        // Vortex central plus sombre
        int centralParticles = (int) (5 + progress * 20);
        for (int i = 0; i < centralParticles; i++) {
            getWorld().addParticle(
                    ParticleTypes.SMOKE,
                    getX() + (random.nextDouble() - 0.5) * radius * 0.3,
                    getY() + (random.nextDouble() - 0.5),
                    getZ() + (random.nextDouble() - 0.5) * radius * 0.3,
                    0, -0.05 - progress * 0.1, 0
            );
        }
    }

    private void clientAbsorptionAnimation() {
        int absorptionTick = animationTick - DESCENT_DURATION - BLOCK_COLLECTION_DURATION;
        double progress = Math.max(0, (double) absorptionTick / ABSORPTION_DURATION);

        // Vortex intense qui grossit
        for (int i = 0; i < 25; i++) {
            double angle = (animationTick * 20 + i * 18) * Math.PI / 180;
            double radius = 5.0 * (1 - progress * 0.2);

            double x = getX() + Math.cos(angle) * radius;
            double z = getZ() + Math.sin(angle) * radius;

            getWorld().addParticle(
                    ParticleTypes.PORTAL,
                    x, getY(), z,
                    (getX() - x) * 0.3,
                    -0.15,
                    (getZ() - z) * 0.3
            );
        }

        // Flash final
        if (absorptionTick >= ABSORPTION_DURATION - 10) {
            for (int i = 0; i < 50; i++) {
                getWorld().addParticle(
                        ParticleTypes.END_ROD,
                        getX(), getY(), getZ(),
                        (random.nextDouble() - 0.5) * 1.2,
                        (random.nextDouble() - 0.5) * 1.2,
                        (random.nextDouble() - 0.5) * 1.2
                );
            }
        }
    }

    private void spawnFinalWormhole() {
        finalEntitySpawned = true;

        WormholeEntity wormhole = new WormholeEntity(ModEntities.WORMHOLE, (ServerWorld) getWorld());
        wormhole.setPosition(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5);
        getWorld().spawnEntity(wormhole);

        WormholeEntity.setActiveWormhole(wormhole, ((ServerWorld) getWorld()).getTime());

        Chimeras.LOGGER.info("Final wormhole spawned successfully!");

        // Son final d'activation
        getWorld().playSound(
                null,
                targetPos,
                SoundEvents.BLOCK_BEACON_ACTIVATE,
                SoundCategory.HOSTILE,
                3.0f,
                0.8f
        );

        // Explosion finale massive
        ((ServerWorld) getWorld()).spawnParticles(
                ParticleTypes.EXPLOSION_EMITTER,
                targetPos.getX() + 0.5,
                targetPos.getY(),
                targetPos.getZ() + 0.5,
                3, 0, 0, 0, 0
        );

        ((ServerWorld) getWorld()).spawnParticles(
                ParticleTypes.PORTAL,
                targetPos.getX() + 0.5,
                targetPos.getY(),
                targetPos.getZ() + 0.5,
                200,
                3.0, 3.0, 3.0,
                2.0
        );

        discard();
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        cleanupBlocks();
        if (!finalEntitySpawned) {
            WormholeEntity.setSpawning(false);
            Chimeras.LOGGER.warn("WormholeSpawnAnimation removed before completion!");
        }
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {}

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.contains("TargetX")) {
            int x = nbt.getInt("TargetX");
            int y = nbt.getInt("TargetY");
            int z = nbt.getInt("TargetZ");
            this.targetPos = new BlockPos(x, y, z);
        }
        if (nbt.contains("AnimationTick")) {
            this.animationTick = nbt.getInt("AnimationTick");
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("TargetX", targetPos.getX());
        nbt.putInt("TargetY", targetPos.getY());
        nbt.putInt("TargetZ", targetPos.getZ());
        nbt.putInt("AnimationTick", animationTick);
    }
}