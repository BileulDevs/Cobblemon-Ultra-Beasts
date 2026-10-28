package dev.darcosse.ultrabeasts.network;

import java.util.function.Consumer;

/**
 * Hands a received {@link EncounterCinematicPayload} to the client's cinematic screen.
 * <p>
 * NeoForge registers payload handlers on both dists; going through this bridge keeps the
 * handler free of client-only classes (the screen, the camera), which a dedicated server
 * must never load. The client entry point installs the real handler; until then, and on a
 * dedicated server, payloads are dropped.
 */
public final class CinematicClientBridge {

    private static Consumer<EncounterCinematicPayload> handler = payload -> {
    };

    /** Static holder only, never instantiated. */
    private CinematicClientBridge() {
    }

    /** Installs the client handler. Called once from the client entry point. */
    public static void setHandler(Consumer<EncounterCinematicPayload> clientHandler) {
        handler = clientHandler;
    }

    /** Delivers a payload. Must be called on the client thread. */
    public static void onCinematic(EncounterCinematicPayload payload) {
        handler.accept(payload);
    }
}
