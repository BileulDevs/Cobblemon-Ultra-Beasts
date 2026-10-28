package dev.darcosse.ultrabeasts.client.config;

/**
 * Plain data holder that Gson maps onto {@code client.json}: each player's own settings,
 * never read by a server.
 * <p>
 * Field initializers are the defaults: a key missing from the file keeps its default. The
 * upper-case field names ARE the JSON keys; renaming a field silently resets that setting.
 */
public class UltraBeastsClientConfig {

    /**
     * Whether arriving in Ultra-Space plays the encounter cinematic (the structure, then a
     * close-up of the Ultra Beast and its cry). When false, the arrival is a short fade from
     * black instead, which hides the terrain loading as the cinematic's opening does.
     */
    public boolean ENCOUNTER_CINEMATIC = true;
}
