package dev.darcosse.ultrabeasts.entity;

import dev.darcosse.ultrabeasts.UltraBeasts;
import dev.darcosse.ultrabeasts.fx.WormholeFxBridge;
import dev.darcosse.ultrabeasts.fx.WormholeImpactLevers;
import dev.darcosse.ultrabeasts.registry.ModEntities;
import dev.darcosse.ultrabeasts.registry.ModParticles;
import dev.darcosse.ultrabeasts.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Invisible director entity that plays the wormhole's arrival as a singularity, then
 * replaces itself with a {@link WormholeEntity} at the same spot.
 * <ol>
 *   <li>GATHER ({@value #GATHER_TICKS} ticks): a black core grows, ringed by spinning arcs of
 *   light, and pulls particles in from all around;</li>
 *   <li>COLLAPSE ({@value #COLLAPSE_TICKS} ticks): the core shrinks to a point while the arcs
 *   wind up and close in;</li>
 *   <li>IMPACT: it bursts. A shockwave races out, particles are flung away and the portal's
 *   disc snaps open;</li>
 *   <li>SETTLE ({@value #SETTLE_TICKS} ticks): the disc dissolves into the swirl, and the
 *   wormhole takes over seamlessly.</li>
 * </ol>
 * The server only keeps time, plays the sounds and spawns the wormhole. Everything visible
 * is drawn by each client: the shapes by {@code WormholeSpawnAnimationRenderer}, the particles
 * by {@link #clientTick}.
 */
public class WormholeSpawnAnimation extends Entity {

    /** Length of the gathering phase, in ticks. Shared with the renderer. */
    public static final int GATHER_TICKS = 70;

    /** Length of the collapse, in ticks. Short: it is the held breath before the blast. */
    public static final int COLLAPSE_TICKS = 10;

    /** Length of the settling phase after the impact, in ticks. Shared with the renderer. */
    public static final int SETTLE_TICKS = 40;

    /** When the collapse starts, in ticks since the animation began. */
    public static final int COLLAPSE_AT = GATHER_TICKS;

    /**
     * Tick of the impact: the burst, and where the shake, flash, ground wave, title and impact
     * sounds fire (see {@link WormholeImpactLevers}). Must stay below {@link #TOTAL_TICKS}:
     * the entity is gone after that, and the impact would never fire.
     */
    public static final int IMPACT_AT = COLLAPSE_AT + COLLAPSE_TICKS;

    /** Whole animation, in ticks (6 s). The wormhole appears on this tick. */
    public static final int TOTAL_TICKS = IMPACT_AT + SETTLE_TICKS;

    /**
     * Tag of the debris block displays of the pre-4.1 animation. No longer created, but
     * still cleaned up at server start ({@code ModEvents.cleanUp}): a world saved mid
     * animation by 4.0 keeps them otherwise, floating forever.
     */
    public static final String BLOCK_TAG = "wormhole_animation_block";

    // ---- sounds ----------------------------------------------------------

    /** Volume of the animation's sounds. Above 1 only widens the range they are heard at. */
    private static final float SOUND_VOLUME = 2.0f;

    /** Pitch of the impact's sonic boom: below 1 for a deeper, heavier blast. */
    private static final float IMPACT_BOOM_PITCH = 0.6f;

    /** Pitch of the impact's thunder. Below ~0.5 it turns into a rumble without a crack. */
    private static final float IMPACT_THUNDER_PITCH = 0.8f;

    // ---- particles -------------------------------------------------------

    /**
     * Swirl particles per tick reached at the end of the animation, ramping up from none. Same
     * rate as the portal's own swirl, so the hand-over is invisible.
     */
    private static final int SWIRL_PER_TICK = 18;

    /** Plane of the swirl's ring along its axis, one block towards the viewer, as on the portal. */
    private static final double SWIRL_RING_OFFSET = 1.0;

    /** Radius of the swirl's ring, as on the portal. */
    private static final double SWIRL_RADIUS = 2.0;

    /**
     * How late after the impact a client that just started seeing the animation still gets
     * the one-shot impact effects, in ticks. Covers the spawn packet arriving a tick or two
     * late; raising it makes far-away players get a delayed title.
     */
    private static final int IMPACT_LATE_GRACE_TICKS = 5;

    /** Ticks between two resonances while the core gathers. */
    private static final int RESONANCE_INTERVAL = 14;

    /** Particles pulled in per tick at the start of the gathering, and at its end. */
    private static final int SUCTION_MIN_PER_TICK = 2;
    private static final int SUCTION_MAX_PER_TICK = 8;

    /** Particles pulled in per tick during the collapse: the last, frantic gulp. */
    private static final int COLLAPSE_SUCTION_PER_TICK = 10;

    /**
     * Distance the gathered particles come from, in blocks. Beyond ~6 they start outside
     * the view of a player standing close.
     */
    private static final double SUCTION_MIN_RADIUS = 3.0;
    private static final double SUCTION_MAX_RADIUS = 6.0;

    /** Distance the collapse's particles come from, in blocks: closer, so they arrive in time. */
    private static final double COLLAPSE_SUCTION_RADIUS = 2.5;

    /** Particles flung out in every direction by the burst. */
    private static final int BURST_SPHERE_PARTICLES = 60;

    /** Particles flung out flat, in the plane facing the viewer: the visible shockwave front. */
    private static final int BURST_RING_PARTICLES = 32;

    /** Speed of the burst's particles, in blocks per tick. */
    private static final double BURST_SPEED = 0.45;

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

    /** Client side: the burst's particles already thrown. */
    private boolean burstThrown;

    /** Constructor used by the entity type factory, and when loading from a save. */
    public WormholeSpawnAnimation(EntityType<? extends WormholeSpawnAnimation> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setInvulnerable(true);
        // What is drawn reaches well beyond the entity's 1-block hitbox: without this, it
        // vanished as soon as the entity's feet left the screen.
        this.noCulling = true;
    }

    /** Creates an animation that will open a wormhole at {@code pos}, starting now. */
    public WormholeSpawnAnimation(EntityType<? extends WormholeSpawnAnimation> type, Level level, BlockPos pos) {
        this(type, level);
        this.targetPos = pos;
        this.entityData.set(DATA_STARTED_AT, level.getGameTime());
    }

    /**
     * Where the animation plays and the wormhole appears: the centre of the target block, at
     * its bottom, exactly where {@link #spawnFinalWormhole} places the wormhole.
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
     * Seed of this animation's random details: the same on every client, since the UUID comes
     * with the spawn packet, and different for every wormhole.
     */
    public long visualSeed() {
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
            // The nether portal's whoosh, slowed down: a long breath in.
            play(level, SoundEvents.PORTAL_TRIGGER, 0.5f);
        } else if (animationTick < COLLAPSE_AT && animationTick % RESONANCE_INTERVAL == 0) {
            // Rising pitch: the pull gets stronger.
            play(level, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.5f + 0.7f * animationTick / COLLAPSE_AT);
        } else if (animationTick == COLLAPSE_AT) {
            play(level, SoundEvents.BEACON_DEACTIVATE, 0.5f);
        } else if (animationTick == IMPACT_AT) {
            play(level, ModSounds.WORMHOLE_ANIMATION_SPAWN, 1.0f);
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
     * Server side, so every player in range hears it, including those who do not see it.
     */
    private void playImpactSounds(ServerLevel level) {
        float volume = WormholeImpactLevers.IMPACT_SOUND_VOLUME;
        level.playSound(null, targetPos, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, volume, IMPACT_BOOM_PITCH);
        level.playSound(null, targetPos, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, volume, IMPACT_THUNDER_PITCH);
    }

    /** Plays a sound at the animation for every nearby player. */
    private void play(ServerLevel level, SoundEvent sound, float pitch) {
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
     * Spawns this tick's particles: suction while the core gathers and collapses, one burst
     * on the impact, then the swirl ramping up to the portal's rate. Cost: at most
     * {@value #COLLAPSE_SUCTION_PER_TICK} particles per tick, plus a one-off burst of
     * {@value #BURST_SPHERE_PARTICLES} + {@value #BURST_RING_PARTICLES}.
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
            if (t < COLLAPSE_AT) {
                double progress = t / GATHER_TICKS;
                int count = SUCTION_MIN_PER_TICK
                        + Mth.floor((SUCTION_MAX_PER_TICK - SUCTION_MIN_PER_TICK) * progress);
                suck(level, x, y, z, count, SUCTION_MIN_RADIUS, SUCTION_MAX_RADIUS);
            } else if (t < IMPACT_AT) {
                suck(level, x, y, z, COLLAPSE_SUCTION_PER_TICK, COLLAPSE_SUCTION_RADIUS, COLLAPSE_SUCTION_RADIUS);
            } else {
                if (!burstThrown) {
                    burstThrown = true;
                    burst(level, x, y, z);
                }
                spawnSwirl(level, x, y, z, (t - IMPACT_AT) / SETTLE_TICKS);
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
     * Axis of the swirl, frozen towards the viewer (see {@link #reportToImpactEffects}).
     * Also gives the plane facing the viewer, for particle effects that must lie flat in the
     * picture like the renderer's drawing.
     */
    private Vec3 swirlAxis() {
        return swirlAxis != null ? swirlAxis : PortalSwirl.DEFAULT_AXIS;
    }

    /**
     * Particles converging on the core from a random sphere. Vanilla's portal particle takes
     * its velocity as the offset it starts from and flies back to the point it was spawned at,
     * which is exactly a pull towards the centre.
     */
    private void suck(Level level, double x, double y, double z, int count, double minRadius, double maxRadius) {
        for (int i = 0; i < count; i++) {
            Vec3 direction = randomDirection();
            double distance = minRadius + this.random.nextDouble() * (maxRadius - minRadius);
            level.addParticle(ParticleTypes.PORTAL, x, y, z,
                    direction.x * distance, direction.y * distance, direction.z * distance);
        }
    }

    /**
     * The impact's burst: reverse-portal particles flung out in every direction, a flat ring
     * of light racing out in the plane facing the viewer, and a flash at the core.
     */
    private void burst(Level level, double x, double y, double z) {
        for (int i = 0; i < BURST_SPHERE_PARTICLES; i++) {
            Vec3 direction = randomDirection();
            double speed = BURST_SPEED * (0.5 + this.random.nextDouble() * 0.5);
            level.addParticle(ParticleTypes.REVERSE_PORTAL, x, y, z,
                    direction.x * speed, direction.y * speed, direction.z * speed);
        }

        Vec3[] plane = PortalSwirl.basis(swirlAxis());
        for (int i = 0; i < BURST_RING_PARTICLES; i++) {
            double angle = Math.PI * 2 * i / BURST_RING_PARTICLES;
            Vec3 direction = plane[0].scale(Math.cos(angle)).add(plane[1].scale(Math.sin(angle)));
            level.addParticle(ParticleTypes.END_ROD, x, y, z,
                    direction.x * BURST_SPEED, direction.y * BURST_SPEED, direction.z * BURST_SPEED);
        }

        level.addParticle(ParticleTypes.FLASH, x, y, z, 0, 0, 0);
    }

    /** A uniformly random unit vector. */
    private Vec3 randomDirection() {
        double yComponent = this.random.nextDouble() * 2.0 - 1.0;
        double angle = this.random.nextDouble() * Math.PI * 2;
        double horizontal = Math.sqrt(1.0 - yComponent * yComponent);
        return new Vec3(Math.cos(angle) * horizontal, yComponent, Math.sin(angle) * horizontal);
    }

    /**
     * The portal's swirl, ramping from nothing to its full rate, with the same geometry as
     * {@link AbstractPortalEntity}: ring one block in front, converging three blocks behind.
     */
    private void spawnSwirl(Level level, double x, double y, double z, double progress) {
        SimpleParticleType type = ModParticles.WORMHOLE;
        if (type == null) return;

        int count = Mth.ceil(SWIRL_PER_TICK * Mth.clamp(progress, 0.0, 1.0));
        PortalSwirl.spawn(level, type, new Vec3(x, y, z), swirlAxis(),
                SWIRL_RING_OFFSET, WormholeEntity.PARTICLE_DESTINATION_Z_OFFSET,
                SWIRL_RADIUS, count, this.random);
    }

    /** Fast start, slow end. */
    public static double easeOut(double t) {
        double c = 1.0 - Mth.clamp(t, 0.0, 1.0);
        return 1.0 - c * c * c;
    }

    /** Smooth start and stop. */
    public static double easeInOut(double t) {
        double c = Mth.clamp(t, 0.0, 1.0);
        return c * c * (3.0 - 2.0 * c);
    }

    /** Slow start, fast end. */
    public static double easeIn(double t) {
        double c = Mth.clamp(t, 0.0, 1.0);
        return c * c * c;
    }

    /** Overshoot of {@link #easeOutBack}: 1.7 shoots ~10 % past the end. 0 disables it. */
    private static final double OVERSHOOT = 1.7;

    /**
     * Fast start that shoots a little past 1 before settling: the disc bursts open slightly
     * too wide, then snaps back.
     */
    public static double easeOutBack(double t) {
        double c = Mth.clamp(t, 0.0, 1.0) - 1.0;
        return 1.0 + c * c * ((OVERSHOOT + 1.0) * c + OVERSHOOT);
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
