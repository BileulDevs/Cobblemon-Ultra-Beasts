package dev.darcosse.ultrabeasts.fabric;

import dev.darcosse.ultrabeasts.platform.PlatformAdapter;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public class FabricPlatformAdapter implements PlatformAdapter {

    @Override
    public Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }
}
