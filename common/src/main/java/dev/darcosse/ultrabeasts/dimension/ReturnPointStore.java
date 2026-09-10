package dev.darcosse.ultrabeasts.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Where each player was standing before entering Ultra-Space.
 *
 * This used to be a static HashMap on WormholeEntity, which meant every return
 * point was lost on server restart: a player who logged out inside Ultra-Space
 * came back at world spawn instead of where they left. Persisting it on the
 * overworld fixes that, and the data is tiny (one UUID + one position each).
 */
public class ReturnPointStore extends SavedData {

    private final Map<UUID, BlockPos> returnPoints = new HashMap<>();

    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
        ListTag list = new ListTag();

        returnPoints.forEach((uuid, pos) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("player", uuid);
            entry.put("pos", NbtUtils.writeBlockPos(pos));
            list.add(entry);
        });

        nbt.put("return_points", list);
        return nbt;
    }

    public static ReturnPointStore load(CompoundTag nbt, HolderLookup.Provider registries) {
        ReturnPointStore store = new ReturnPointStore();

        if (nbt.contains("return_points", Tag.TAG_LIST)) {
            ListTag list = nbt.getList("return_points", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                if (!entry.hasUUID("player")) continue;

                NbtUtils.readBlockPos(entry, "pos").ifPresent(pos ->
                        store.returnPoints.put(entry.getUUID("player"), pos));
            }
        }

        return store;
    }

    private static final SavedData.Factory<ReturnPointStore> FACTORY =
            new SavedData.Factory<>(ReturnPointStore::new, ReturnPointStore::load, null);

    public static ReturnPointStore get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(FACTORY, "ultrabeasts_return_points");
    }

    public void put(UUID player, BlockPos pos) {
        returnPoints.put(player, pos);
        setDirty();
    }

    public BlockPos remove(UUID player) {
        BlockPos pos = returnPoints.remove(player);
        if (pos != null) setDirty();
        return pos;
    }
}
