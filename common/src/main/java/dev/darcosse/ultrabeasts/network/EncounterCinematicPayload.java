package dev.darcosse.ultrabeasts.network;

import dev.darcosse.ultrabeasts.UltraBeasts;
import dev.darcosse.ultrabeasts.cinematic.CinematicShot;
import dev.darcosse.ultrabeasts.cinematic.CinematicShotStore;
import dev.darcosse.ultrabeasts.dimension.UltraSpaceStructureManager.CinematicStyle;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-to-client order to play the encounter cinematic on arrival in Ultra-Space.
 * <p>
 * Sent once per trip; the client then plays the whole sequence on its own clock, so the
 * server keeps no cinematic state: the player is already invulnerable in Ultra-Space and the
 * Ultra Beast is already frozen (no AI), and the screen the client opens holds the player
 * still. The species travels as a translation key so each player reads it in their own
 * language and the payload needs no registry access.
 *
 * @param structureMin lowest corner of the structure's bounding box
 * @param structureMax highest corner of the structure's bounding box
 * @param beastPos     where the Ultra Beast was sent out; the camera falls back on it while
 *                     the entity has not reached the client
 * @param speciesKey   translation key of the Ultra Beast's species name
 * @param style        how the structure is presented before the close-up
 * @param shots        the tour's shots, in order; empty unless {@code style} is
 *                     {@link CinematicStyle#TOUR}
 */
public record EncounterCinematicPayload(BlockPos structureMin, BlockPos structureMax, Vec3 beastPos,
                                        String speciesKey, CinematicStyle style,
                                        List<CinematicShot> shots) implements CustomPacketPayload {

    /**
     * Wire format version, checked before anything else is read. The format is positional:
     * adding, removing or reordering a field without raising this makes a peer of another
     * build read garbage without an error. Raise it on every change to write/read.
     * <p>
     * History: 1 had structureMin, structureMax, beastPos, speciesKey. 2 added style. 3 added
     * shots, and the TOUR style.
     */
    public static final int WIRE_VERSION = 3;

    /**
     * Longest translation key accepted, in characters. Species keys are short; the bound
     * keeps a forged packet from making the client allocate a huge string.
     */
    private static final int MAX_KEY_LENGTH = 256;

    public static final CustomPacketPayload.Type<EncounterCinematicPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(UltraBeasts.MOD_ID, "encounter_cinematic"));

    public static final StreamCodec<FriendlyByteBuf, EncounterCinematicPayload> CODEC =
            StreamCodec.of((FriendlyByteBuf buf, EncounterCinematicPayload payload) -> payload.write(buf),
                    EncounterCinematicPayload::read);

    /** MUST list the same fields in the same order as {@link #read}. */
    private void write(FriendlyByteBuf buf) {
        buf.writeVarInt(WIRE_VERSION);
        buf.writeBlockPos(structureMin);
        buf.writeBlockPos(structureMax);
        buf.writeDouble(beastPos.x);
        buf.writeDouble(beastPos.y);
        buf.writeDouble(beastPos.z);
        buf.writeUtf(speciesKey, MAX_KEY_LENGTH);
        buf.writeByte(style.ordinal());
        buf.writeVarInt(shots.size());
        for (CinematicShot shot : shots) {
            shot.write(buf);
        }
    }

    /** MUST list the same fields in the same order as {@link #write}. */
    private static EncounterCinematicPayload read(FriendlyByteBuf buf) {
        int version = buf.readVarInt();
        if (version != WIRE_VERSION) {
            throw new IllegalStateException("Ultra-Beasts: encounter cinematic wire version mismatch (got " + version
                    + ", expected " + WIRE_VERSION + "). Server and client run different versions of the mod.");
        }
        BlockPos structureMin = buf.readBlockPos();
        BlockPos structureMax = buf.readBlockPos();
        // Doubles, not a BlockPos: the beast stands on a block centre, half a block up.
        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();
        String speciesKey = buf.readUtf(MAX_KEY_LENGTH);
        int styleOrdinal = buf.readByte();
        CinematicStyle[] styles = CinematicStyle.values();
        if (styleOrdinal < 0 || styleOrdinal >= styles.length) {
            // A known version with an unknown constant: a build that appended a style
            // without raising the version.
            throw new IllegalStateException("Ultra-Beasts: unknown encounter cinematic style " + styleOrdinal
                    + ". Server and client run different versions of the mod.");
        }
        int shotCount = buf.readVarInt();
        if (shotCount < 0 || shotCount > CinematicShotStore.MAX_SHOTS) {
            // Bounded before allocating: a forged count must not make the client loop for long.
            throw new IllegalStateException("Ultra-Beasts: encounter cinematic with " + shotCount
                    + " shots, at most " + CinematicShotStore.MAX_SHOTS + " expected.");
        }
        List<CinematicShot> shots = new ArrayList<>(shotCount);
        for (int i = 0; i < shotCount; i++) {
            shots.add(CinematicShot.read(buf));
        }
        return new EncounterCinematicPayload(structureMin, structureMax, new Vec3(x, y, z), speciesKey,
                styles[styleOrdinal], List.copyOf(shots));
    }

    /** Type under which each loader registers this payload. */
    @Override
    public CustomPacketPayload.Type<EncounterCinematicPayload> type() {
        return TYPE;
    }
}
