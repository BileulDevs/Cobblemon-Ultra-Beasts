package dev.darcosse.ultrabeasts.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Persisted state of the single Ultra-Space instance: the structure in place, its exit
 * portal, and who currently holds the dimension. Saved with the Ultra-Space level.
 * <p>
 * The occupancy lock is a lease, not a token: the server owns it, exactly one player can
 * hold it, and it survives a restart. Every check reads the game time from the same place,
 * so two players can never both consider themselves legitimate.
 * <p>
 * Volumes are stored as bounding boxes rather than one BlockPos per block. For celesteela
 * (8 parts of 48x48x48) the old format meant ~885 000 NBT entries written on every save;
 * structure parts are axis-aligned cuboids, so two corners carry the same information.
 * <p>
 * The NBT keys below are a save format: renaming one makes existing worlds forget their
 * structure or their occupant.
 */
public class UltraSpaceState extends SavedData {

    /** Name of the data file under the level's {@code data} folder. */
    private static final String DATA_NAME = "ultrabeasts_structure";

    /**
     * An inclusive axis-aligned volume.
     *
     * @param min lowest corner, inclusive
     * @param max highest corner, inclusive
     */
    public record Region(BlockPos min, BlockPos max) {
        /** Serialises the two corners as int arrays. */
        CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putIntArray("min", new int[]{min.getX(), min.getY(), min.getZ()});
            tag.putIntArray("max", new int[]{max.getX(), max.getY(), max.getZ()});
            return tag;
        }

        /** @return null when the tag is malformed, so a corrupt entry is skipped. */
        static Region fromTag(CompoundTag tag) {
            int[] min = tag.getIntArray("min");
            int[] max = tag.getIntArray("max");
            if (min.length != 3 || max.length != 3) return null;
            return new Region(
                    new BlockPos(min[0], min[1], min[2]),
                    new BlockPos(max[0], max[1], max[2])
            );
        }
    }

    /** Volumes of the structure currently in place; empty when none is up. */
    private final List<Region> regions = new ArrayList<>();

    /**
     * Where the exit portal belongs for the structure currently in place,
     * or null when no structure is up.
     */
    private BlockPos returnPortalPos;

    /** Player currently holding the dimension, or null when it is free. */
    private UUID occupant;

    /**
     * Game tick at which a disconnected occupant loses their claim.
     *
     * 0 means "no countdown": the occupant is online, and their claim does not
     * expire however long they stay. The countdown only ever runs while they
     * are logged out.
     */
    private long graceEndsAt;

    /** Writes the regions, the exit portal position and the lease. */
    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Region region : regions) {
            list.add(region.toTag());
        }
        nbt.put("regions", list);

        if (returnPortalPos != null) {
            nbt.put("return_portal", NbtUtils.writeBlockPos(returnPortalPos));
        }

        if (occupant != null) {
            nbt.putUUID("occupant", occupant);
            nbt.putLong("grace_ends_at", graceEndsAt);
        }

        return nbt;
    }

    /** Reads what {@link #save} wrote. Malformed region entries are skipped, not fatal. */
    public static UltraSpaceState load(CompoundTag nbt, HolderLookup.Provider registries) {
        UltraSpaceState state = new UltraSpaceState();

        if (nbt.contains("regions", Tag.TAG_LIST)) {
            ListTag list = nbt.getList("regions", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                Region region = Region.fromTag(list.getCompound(i));
                if (region != null) {
                    state.regions.add(region);
                }
            }
        }

        NbtUtils.readBlockPos(nbt, "return_portal")
                .ifPresent(pos -> state.returnPortalPos = pos);

        if (nbt.hasUUID("occupant")) {
            state.occupant = nbt.getUUID("occupant");
            state.graceEndsAt = nbt.getLong("grace_ends_at");
        }

        return state;
    }

    private static final SavedData.Factory<UltraSpaceState> FACTORY =
            new SavedData.Factory<>(UltraSpaceState::new, UltraSpaceState::load, null);

    /** Returns the state stored in Ultra-Space, creating an empty one on first use. */
    public static UltraSpaceState getServerState(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    // ---- structure -------------------------------------------------------

    /** Read-only view of the structure's volumes. */
    public List<Region> getRegions() {
        return Collections.unmodifiableList(regions);
    }

    /** Records one more volume of the structure being placed. */
    public void addRegion(Region region) {
        regions.add(region);
        setDirty();
    }

    /** Forgets every volume: no structure is considered in place afterwards. */
    public void clearRegions() {
        regions.clear();
        setDirty();
    }

    /** @return null when no structure is currently in place. */
    public BlockPos getReturnPortalPos() {
        return returnPortalPos;
    }

    /** Pass null when tearing the structure down. */
    public void setReturnPortalPos(BlockPos pos) {
        this.returnPortalPos = pos;
        setDirty();
    }

    /** Whether a structure is currently standing. */
    public boolean hasStructure() {
        return !regions.isEmpty();
    }

    // ---- occupancy lease -------------------------------------------------

    /**
     * Is the dimension claimed by someone right now?
     *
     * @param gameTime current game time, from the overworld
     */
    public boolean isClaimed(long gameTime) {
        // Not a pure getter: an expired lease is cleared on the spot, so every caller sees the
        // same answer afterwards.
        if (occupant == null) return false;

        // graceEndsAt == 0 means the occupant is online: no expiry.
        if (graceEndsAt == 0) return true;

        if (gameTime < graceEndsAt) return true;

        // Lease expired while they were away.
        clearClaim();
        return false;
    }

    /** @return null when free, or once the lease has expired. */
    public UUID getOccupant(long gameTime) {
        return isClaimed(gameTime) ? occupant : null;
    }

    /** Whether this player holds a valid lease right now. */
    public boolean isOccupant(UUID player, long gameTime) {
        return isClaimed(gameTime) && occupant.equals(player);
    }

    /** Takes the lease for a player who is online. */
    public void claim(UUID player) {
        this.occupant = player;
        this.graceEndsAt = 0;
        setDirty();
    }

    /** Starts the countdown: the occupant just went offline. */
    public void beginGrace(long gameTime, int graceTicks) {
        if (occupant == null) return;
        this.graceEndsAt = gameTime + graceTicks;
        setDirty();
    }

    /** Stops the countdown: the occupant came back in time. */
    public void endGrace() {
        if (occupant == null) return;
        this.graceEndsAt = 0;
        setDirty();
    }

    /** Frees the dimension. */
    public void clearClaim() {
        this.occupant = null;
        this.graceEndsAt = 0;
        setDirty();
    }
}
