package dev.darcosse.ultrabeasts.client;

import dev.darcosse.ultrabeasts.client.cinematic.ArrivalFadeScreen;
import dev.darcosse.ultrabeasts.client.cinematic.EncounterCinematicScreen;
import dev.darcosse.ultrabeasts.client.config.ClientConfigManager;
import dev.darcosse.ultrabeasts.network.EncounterCinematicPayload;
import dev.darcosse.ultrabeasts.network.CinematicClientBridge;

/**
 * Loader-independent client set-up, called once by each loader's client entry point.
 * Client only: a dedicated server never loads this class or what it references.
 */
public final class UltraBeastsClient {

    /** Static entry point only, never instantiated. */
    private UltraBeastsClient() {
    }

    /**
     * Loads the player's settings and plugs the client-only handlers into the bridges the
     * common code calls.
     */
    public static void init() {
        ClientConfigManager.load();
        CinematicClientBridge.setHandler(UltraBeastsClient::onArrival);
    }

    /**
     * Arrival in Ultra-Space: the cinematic, or a plain fade for a player who turned it off.
     * The settings are read again first, so editing {@code client.json} applies on the next
     * trip without restarting; a file of a few bytes, read once per trip.
     */
    private static void onArrival(EncounterCinematicPayload payload) {
        ClientConfigManager.load();
        if (ClientConfigManager.isEncounterCinematicEnabled()) {
            EncounterCinematicScreen.open(payload);
        } else {
            ArrivalFadeScreen.open(payload);
        }
    }
}
