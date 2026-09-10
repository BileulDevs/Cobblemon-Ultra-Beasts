package dev.darcosse.ultrabeasts.neoforge;

import dev.darcosse.ultrabeasts.platform.PlatformAdapter;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;

public class NeoForgePlatformAdapter implements PlatformAdapter {

    @Override
    public Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }
}
