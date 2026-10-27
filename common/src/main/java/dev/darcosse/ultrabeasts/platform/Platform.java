package dev.darcosse.ultrabeasts.platform;

/**
 * Holds the {@link PlatformAdapter} supplied by the running loader, so common code can
 * reach loader services without depending on Fabric or NeoForge classes.
 */
public final class Platform {

    private static PlatformAdapter adapter;

    /** Static holder only, never instantiated. */
    private Platform() {
    }

    /**
     * Installs the loader's adapter. Called once, by {@code UltraBeasts.init}.
     *
     * @param platformAdapter the running loader's implementation
     */
    public static void set(PlatformAdapter platformAdapter) {
        adapter = platformAdapter;
    }

    /**
     * Returns the loader's adapter.
     *
     * @throws IllegalStateException if called before {@code UltraBeasts.init}; failing loudly
     *                               beats a NullPointerException deep inside the config code
     */
    public static PlatformAdapter get() {
        if (adapter == null) {
            throw new IllegalStateException(
                    "PlatformAdapter not initialized: UltraBeasts.init(adapter) was never called.");
        }
        return adapter;
    }
}
