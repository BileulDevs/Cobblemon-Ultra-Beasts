package dev.darcosse.ultrabeasts.entity;

import dev.darcosse.ultrabeasts.UltraBeasts;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Shared behaviour of the two portals: the inbound {@link WormholeEntity} and the exit
 * {@link ReturnWormholeEntity}. Both are invisible, physics-less entities drawn by a
 * billboard renderer, surrounded by a swirl of converging particles, and activated by
 * walking into them or right-clicking them.
 * <p>
 * Runs on both sides: the client draws the particles, the server handles activation.
 */
public abstract class AbstractPortalEntity extends Entity {

    /**
     * Particles spawned per client tick around one portal. Generated locally since 4.1, so
     * raising it only costs client-side rendering, linearly.
     */
    private static final int PARTICLES_PER_TICK = 18;

    /** Radius of the ring the particles start from, in blocks. */
    private static final double PORTAL_RADIUS = 2.0;

    /**
     * Set once the swirl cannot be drawn (type not registered, or its sprites never loaded),
     * so a broken install logs one warning instead of failing on every tick.
     */
    private boolean particlesUnavailable = false;

    /** Creates a portal that ignores collisions and cannot be damaged. */
    protected AbstractPortalEntity(EntityType<? extends AbstractPortalEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setInvulnerable(true);
    }

    /** Portals sync nothing beyond position: all their state is server-side. */
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    /** The particle type of this portal's swirl. May be null if registration failed. */
    protected abstract SimpleParticleType particleType();

    /**
     * Z offset, relative to the entity, of the plane of the ring the particles start from.
     */
    protected abstract double particleRingZOffset();

    /**
     * Z offset, relative to the entity, of the point the particles converge on. It differs
     * from {@link #particleRingZOffset()}: the particles travel along Z while they close in,
     * which is what draws the funnel. With both offsets equal, the swirl collapses into a
     * flat disc that looks like a column from the side.
     */
    protected abstract double particleDestinationZOffset();

    /** Server-side work for one tick. The default does nothing. */
    protected void serverTick(ServerLevel level) {
    }

    /** Called on the server when a player walks into or right-clicks the portal. */
    protected abstract void onPlayerEnter(ServerPlayer player);

    /** Dispatches each tick to the client particles or to {@link #serverTick}. */
    @Override
    public void tick() {
        super.tick();

        if (this.level() instanceof ServerLevel serverLevel) {
            serverTick(serverLevel);
        } else {
            spawnSwirlParticles();
        }
    }

    /**
     * Emits the swirl of particles converging on the portal, client side.
     * <p>
     * The destination travels in the velocity arguments, which the particle factory reads as
     * a target point (see {@code AbstractWormholeParticle}).
     * <p>
     * Before 4.1 the server sent each particle as its own packet: 18 packets per tick, i.e.
     * 360 per second, to every player near a portal. Those packets did NOT carry the target:
     * with count 0 the client computes the velocity as speed times offset, and the speed was
     * 0. Every particle therefore fell back to looking up the nearest portal and aiming at its
     * position shifted by the fallback Z offset, which is what drew the funnel. The offsets
     * below reproduce that exact target, without the per-particle entity lookup.
     */
    private void spawnSwirlParticles() {
        if (particlesUnavailable) return;

        SimpleParticleType type = particleType();
        if (type == null) {
            particlesUnavailable = true;
            UltraBeasts.LOGGER.warn(
                    "Portal particle type is not registered, disabling portal particles for this entity.");
            return;
        }

        Level level = this.level();
        double centerX = this.getX();
        double centerY = this.getY();
        double ringZ = this.getZ() + particleRingZOffset();
        double destinationZ = this.getZ() + particleDestinationZOffset();

        try {
            for (int i = 0; i < PARTICLES_PER_TICK; i++) {
                double angle = level.random.nextDouble() * 2 * Math.PI;

                level.addParticle(type,
                        centerX + Math.cos(angle) * PORTAL_RADIUS,
                        centerY + Math.sin(angle) * PORTAL_RADIUS,
                        ringZ,
                        centerX, centerY, destinationZ);
            }
        } catch (RuntimeException e) {
            // Thrown when the particle's sprites were never loaded: missing or broken mod
            // assets, or a resource pack overriding particles/*.json. The provider then hits a
            // null sprite list and LevelRenderer rethrows it as a ReportedException. Uncaught
            // here, it crashes the client: this runs inside the entity tick, unlike the 4.0
            // packet path, where vanilla's packet handler caught it and only logged a warning.
            particlesUnavailable = true;
            UltraBeasts.LOGGER.warn("Could not spawn portal particles, disabling them for this entity. "
                    + "Are the mod's assets loaded?", e);
        }
    }

    /** Right-click activation; walking in is handled by {@link #playerTouch}. */
    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (this.level().isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        if (!this.isRemoved()) {
            onPlayerEnter(serverPlayer);
        }
        return InteractionResult.SUCCESS;
    }

    /** Walk-in activation. Called every tick while a player overlaps the hitbox. */
    @Override
    public void playerTouch(Player player) {
        // isRemoved() guards against a second activation in the same tick, before the
        // discarded entity is actually unloaded.
        if (!this.level().isClientSide && player instanceof ServerPlayer serverPlayer && !this.isRemoved()) {
            onPlayerEnter(serverPlayer);
        }
        super.playerTouch(player);
    }
}
