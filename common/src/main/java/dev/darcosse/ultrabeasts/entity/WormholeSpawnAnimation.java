package dev.darcosse.ultrabeasts.entity;

import com.mojang.math.Transformation;
import dev.darcosse.ultrabeasts.registry.ModEntities;
import dev.darcosse.ultrabeasts.registry.ModSounds;
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

/**
 * Invisible director entity that plays the wormhole's arrival, then replaces itself with a
 * {@link WormholeEntity}.
 * <p>
 * Three phases of {@value #PHASE_DURATION} ticks each: a smoke trail descends onto the spot,
 * debris block displays spawn in a wide ring, then the debris is pulled into the centre and
 * shrinks away. Server side only: the client just receives the vanilla particles and block
 * displays it creates.
 */
public class WormholeSpawnAnimation extends Entity {

    /** Length of each phase, in ticks (3 s). */
    private static final int PHASE_DURATION = 60;

    private static final int DESCENT_DURATION = PHASE_DURATION;
    private static final int BLOCK_COLLECTION_DURATION = PHASE_DURATION;
    private static final int ABSORPTION_DURATION = PHASE_DURATION;
    private static final int TOTAL_DURATION = DESCENT_DURATION + BLOCK_COLLECTION_DURATION + ABSORPTION_DURATION;

    /**
     * Height above the target the descent starts from, in blocks. Also used by
     * {@code WormholeEntity.spawnWormhole} for the entity's initial position, so the smoke
     * trail does not jump on its first tick.
     */
    public static final int DESCENT_HEIGHT = 20;

    /**
     * Debris block displays spawned. Each one is its own entity: position updates every tick
     * plus a transformation update every {@link #TRANSFORM_UPDATE_INTERVAL} ticks, sent to
     * every nearby player.
     */
    private static final int MAX_FLYING_BLOCKS = 80;

    /** Inner radius of the debris ring, in blocks. */
    private static final double DEBRIS_MIN_RADIUS = 12;

    /** Width of the debris ring, in blocks: debris spawns between 12 and 27 blocks out. */
    private static final double DEBRIS_RADIUS_SPREAD = 15;

    /** Vertical spread of the debris around the centre, in blocks (±5). */
    private static final double DEBRIS_HEIGHT_SPREAD = 10;

    /** Height of the convergence point above the target block. */
    private static final double CENTER_HEIGHT = 1.5;

    /** Tag that marks debris displays, so they can be found again after a reload or a crash. */
    public static final String BLOCK_TAG = "wormhole_animation_block";

    /**
     * Transformations are only pushed every N ticks, with the interpolation duration set to
     * the same N so the client fills in the gap.
     * <p>
     * Updating 80 display entities every tick meant 80 entity-data syncs per tick to every
     * nearby player. At 3 the traffic drops by two thirds, and the motion looks smoother
     * because the client interpolates instead of snapping to each new value.
     */
    private static final int TRANSFORM_UPDATE_INTERVAL = 3;

    /** Blocks the debris is drawn from: terrain-looking, so it reads as torn from the ground. */
    private static final List<BlockState> DEBRIS_STATES = List.of(
            Blocks.STONE.defaultBlockState(),
            Blocks.DIRT.defaultBlockState(),
            Blocks.GRASS_BLOCK.defaultBlockState(),
            Blocks.SAND.defaultBlockState(),
            Blocks.COBBLESTONE.defaultBlockState()
    );

    /** World up, used to build the swirl tangent. Allocated once instead of per block per tick. */
    private static final Vec3 UP = new Vec3(0, 1, 0);

    // NBT keys. Renaming one makes animations saved mid-play restart from scratch.
    private static final String NBT_TICK = "AnimationTick";
    private static final String NBT_BLOCKS_SPAWNED = "BlocksSpawned";
    private static final String NBT_FINAL_SPAWNED = "FinalSpawned";
    private static final String NBT_TARGET_X = "TargetX";
    private static final String NBT_TARGET_Y = "TargetY";
    private static final String NBT_TARGET_Z = "TargetZ";

    private int animationTick = 0;
    private BlockPos targetPos = BlockPos.ZERO;
    private boolean blocksSpawned = false;
    private boolean finalEntitySpawned = false;

    /** Debris spawned by this animation. Not persisted: rebuilt from {@link #BLOCK_TAG} after a reload. */
    private final List<Display.BlockDisplay> flyingBlocks = new ArrayList<>();

    /**
     * Set once the tag lookup has run after a reload, so it is not repeated every tick if the
     * debris is genuinely gone.
     */
    private boolean debrisRecovered = false;

    /** Constructor used by the entity type factory, and when loading from a save. */
    public WormholeSpawnAnimation(EntityType<? extends WormholeSpawnAnimation> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setInvulnerable(true);
    }

    /** Creates an animation that will open a wormhole at {@code pos}. */
    public WormholeSpawnAnimation(EntityType<? extends WormholeSpawnAnimation> type, Level level, BlockPos pos) {
        this(type, level);
        this.targetPos = pos;
    }

    /** Advances the animation by one tick and dispatches to the current phase. */
    @Override
    public void tick() {
        super.tick();

        if (!(this.level() instanceof ServerLevel level)) return;

        animationTick++;

        if (animationTick <= DESCENT_DURATION) {
            handleDescent(level);
        } else if (animationTick <= TOTAL_DURATION) {
            if (!blocksSpawned) {
                spawnFlyingBlocks(level);
                blocksSpawned = true;
                level.playSound(null, targetPos, SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 4.0f, 1.0f);
            }
            animateFlyingBlocks(level);
        } else if (!finalEntitySpawned) {
            cleanupBlocks(level);
            spawnFinalWormhole(level);
        }
    }

    /** Phase 1: the invisible entity descends onto the target, trailing smoke. */
    private void handleDescent(ServerLevel level) {
        double progress = (double) animationTick / DESCENT_DURATION;
        double y = (targetPos.getY() + DESCENT_HEIGHT) - (progress * DESCENT_HEIGHT);

        this.setPos(targetPos.getX() + 0.5, y, targetPos.getZ() + 0.5);

        double x = this.getX();
        double z = this.getZ();

        // count > 0: one packet per call carries the whole burst, unlike the portal swirl.
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 6, 0.05, 0.05, 0.05, 0.01);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 2, 0.1, 0.1, 0.1, 0.02);
        level.sendParticles(ParticleTypes.FLAME, x, y, z, 1, 0.1, 0.1, 0.1, 0.05);
    }

    /** Phase 2 set-up: spawns the ring of debris displays. */
    private void spawnFlyingBlocks(ServerLevel level) {
        for (int i = 0; i < MAX_FLYING_BLOCKS; i++) {
            double angle = this.random.nextDouble() * Math.PI * 2;
            double radius = DEBRIS_MIN_RADIUS + this.random.nextDouble() * DEBRIS_RADIUS_SPREAD;
            double height = this.random.nextDouble() * DEBRIS_HEIGHT_SPREAD - DEBRIS_HEIGHT_SPREAD / 2;

            Display.BlockDisplay display = new Display.BlockDisplay(EntityType.BLOCK_DISPLAY, level);
            display.addTag(BLOCK_TAG);
            display.setBlockState(DEBRIS_STATES.get(this.random.nextInt(DEBRIS_STATES.size())));

            display.setPos(
                    targetPos.getX() + 0.5 + Math.cos(angle) * radius,
                    targetPos.getY() + CENTER_HEIGHT + height,
                    targetPos.getZ() + 0.5 + Math.sin(angle) * radius
            );

            display.setNoGravity(true);
            display.setInvulnerable(true);

            display.setTransformationInterpolationDuration(TRANSFORM_UPDATE_INTERVAL);

            float scale = 0.4f + this.random.nextFloat() * 0.4f;
            display.setTransformation(new Transformation(null, null, new Vector3f(scale, scale, scale), null));

            level.addFreshEntity(display);
            flyingBlocks.add(display);
        }
    }

    /** Phases 2 and 3: pulls the debris toward the centre in a tightening swirl. */
    private void animateFlyingBlocks(ServerLevel level) {
        Vec3 center = new Vec3(targetPos.getX() + 0.5, targetPos.getY() + CENTER_HEIGHT, targetPos.getZ() + 0.5);

        int absorbTick = animationTick - DESCENT_DURATION;
        int absorbDuration = BLOCK_COLLECTION_DURATION + ABSORPTION_DURATION;
        double absorbProgress = Math.min(1.0, (double) absorbTick / absorbDuration);

        // After a reload the entity list is empty but the tagged displays survived. Looked up
        // once only: if the debris is really gone, scanning every entity of the level on each
        // remaining tick would be pure waste.
        if (flyingBlocks.isEmpty() && !debrisRecovered) {
            debrisRecovered = true;
            level.getEntities(EntityType.BLOCK_DISPLAY, e -> e.getTags().contains(BLOCK_TAG))
                    .forEach(flyingBlocks::add);
        }

        boolean pushTransform = animationTick % TRANSFORM_UPDATE_INTERVAL == 0;

        // Pull grows and spin fades as the absorption progresses, so the swirl collapses inward.
        double pullStrength = 0.05 + (absorbProgress * 0.35);
        double tangentStrength = 0.25 * (1.0 - absorbProgress);
        float scale = (float) (0.6f * (1.0 - (absorbProgress * 0.9)));

        for (Display.BlockDisplay block : flyingBlocks) {
            if (block.isRemoved()) continue;

            Vec3 currentPos = block.position();
            Vec3 toCenter = center.subtract(currentPos);

            Vec3 tangent = toCenter.cross(UP).normalize().scale(tangentStrength);
            Vec3 nextPos = currentPos.add(toCenter.scale(pullStrength)).add(tangent);

            // Position every tick: ordinary movement packets, and this is what keeps the
            // debris looking like it flows rather than teleports.
            block.setPos(nextPos.x, nextPos.y, nextPos.z);

            if (pushTransform) {
                // Per-block spin speed derived from its UUID: stable across reloads, and no
                // extra field to persist.
                long seed = block.getUUID().getMostSignificantBits();
                float rotSpeed = 0.1f + ((seed % 100) / 500f);

                Quaternionf rotation = new Quaternionf().rotateXYZ(
                        animationTick * rotSpeed,
                        animationTick * rotSpeed * 1.2f,
                        animationTick * rotSpeed * 0.8f);

                block.setTransformation(
                        new Transformation(null, rotation, new Vector3f(scale, scale, scale), null));

                // Restarts the client-side interpolation towards the new transformation. The
                // client only re-anchors its interpolation start when this value is RECEIVED,
                // and the setter force-syncs it even when unchanged. Set once at spawn only
                // (as in 4.0), every later transformation snapped instead of interpolating.
                block.setTransformationInterpolationDelay(0);
            }
        }

        level.sendParticles(ParticleTypes.ENCHANT, center.x, center.y, center.z, 8, 1.2, 1.2, 1.2, 0.1);

        if (animationTick % 2 == 0) {
            level.sendParticles(ParticleTypes.SQUID_INK, center.x, center.y, center.z,
                    (int) (5 + (absorbProgress * 15)), 0.1, 0.1, 0.1, 0.02);
        }
    }

    /** Removes all debris, including strays found by tag, and flashes the centre. */
    private void cleanupBlocks(ServerLevel level) {
        flyingBlocks.forEach(Entity::discard);
        flyingBlocks.clear();

        level.getEntities(EntityType.BLOCK_DISPLAY, e -> e.getTags().contains(BLOCK_TAG))
                .forEach(Entity::discard);

        level.sendParticles(ParticleTypes.FLASH,
                targetPos.getX() + 0.5, targetPos.getY() + CENTER_HEIGHT, targetPos.getZ() + 0.5,
                1, 0, 0, 0, 0);
    }

    /** Spawns the actual wormhole at the target and removes this director entity. */
    private void spawnFinalWormhole(ServerLevel level) {
        finalEntitySpawned = true;

        WormholeEntity wormhole = new WormholeEntity(ModEntities.WORMHOLE, level);
        wormhole.setPos(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5);
        wormhole.setPlacementTime(level.getGameTime());
        level.addFreshEntity(wormhole);

        level.playSound(null, targetPos, ModSounds.WORMHOLE_SPAWN, SoundSource.HOSTILE, 3.0f, 0.8f);

        this.discard();
    }

    /** Nothing to sync: the animation entity itself is never rendered. */
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        this.animationTick = nbt.getInt(NBT_TICK);
        this.blocksSpawned = nbt.getBoolean(NBT_BLOCKS_SPAWNED);
        this.finalEntitySpawned = nbt.getBoolean(NBT_FINAL_SPAWNED);
        if (nbt.contains(NBT_TARGET_X)) {
            this.targetPos = new BlockPos(nbt.getInt(NBT_TARGET_X), nbt.getInt(NBT_TARGET_Y), nbt.getInt(NBT_TARGET_Z));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        nbt.putInt(NBT_TICK, animationTick);
        nbt.putBoolean(NBT_BLOCKS_SPAWNED, blocksSpawned);
        nbt.putBoolean(NBT_FINAL_SPAWNED, finalEntitySpawned);
        nbt.putInt(NBT_TARGET_X, targetPos.getX());
        nbt.putInt(NBT_TARGET_Y, targetPos.getY());
        nbt.putInt(NBT_TARGET_Z, targetPos.getZ());
    }
}
