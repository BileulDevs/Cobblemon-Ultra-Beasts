package dev.darcosse.ultrabeasts.platform;

/** Holds the adapter provided by the running loader. */
public final class Platform {

    private static PlatformAdapter adapter;

    private Platform() {
    }

    public static void set(PlatformAdapter platformAdapter) {
        adapter = platformAdapter;
    }

    public static PlatformAdapter get() {
        if (adapter == null) {
            throw new IllegalStateException(
                    "PlatformAdapter not initialized: UltraBeasts.init(adapter) was never called.");
        }
        return adapter;
    }
}
