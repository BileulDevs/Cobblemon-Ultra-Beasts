package dev.darcosse.ultrabeasts.fabric;

import dev.darcosse.ultrabeasts.platform.PlatformAdapter;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

/** Fabric implementation of the platform services. */
public class FabricPlatformAdapter implements PlatformAdapter {

    /** Fabric's config directory, usually {@code .minecraft/config}. */
    @Override
    public Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }
}
