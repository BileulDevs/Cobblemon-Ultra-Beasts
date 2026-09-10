package dev.darcosse.ultrabeasts.platform;

import java.nio.file.Path;

/** Everything the common code cannot do on its own. */
public interface PlatformAdapter {

    /** The game's config directory. */
    Path getConfigDir();
}
