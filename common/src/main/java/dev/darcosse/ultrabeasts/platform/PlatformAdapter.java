package dev.darcosse.ultrabeasts.platform;

import java.nio.file.Path;

/**
 * Loader services the common code cannot obtain on its own. Implemented once per loader
 * module and installed through {@link Platform#set(PlatformAdapter)}.
 */
public interface PlatformAdapter {

    /** Returns the game's root config directory, as defined by the running loader. */
    Path getConfigDir();
}
