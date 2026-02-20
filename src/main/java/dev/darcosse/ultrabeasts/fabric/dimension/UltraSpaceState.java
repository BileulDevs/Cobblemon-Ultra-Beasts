package dev.darcosse.ultrabeasts.fabric.dimension;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;

import java.util.ArrayList;
import java.util.List;

public class UltraSpaceState extends PersistentState {
    public List<BlockPos> structureBlocks = new ArrayList<>();

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        NbtList list = new NbtList();
        for (BlockPos pos : structureBlocks) {
            NbtCompound c = new NbtCompound();
            c.putInt("X", pos.getX());
            c.putInt("Y", pos.getY());
            c.putInt("Z", pos.getZ());
            list.add(c);
        }
        nbt.put("blocks", list);
        return nbt;
    }

    public static UltraSpaceState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        UltraSpaceState state = new UltraSpaceState();
        if (nbt.contains("blocks", 9)) {
            NbtList list = nbt.getList("blocks", 10);
            for (int i = 0; i < list.size(); i++) {
                NbtCompound c = list.getCompound(i);
                state.structureBlocks.add(new BlockPos(c.getInt("X"), c.getInt("Y"), c.getInt("Z")));
            }
        }
        return state;
    }

    public static UltraSpaceState getServerState(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(
                new PersistentState.Type<>(
                        UltraSpaceState::new,
                        UltraSpaceState::fromNbt,
                        null
                ),
                "ultrabeasts_structure"
        );
    }
}