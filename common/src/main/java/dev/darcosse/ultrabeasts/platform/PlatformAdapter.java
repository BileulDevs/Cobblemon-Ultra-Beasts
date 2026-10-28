package dev.darcosse.ultrabeasts.platform;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;

/**
 * Loader services the common code cannot obtain on its own. Implemented once per loader
 * module and installed through {@link Platform#set(PlatformAdapter)}.
 */
public interface PlatformAdapter {

    /** Returns the game's root config directory, as defined by the running loader. */
    Path getConfigDir();

    /**
     * Sends a payload to one player through the loader's networking. The payload type must
     * have been registered by that loader's entry point; a client that does not know it
     * simply does not receive it.
     */
    void sendToPlayer(ServerPlayer player, CustomPacketPayload payload);
}
