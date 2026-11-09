package dev.darcosse.ultrabeasts.cinematic;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * One shot of a structure's tour, the part of the encounter cinematic that shows the
 * scenery before the close-up on the Ultra Beast: the camera moves in a straight line from
 * one pose to another, turning as it goes.
 * <p>
 * Poses are absolute world positions in Ultra-Space, with Minecraft's own angles (yaw 0
 * faces +Z, positive pitch looks down), so a shot can be recorded by standing where the
 * camera should be and looking where it should look. Shared by the server, which loads and
 * sends the shots, and the client, which plays them.
 *
 * @param from       where the shot starts
 * @param to         where the shot ends
 * @param durationMs how long the shot lasts, cuts included
 */
public record CinematicShot(Pose from, Pose to, int durationMs) {

    /** Shortest shot accepted, in milliseconds: below it the cuts on both sides eat it whole. */
    public static final int MIN_DURATION_MS = 800;

    /** Longest shot accepted, in milliseconds: a tour is a few seconds, not a film. */
    public static final int MAX_DURATION_MS = 15_000;

    /** A camera pose: where it stands and where it looks. */
    public record Pose(Vec3 position, float yaw, float pitch) {

        /** Unit vector the pose looks along, with the same conventions as {@code Entity#lookAt}. */
        public Vec3 direction() {
            float yawRad = yaw * Mth.DEG_TO_RAD;
            float pitchRad = pitch * Mth.DEG_TO_RAD;
            double horizontal = Math.cos(pitchRad);
            return new Vec3(-Math.sin(yawRad) * horizontal, -Math.sin(pitchRad), Math.cos(yawRad) * horizontal);
        }

        /** Writes the pose; MUST match {@link #read}. */
        void write(FriendlyByteBuf buf) {
            buf.writeDouble(position.x);
            buf.writeDouble(position.y);
            buf.writeDouble(position.z);
            buf.writeFloat(yaw);
            buf.writeFloat(pitch);
        }

        /** Reads a pose; MUST match {@link #write}. */
        static Pose read(FriendlyByteBuf buf) {
            Vec3 position = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
            return new Pose(position, buf.readFloat(), buf.readFloat());
        }
    }

    /** The shot with its duration clamped to the accepted range. */
    public CinematicShot {
        durationMs = Mth.clamp(durationMs, MIN_DURATION_MS, MAX_DURATION_MS);
    }

    /**
     * The pose at a given progress, 0 to 1, eased in and out so the camera starts and stops
     * gently. The yaw takes the short way round: from 170 to -170 turns 20 degrees, not 340.
     */
    public Pose at(double progress) {
        double t = Mth.clamp(progress, 0.0, 1.0);
        float eased = (float) (t * t * (3.0 - 2.0 * t));
        Vec3 position = from.position().lerp(to.position(), eased);
        float yaw = from.yaw() + Mth.wrapDegrees(to.yaw() - from.yaw()) * eased;
        float pitch = Mth.lerp(eased, from.pitch(), to.pitch());
        return new Pose(position, yaw, pitch);
    }

    /** Writes the shot to a payload; MUST match {@link #read}. */
    public void write(FriendlyByteBuf buf) {
        from.write(buf);
        to.write(buf);
        buf.writeVarInt(durationMs);
    }

    /** Reads a shot from a payload; MUST match {@link #write}. */
    public static CinematicShot read(FriendlyByteBuf buf) {
        Pose from = Pose.read(buf);
        Pose to = Pose.read(buf);
        return new CinematicShot(from, to, buf.readVarInt());
    }
}
