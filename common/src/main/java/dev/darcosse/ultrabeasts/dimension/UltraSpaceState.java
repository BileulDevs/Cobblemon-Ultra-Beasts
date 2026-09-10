package dev.darcosse.ultrabeasts.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

public class UltraSpaceState extends SavedData {

    public List<BlockPos> structureBlocks = new ArrayList<>();

    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (BlockPos pos : structureBlocks) {
            CompoundTag c = new CompoundTag();
            c.putInt("X", pos.getX());
            c.putInt("Y", pos.getY());
            c.putInt("Z", pos.getZ());
            list.add(c);
        }
        nbt.put("blocks", list);
        return nbt;
    }

    public static UltraSpaceState load(CompoundTag nbt, HolderLookup.Provider registries) {
        UltraSpaceState state = new UltraSpaceState();
        if (nbt.contains("blocks", Tag.TAG_LIST)) {
            ListTag list = nbt.getList("blocks", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag c = list.getCompound(i);
                state.structureBlocks.add(new BlockPos(c.getInt("X"), c.getInt("Y"), c.getInt("Z")));
            }
        }
        return state;
    }

    private static final SavedData.Factory<UltraSpaceState> FACTORY =
            new SavedData.Factory<>(UltraSpaceState::new, UltraSpaceState::load, null);

    public static UltraSpaceState getServerState(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, "ultrabeasts_structure");
    }
}
