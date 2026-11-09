package dev.darcosse.ultrabeasts.entity;

import dev.darcosse.ultrabeasts.fx.WormholeFxBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * Geometry of the converging swirl drawn around both portals and at the end of the arrival
 * animation: a ring of particles in a plane, flying to a point on the ring's axis.
 * <p>
 * Until 4.1 that axis was world Z, hard-coded in the particle itself. It is now a parameter:
 * the exit portal keeps world Z ({@link #DEFAULT_AXIS}, unchanged look), the inbound wormhole
 * turns it towards each viewer, frozen at the moment the wormhole appears to them, so the
 * funnel always sinks behind the disc.
 * <p>
 * Client side only in practice: particles are only spawned on the client.
 */
public final class PortalSwirl {

    /** Axis of the exit portal, and of any particle spawned without one (e.g. by /particle). */
    public static final Vec3 DEFAULT_AXIS = new Vec3(0, 0, 1);

    /**
     * The axis of the particles being spawned right now, read by the particle's constructor.
     * <p>
     * A particle only receives a position and three doubles (already used for its target),
     * and the alternative, a particle type with its own options codec, means a new network
     * format registered on both loaders. {@code Level.addParticle} constructs the particle
     * synchronously on the calling thread, so a thread-local set around the call is enough.
     * Thread-local rather than a plain static: in single player the server thread runs the
     * same entity code, and must never see or clobber the client's value.
     */
    private static final ThreadLocal<Vec3> PENDING_AXIS = new ThreadLocal<>();

    /**
     * Axes frozen by arrival animations, waiting for the wormhole that will replace them, keyed
     * by block position. Client thread only. Capped: an animation whose wormhole this client
     * never sees (it walked away) leaves its entry behind.
     */
    private static final Map<Long, Vec3> REMEMBERED_AXES = new HashMap<>();

    /** Entries kept at most in {@link #REMEMBERED_AXES}; over it the map is simply cleared. */
    private static final int MAX_REMEMBERED_AXES = 8;

    /**
     * Below this squared length, a direction is treated as undefined (viewer standing inside
     * the portal) and the default axis is used.
     */
    private static final double MIN_DIRECTION_LENGTH_SQR = 1.0E-4;

    /** Static helpers only, never instantiated. */
    private PortalSwirl() {
    }

    /**
     * Spawns one tick of the swirl.
     *
     * @param centre            centre of the portal
     * @param axis              unit vector the funnel lies along
     * @param ringOffset        position of the ring's plane along the axis, from the centre
     * @param destinationOffset point the particles converge on, along the axis, from the centre
     * @param radius            radius of the ring, in blocks
     * @param count             particles to spawn
     */
    public static void spawn(Level level, ParticleOptions type, Vec3 centre, Vec3 axis,
                             double ringOffset, double destinationOffset, double radius, int count,
                             RandomSource random) {
        if (count <= 0) return;

        Vec3[] basis = basis(axis);
        Vec3 u = basis[0];
        Vec3 v = basis[1];
        Vec3 ringCentre = centre.add(axis.scale(ringOffset));
        Vec3 destination = centre.add(axis.scale(destinationOffset));

        PENDING_AXIS.set(axis);
        try {
            for (int i = 0; i < count; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double cos = Math.cos(angle) * radius;
                double sin = Math.sin(angle) * radius;
                level.addParticle(type,
                        ringCentre.x + u.x * cos + v.x * sin,
                        ringCentre.y + u.y * cos + v.y * sin,
                        ringCentre.z + u.z * cos + v.z * sin,
                        destination.x, destination.y, destination.z);
            }
        } finally {
            // Cleared even if a particle throws (missing sprite): a stale axis would bend the
            // next particle spawned by anything else on this thread.
            PENDING_AXIS.remove();
        }
    }

    /**
     * The axis of the particle being constructed, for the particle's constructor. The default
     * axis when the particle was not spawned by {@link #spawn}.
     */
    public static Vec3 pendingAxis() {
        Vec3 axis = PENDING_AXIS.get();
        return axis != null ? axis : DEFAULT_AXIS;
    }

    /**
     * Two unit vectors spanning the plane perpendicular to {@code axis}. For world Z they are
     * world X and world Y, exactly the ring the swirl used before the axis existed.
     */
    public static Vec3[] basis(Vec3 axis) {
        Vec3 u = new Vec3(0, 1, 0).cross(axis);
        // Up and the axis are parallel when the viewer is right above or below the portal.
        u = u.lengthSqr() < MIN_DIRECTION_LENGTH_SQR ? new Vec3(1, 0, 0) : u.normalize();
        Vec3 v = axis.cross(u);
        return new Vec3[]{u, v};
    }

    /**
     * The unit vector from {@code centre} to the local player's eyes, or the default axis if
     * there is no local player (dedicated server, menu) or they stand at the centre.
     */
    public static Vec3 axisTowardsViewer(Vec3 centre) {
        Vec3 viewer = WormholeFxBridge.viewerPosition();
        if (viewer == null) return DEFAULT_AXIS;

        Vec3 direction = viewer.subtract(centre);
        if (direction.lengthSqr() < MIN_DIRECTION_LENGTH_SQR) return DEFAULT_AXIS;
        return direction.normalize();
    }

    /** Hands an animation's frozen axis to the wormhole that will appear at the same place. */
    public static void remember(Vec3 centre, Vec3 axis) {
        if (REMEMBERED_AXES.size() >= MAX_REMEMBERED_AXES) {
            REMEMBERED_AXES.clear();
        }
        REMEMBERED_AXES.put(key(centre), axis);
    }

    /**
     * The axis an animation froze at this place, consumed; or, if this client never saw the
     * animation (it arrived later), a new axis frozen towards the viewer now.
     */
    public static Vec3 recallOrFreeze(Vec3 centre) {
        Vec3 remembered = REMEMBERED_AXES.remove(key(centre));
        return remembered != null ? remembered : axisTowardsViewer(centre);
    }

    /** Map key of a portal centre: the block it sits in. Animation and wormhole share it. */
    private static long key(Vec3 centre) {
        return BlockPos.containing(centre).asLong();
    }
}
