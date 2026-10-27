package dev.darcosse.ultrabeasts.neoforge;

import dev.darcosse.ultrabeasts.platform.PlatformAdapter;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;

/** NeoForge implementation of the platform services. */
public class NeoForgePlatformAdapter implements PlatformAdapter {

    /** NeoForge's config directory, usually {@code .minecraft/config}. */
    @Override
    public Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }
}
