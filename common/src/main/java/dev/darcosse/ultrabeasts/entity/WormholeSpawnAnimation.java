package dev.darcosse.ultrabeasts.entity;

import dev.darcosse.ultrabeasts.UltraBeasts;
import dev.darcosse.ultrabeasts.fx.WormholeFxBridge;
import dev.darcosse.ultrabeasts.fx.WormholeImpactLevers;
import dev.darcosse.ultrabeasts.registry.ModEntities;
import dev.darcosse.ultrabeasts.registry.ModParticles;
import dev.darcosse.ultrabeasts.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Invisible director entity that plays the wormhole's arrival as a tear in space, then
 * replaces itself with a {@link WormholeEntity} at the same spot.
 * <ol>
 *   <li>CRACK ({@value #CRACK_TICKS} ticks): a jagged line of light grows up and down from
 *   a point, crackling;</li>
 *   <li>OPEN ({@value #OPEN_TICKS} ticks): the line splits into a lens that opens onto the
 *   wormhole behind it, throwing out shards of tinted glass;</li>
 *   <li>SPIRAL ({@value #SPIRAL_TICKS} ticks): the lens turns on itself and rounds into the
 *   portal's disc while the swirl starts; the wormhole then takes over seamlessly.</li>
 * </ol>
 * The server only keeps time, plays the sounds and spawns the wormhole. Everything visible
 * is drawn by each client: the tear by {@code WormholeSpawnAnimationRenderer}, the particles
 * by {@link #clientTick}. Up to 4.0 the animation sent particle packets every tick and moved
 * 80 block display entities, all through the network; it now sends one start time, once.
 */
public class WormholeSpawnAnimation extends Entity {

    /** Length of the crack phase, in ticks. Shared with the renderer. */
    public static final int CRACK_TICKS = 60;

    /** Length of the opening phase, in ticks. Shared with the renderer. */
    public static final int OPEN_TICKS = 45;

    /** Length of the spiral phase, in ticks. Shared with the renderer. */
    public static final int SPIRAL_TICKS = 45;

    /** When the opening starts, in ticks since the animation began. */
    public static final int OPEN_AT = CRACK_TICKS;

    /** When the spiral starts, in ticks since the animation began. */
    public static final int SPIRAL_AT = CRACK_TICKS + OPEN_TICKS;

    /**
     * Whole animation, in ticks (7.5 s). The wormhole appears on this tick. The three phases
     * were 40/30/30 (5 s) at first, too quick to read the crack before it opened.
     */
    public static final int TOTAL_TICKS = CRACK_TICKS + OPEN_TICKS + SPIRAL_TICKS;

    /**
     * Tick of the impact: where the shake, flash, ground wave, title and impact sounds fire,
     * and where the darkening and vignette peak (see {@link WormholeImpactLevers}). For the
     * tear, the moment it splits open. Must stay below {@link #TOTAL_TICKS}: the entity is
     * gone after that, and the impact would never fire.
     */
    public static final int IMPACT_AT = OPEN_AT;

    /**
     * Tag of the debris block displays of the pre-4.1 animation. No longer created, but
     * still cleaned up at server start ({@code ModEvents.cleanUp}): a world saved mid
     * animation by 4.0 keeps them otherwise, floating forever.
     */
    public static final String BLOCK_TAG = "wormhole_animation_block";

    // ---- sounds ----------------------------------------------------------

    /** Ticks between two crackles of the growing crack. */
    private static final int CRACKLE_INTERVAL = 7;

    /**
     * How late after the impact a client that just started seeing the animation still gets
     * the one-shot impact effects, in ticks. Covers the spawn packet arriving a tick or two
     * late; raising it makes far-away players get a delayed title.
     */
    private static final int IMPACT_LATE_GRACE_TICKS = 5;

    /** Pitch of the impact's sonic boom: below 1 for a deeper, heavier blast. */
    private static final float IMPACT_BOOM_PITCH = 0.6f;

    /** Pitch of the impact's thunder. Below ~0.5 it turns into a rumble without a crack. */
    private static final float IMPACT_THUNDER_PITCH = 0.8f;

    /** Volume of the animation's sounds. Above 1 only widens the range they are heard at. */
    private static final float SOUND_VOLUME = 2.0f;

    // ---- particles -------------------------------------------------------

    /** Half-height of the full crack, in blocks. Shared with the renderer. */
    public static final double CRACK_HALF_LENGTH = 1.6;

    /** Glass shards thrown out when the crack splits open. */
    private static final int SHARD_COUNT = 36;

    /**
     * Swirl particles per tick reached at the end of the spiral, ramping up from none. Same
     * rate as the portal's own swirl, so the hand-over is invisible.
     */
    private static final int SWIRL_PER_TICK = 18;

    /** Plane of the swirl's ring along its axis, one block towards the viewer, as on the portal. */
    private static final double SWIRL_RING_OFFSET = 1.0;

    /** Radius of the swirl's ring, as on the portal. */
    private static final double SWIRL_RADIUS = 2.0;

    /** Colours of the shards: the pink, lilac and azure of the portal swirl. */
    private static final List<BlockState> SHARD_STATES = List.of(
            Blocks.PINK_STAINED_GLASS.defaultBlockState(),
            Blocks.MAGENTA_STAINED_GLASS.defaultBlockState(),
            Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState()
    );

    /**
     * Game time the animation started at, synced to clients with the spawn packet. Sent once:
     * each client derives the current tick from its own clock instead of receiving it every
     * tick. 0 means unknown (never set), and clients fall back on their own tick count.
     */
    private static final EntityDataAccessor<Long> DATA_STARTED_AT =
            SynchedEntityData.defineId(WormholeSpawnAnimation.class, EntityDataSerializers.LONG);

    // NBT keys. Renaming one makes animations saved mid-play restart from scratch. The tick
    // and target keys are those of 4.0, so an animation saved by 4.0 resumes here.
    private static final String NBT_TICK = "AnimationTick";
    private static final String NBT_FINAL_SPAWNED = "FinalSpawned";
    private static final String NBT_TARGET_X = "TargetX";
    private static final String NBT_TARGET_Y = "TargetY";
    private static final String NBT_TARGET_Z = "TargetZ";

    /** Ticks played, server side. */
    private int animationTick = 0;
    private BlockPos targetPos = BlockPos.ZERO;
    private boolean finalEntitySpawned = false;
    /** Server side: whether the entity was moved onto its target since it was created or loaded. */
    private boolean positioned;

    /** Client side: one-shot effects already played, so a missed tick never replays them. */
    private boolean shardsThrown;
    private boolean flashShown;
    /** Client side: set if the particles cannot be spawned, so the error is logged once. */
    private boolean particlesUnavailable;

    /** Client side: start of the arrival already reported to the impact effects. */
    private boolean arrivalReported;

    /** Client side: impact already reported, so it fires once even if a tick is skipped. */
    private boolean impactReported;

    /**
     * Client side: axis of the swirl, towards the local player when the animation appeared to
     * them, then fixed. Handed to the wormhole through {@link PortalSwirl#remember}.
     */
    private Vec3 swirlAxis;

    /** Constructor used by the entity type factory, and when loading from a save. */
    public WormholeSpawnAnimation(EntityType<? extends WormholeSpawnAnimation> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setInvulnerable(true);
        // The tear reaches 1.6 blocks above and below the entity, beyond its hitbox: without
        // this, it vanished as soon as the entity's feet left the screen.
        this.noCulling = true;
    }

    /** Creates an animation that will open a wormhole at {@code pos}, starting now. */
    public WormholeSpawnAnimation(EntityType<? extends WormholeSpawnAnimation> type, Level level, BlockPos pos) {
        this(type, level);
        this.targetPos = pos;
        this.entityData.set(DATA_STARTED_AT, level.getGameTime());
    }

    /**
     * Where the tear opens and the wormhole appears: the centre of the target block, at its
     * bottom, exactly where {@link #spawnFinalWormhole} places the wormhole.
     */
    public static Vec3 centreOf(BlockPos target) {
        return Vec3.atBottomCenterOf(target);
    }

    /**
     * Ticks since the animation started, with the partial tick, as seen by this side. Used
     * by the renderer every frame.
     */
    public float elapsed(float partialTick) {
        long startedAt = this.entityData.get(DATA_STARTED_AT);
        float ticks = startedAt == 0L
                ? this.tickCount
                : (float) (this.level().getGameTime() - startedAt);
        return Math.max(0.0f, ticks + partialTick);
    }

    /**
     * Seed of this animation's crack shape: the same on every client, since the UUID comes
     * with the spawn packet, and different for every wormhole.
     */
    public long crackSeed() {
        return this.getUUID().getLeastSignificantBits();
    }

    /** Advances the animation: timing and sounds on the server, particles on the client. */
    @Override
    public void tick() {
        super.tick();

        if (this.level() instanceof ServerLevel level) {
            serverTick(level);
        } else {
            clientTick();
        }
    }

    // ---- server ----------------------------------------------------------

    /** Counts ticks, plays the sounds of each phase, and spawns the wormhole at the end. */
    private void serverTick(ServerLevel level) {
        if (finalEntitySpawned) return;

        if (!positioned) {
            // A 4.0 animation reloaded mid-descent sits up in the air: bring it home.
            positioned = true;
            Vec3 centre = centreOf(targetPos);
            this.setPos(centre.x, centre.y, centre.z);
        }

        animationTick++;

        if (animationTick == 1) {
            play(level, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.6f);
        } else if (animationTick < OPEN_AT && animationTick % CRACKLE_INTERVAL == 0) {
            play(level, SoundEvents.AMETHYST_BLOCK_CHIME, 0.5f + this.random.nextFloat() * 0.6f);
        } else if (animationTick == OPEN_AT) {
            play(level, SoundEvents.GLASS_BREAK, 0.5f);
            // Registered since 4.0 but never played until now.
            play(level, ModSounds.WORMHOLE_ANIMATION_SPAWN, 1.0f);
        } else if (animationTick == SPIRAL_AT) {
            play(level, SoundEvents.BEACON_ACTIVATE, 1.4f);
        }

        if (animationTick == IMPACT_AT && WormholeImpactLevers.IMPACT_SOUND) {
            playImpactSounds(level);
        }

        if (animationTick >= TOTAL_TICKS) {
            spawnFinalWormhole(level);
        }
    }

    /**
     * The impact's boom and thunder, heard much farther than the animation's own sounds.
     * Server side, so every player in range hears it, including those who do not see the tear.
     */
    private void playImpactSounds(ServerLevel level) {
        float volume = WormholeImpactLevers.IMPACT_SOUND_VOLUME;
        level.playSound(null, targetPos, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, volume, IMPACT_BOOM_PITCH);
        level.playSound(null, targetPos, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, volume, IMPACT_THUNDER_PITCH);
    }

    /** Plays a sound at the tear for every nearby player. */
    private void play(ServerLevel level, net.minecraft.sounds.SoundEvent sound, float pitch) {
        level.playSound(null, targetPos, sound, SoundSource.HOSTILE, SOUND_VOLUME, pitch);
    }

    /** Spawns the actual wormhole at the target and removes this director entity. */
    private void spawnFinalWormhole(ServerLevel level) {
        finalEntitySpawned = true;

        Vec3 centre = centreOf(targetPos);
        WormholeEntity wormhole = new WormholeEntity(ModEntities.WORMHOLE, level);
        wormhole.setPos(centre.x, centre.y, centre.z);
        wormhole.setPlacementTime(level.getGameTime());
        level.addFreshEntity(wormhole);

        level.playSound(null, targetPos, ModSounds.WORMHOLE_SPAWN, SoundSource.HOSTILE, 3.0f, 0.8f);

        this.discard();
    }

    // ---- client ----------------------------------------------------------

    /**
     * Spawns this tick's particles. Each phase adds its own: sparks at the growing tips, a
     * burst of glass shards when the crack splits, then the swirl ramping up to the portal's
     * rate. Cost: a few particles per tick, at most {@value #SWIRL_PER_TICK} plus three,
     * during seven and a half seconds.
     */
    private void clientTick() {
        // Before the particle guard: a broken particle sprite must not cost the impact.
        reportToImpactEffects(elapsed(0.0f));
        if (particlesUnavailable) return;

        Level level = this.level();
        float t = elapsed(0.0f);
        double x = this.getX();
        double y = this.getY();
        double z = this.getZ();

        try {
            if (t < OPEN_AT) {
                double reach = CRACK_HALF_LENGTH * easeOut(t / CRACK_TICKS);
                // Both tips, where the crack is tearing.
                level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y + reach, z, 0, 0.02, 0);
                level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y - reach, z, 0, -0.02, 0);
                if (this.random.nextInt(3) == 0) {
                    level.addParticle(ParticleTypes.END_ROD, x, y + (this.random.nextDouble() * 2 - 1) * reach, z,
                            0, 0, 0);
                }
            } else if (t < SPIRAL_AT) {
                if (!shardsThrown) {
                    shardsThrown = true;
                    throwShards(level, x, y, z);
                }
                level.addParticle(ParticleTypes.END_ROD,
                        x, y + (this.random.nextDouble() * 2 - 1) * CRACK_HALF_LENGTH, z,
                        (this.random.nextDouble() - 0.5) * 0.05, 0, (this.random.nextDouble() - 0.5) * 0.05);
            } else {
                spawnSwirl(level, x, y, z, (t - SPIRAL_AT) / SPIRAL_TICKS);
                if (!flashShown && t >= TOTAL_TICKS - 1) {
                    flashShown = true;
                    level.addParticle(ParticleTypes.FLASH, x, y, z, 0, 0, 0);
                }
            }
        } catch (RuntimeException e) {
            // Same safeguard as the portal swirl: a missing particle sprite throws from the
            // particle engine, and uncaught here it would crash the client.
            particlesUnavailable = true;
            UltraBeasts.LOGGER.warn("Could not spawn wormhole animation particles, disabling them.", e);
        }
    }

    /**
     * Tells the client's impact effects where the animation stands: its timeline on the first
     * tick this client sees, then the impact once. Also freezes the swirl's axis on that first
     * tick, which is the moment the animation "appears" to this player.
     */
    private void reportToImpactEffects(float t) {
        Vec3 centre = this.position();
        if (!arrivalReported) {
            arrivalReported = true;
            swirlAxis = PortalSwirl.axisTowardsViewer(centre);
            PortalSwirl.remember(centre, swirlAxis);

            long start = startGameTime();
            WormholeFxBridge.arrivalStarted(centre, start, start + IMPACT_AT);
            // A player who only comes into range well after the impact should not get a
            // title and a ground wave out of nowhere.
            if (t > IMPACT_AT + IMPACT_LATE_GRACE_TICKS) {
                impactReported = true;
            }
        }
        if (!impactReported && t >= IMPACT_AT) {
            impactReported = true;
            WormholeFxBridge.impact(centre);
        }
    }

    /** Level game time the animation started at, on this side's clock. */
    private long startGameTime() {
        long startedAt = this.entityData.get(DATA_STARTED_AT);
        return startedAt != 0L ? startedAt : this.level().getGameTime() - this.tickCount;
    }

    /**
     * Throws tinted glass shards out of the crack, in every direction. Vanilla's block
     * particles keep the direction but pick their own small speed, and fall with gravity.
     */
    private void throwShards(Level level, double x, double y, double z) {
        for (int i = 0; i < SHARD_COUNT; i++) {
            BlockState state = SHARD_STATES.get(this.random.nextInt(SHARD_STATES.size()));
            double along = (this.random.nextDouble() * 2 - 1) * CRACK_HALF_LENGTH;
            double angle = this.random.nextDouble() * Math.PI * 2;
            level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, state),
                    x, y + along, z,
                    Math.cos(angle), 0.3 + this.random.nextDouble() * 0.5, Math.sin(angle));
        }
    }

    /**
     * The portal's swirl, ramping from nothing to its full rate, with the same geometry as
     * {@link AbstractPortalEntity}: ring one block in front, converging three blocks behind.
     */
    private void spawnSwirl(Level level, double x, double y, double z, double progress) {
        SimpleParticleType type = ModParticles.WORMHOLE;
        if (type == null) return;

        int count = Mth.ceil(SWIRL_PER_TICK * Mth.clamp(progress, 0.0, 1.0));
        Vec3 axis = swirlAxis != null ? swirlAxis : PortalSwirl.DEFAULT_AXIS;
        PortalSwirl.spawn(level, type, new Vec3(x, y, z), axis,
                SWIRL_RING_OFFSET, WormholeEntity.PARTICLE_DESTINATION_Z_OFFSET,
                SWIRL_RADIUS, count, this.random);
    }

    /** Fast start, slow end: the crack tears quickly, then slows as it reaches full length. */
    public static double easeOut(double t) {
        double c = 1.0 - Mth.clamp(t, 0.0, 1.0);
        return 1.0 - c * c * c;
    }

    /** Smooth start and stop. */
    public static double easeInOut(double t) {
        double c = Mth.clamp(t, 0.0, 1.0);
        return c * c * (3.0 - 2.0 * c);
    }

    // ---- data ------------------------------------------------------------

    /** Declares the synced start time; 0 until the server sets it. */
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_STARTED_AT, 0L);
    }

    /**
     * Restores the animation, and derives its start time from the ticks already played, so
     * clients that see it after a reload resume where it stopped.
     */
    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        this.animationTick = nbt.getInt(NBT_TICK);
        this.finalEntitySpawned = nbt.getBoolean(NBT_FINAL_SPAWNED);
        if (nbt.contains(NBT_TARGET_X)) {
            this.targetPos = new BlockPos(nbt.getInt(NBT_TARGET_X), nbt.getInt(NBT_TARGET_Y), nbt.getInt(NBT_TARGET_Z));
        }
        this.entityData.set(DATA_STARTED_AT, this.level().getGameTime() - this.animationTick);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        nbt.putInt(NBT_TICK, animationTick);
        nbt.putBoolean(NBT_FINAL_SPAWNED, finalEntitySpawned);
        nbt.putInt(NBT_TARGET_X, targetPos.getX());
        nbt.putInt(NBT_TARGET_Y, targetPos.getY());
        nbt.putInt(NBT_TARGET_Z, targetPos.getZ());
    }
}
