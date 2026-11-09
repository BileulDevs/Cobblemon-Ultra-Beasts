package dev.darcosse.ultrabeasts.fx;

import net.minecraft.world.phys.Vec3;

/**
 * Lets the common wormhole entities reach the client-only impact effects and the local
 * player's eyes without referencing client classes.
 * <p>
 * The entities tick on both sides and are loaded by a dedicated server, which must never load
 * client classes. The client entry point installs the real {@link Handler}; until then, and on
 * a dedicated server, every call is a no-op and {@link #viewerPosition()} is null. Same
 * pattern as {@code CinematicClientBridge}.
 */
public final class WormholeFxBridge {

    /** What the client provides. Every method is called on the client thread. */
    public interface Handler {

        /**
         * An arrival animation became visible to this client.
         *
         * @param origin          centre of the future wormhole
         * @param startGameTime   level game time the animation started at
         * @param impactGameTime  level game time of the impact; may be in the past for a
         *                        client that only saw the end of the animation
         */
        void onArrivalStarted(Vec3 origin, long startGameTime, long impactGameTime);

        /** The animation reached its impact, on this client's clock. Called once per animation. */
        void onImpact(Vec3 origin);

        /** Eye position of the local player, or null if there is none yet. */
        Vec3 viewerPosition();
    }

    /** Does nothing: what a dedicated server, or a client before its set-up, gets. */
    private static final Handler NO_OP = new Handler() {
        @Override
        public void onArrivalStarted(Vec3 origin, long startGameTime, long impactGameTime) {
        }

        @Override
        public void onImpact(Vec3 origin) {
        }

        @Override
        public Vec3 viewerPosition() {
            return null;
        }
    };

    private static Handler handler = NO_OP;

    /** Static holder only, never instantiated. */
    private WormholeFxBridge() {
    }

    /** Installs the client handler. Called once from the client entry point. */
    public static void setHandler(Handler clientHandler) {
        handler = clientHandler != null ? clientHandler : NO_OP;
    }

    /** See {@link Handler#onArrivalStarted}. */
    public static void arrivalStarted(Vec3 origin, long startGameTime, long impactGameTime) {
        handler.onArrivalStarted(origin, startGameTime, impactGameTime);
    }

    /** See {@link Handler#onImpact}. */
    public static void impact(Vec3 origin) {
        handler.onImpact(origin);
    }

    /** See {@link Handler#viewerPosition}. */
    public static Vec3 viewerPosition() {
        return handler.viewerPosition();
    }
}
