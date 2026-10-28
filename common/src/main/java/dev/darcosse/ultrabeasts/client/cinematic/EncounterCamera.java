package dev.darcosse.ultrabeasts.client.cinematic;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * The game camera during the encounter cinematic. Read every frame by {@code CameraMixin}
 * at the end of vanilla's camera setup, which it then overrides; rendering, culling and
 * sound all follow the camera, so the world is seen and heard from the shot.
 * <p>
 * The camera does not plan shots itself: a {@link Director} (the cinematic screen) says
 * where it stands and what it looks at, and this class only turns that into a rotation.
 * <p>
 * Client only; state of the client (render) thread.
 */
public final class EncounterCamera {

    /** What the camera does at one moment. */
    public record Shot(Vec3 position, Vec3 look) {
    }

    /** One frame's camera, as vanilla's camera wants it. */
    public record Frame(Vec3 position, float yaw, float pitch) {
    }

    /** Source of the shots. */
    @FunctionalInterface
    public interface Director {
        /**
         * The shot for this frame, or null to leave the player's own view.
         *
         * @param partialTick fraction of the current tick, to interpolate moving entities
         */
        Shot shot(float partialTick);
    }

    private static Director director;

    /** Static state only, never instantiated. */
    private EncounterCamera() {
    }

    /** Hands the camera to a director until {@link #stop()}. */
    public static void start(Director newDirector) {
        director = newDirector;
    }

    /** Gives the camera back to the player. Idempotent. */
    public static void stop() {
        director = null;
    }

    /**
     * This frame's camera, or null to leave vanilla's. Cost: whatever the director computes
     * plus two arc tangents, once per rendered frame, only while a cinematic plays.
     */
    public static Frame frame(float partialTick) {
        Director current = director;
        if (current == null) {
            return null;
        }
        Shot shot = current.shot(partialTick);
        if (shot == null) {
            return null;
        }
        Vec3 toward = shot.look().subtract(shot.position());
        double flat = Math.sqrt(toward.x * toward.x + toward.z * toward.z);
        // Same conventions as Entity#lookAt: yaw 0 faces +Z, positive pitch looks down.
        float yaw = (float) (Mth.atan2(-toward.x, toward.z) * Mth.RAD_TO_DEG);
        float pitch = (float) (-Mth.atan2(toward.y, flat) * Mth.RAD_TO_DEG);
        return new Frame(shot.position(), yaw, pitch);
    }
}
