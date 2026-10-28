package dev.darcosse.ultrabeasts.fabric;

import dev.darcosse.ultrabeasts.platform.PlatformAdapter;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;

/** Fabric implementation of the platform services. */
public class FabricPlatformAdapter implements PlatformAdapter {

    /** Fabric's config directory, usually {@code .minecraft/config}. */
    @Override
    public Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    /**
     * Sends through Fabric API, skipping clients that did not declare the payload: sending
     * to one would throw, and a client without the mod has nothing to play anyway.
     */
    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        if (ServerPlayNetworking.canSend(player, payload.type())) {
            ServerPlayNetworking.send(player, payload);
        }
    }
}
