package dev.darcosse.ultrabeasts.neoforge;

import dev.darcosse.ultrabeasts.platform.PlatformAdapter;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.PacketDistributor;

import java.nio.file.Path;

/** NeoForge implementation of the platform services. */
public class NeoForgePlatformAdapter implements PlatformAdapter {

    /** NeoForge's config directory, usually {@code .minecraft/config}. */
    @Override
    public Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    /** Sends through NeoForge's distributor; the payload is registered as required. */
    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }
}
