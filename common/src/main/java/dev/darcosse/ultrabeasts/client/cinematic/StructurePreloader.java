package dev.darcosse.ultrabeasts.client.cinematic;

import dev.darcosse.ultrabeasts.mixin.client.LevelRendererAccessor;
import dev.darcosse.ultrabeasts.mixin.client.ViewAreaInvoker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Gets the structure fully drawn before the encounter cinematic films it.
 * <p>
 * Minecraft only builds the meshes of the sections the camera currently sees, and only once
 * their light has arrived. Left alone, the parts of the structure behind the player at arrival
 * are built in the middle of the fly-over, and pop in on screen. This class does it up front,
 * whatever the camera sees, in three steps:
 * <ol>
 *   <li>CHUNKS: every chunk of the structure has reached the client;</li>
 *   <li>LIGHT: every section of the structure has its light (built without it, a section
 *   would show dark, then be rebuilt bright during the fly-over);</li>
 *   <li>BUILD: every section of the structure is sent to the renderer's builder, exactly as
 *   the renderer does for the sections it sees, and the step ends once all of them are built.</li>
 * </ol>
 * Sections beyond the player's render distance are left out: the server never sends them and
 * the renderer has no slot for them.
 * <p>
 * Driven by {@link ArrivalLoadingScreen}, once per tick while Minecraft's terrain loading
 * screen is held.
 * <p>
 * Client only, render thread only. Cost: one pass over the structure's sections per tick
 * while the loading screen is held, a few hundred lookups for a large structure; and the
 * meshes themselves, built on the renderer's worker threads, which the fly-over would have
 * built anyway. Nothing once the cinematic rolls.
 */
final class StructurePreloader {

    /** Steps, in order. */
    enum Step { CHUNKS, LIGHT, BUILD, DONE }

    /**
     * Sections sent to the builder per call (per tick) at most. All at once would queue a
     * few hundred tasks at once, a hitch even behind the loading screen; 32 per tick still
     * sends a large structure in under half a second.
     */
    private static final int BUILDS_PER_CALL = 32;

    private final BlockPos min;
    private final BlockPos max;
    private final BlockPos beast;
    private final ChunkPos viewerChunk;

    private Step step = Step.CHUNKS;

    /** Origins of the structure's sections the renderer has a slot for; resolved at the BUILD step. */
    private List<BlockPos> sections;
    /** Section origins already sent to the builder, as packed longs. */
    private final Set<Long> requested = new HashSet<>();
    /** Sections built at the last check, for the log. */
    private int built;

    /**
     * @param min    lowest corner of the structure
     * @param max    highest corner of the structure
     * @param beast  where the Ultra Beast stands, whose chunk is awaited too
     * @param viewer where the player stands: the render distance counts from there
     */
    StructurePreloader(BlockPos min, BlockPos max, Vec3 beast, Vec3 viewer) {
        this.min = min;
        this.max = max;
        this.beast = BlockPos.containing(beast);
        this.viewerChunk = new ChunkPos(BlockPos.containing(viewer));
    }

    /** Advances as far as possible. Returns true once the structure is fully built. */
    boolean update() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) return false;

        if (step == Step.CHUNKS && chunksArrived(minecraft, level)) {
            step = Step.LIGHT;
        }
        if (step == Step.LIGHT && lightArrived(minecraft, level)) {
            step = Step.BUILD;
        }
        if (step == Step.BUILD && build(minecraft.levelRenderer)) {
            step = Step.DONE;
        }
        return step == Step.DONE;
    }

    /** One line for the log: the step reached and the sections built. */
    String summary() {
        int total = sections == null ? 0 : sections.size();
        return "step " + step + ", " + built + "/" + total + " sections built";
    }

    // ---- steps -----------------------------------------------------------

    /** Whether every chunk column of the structure in render distance, and the beast's, is in. */
    private boolean chunksArrived(Minecraft minecraft, ClientLevel level) {
        int renderDistance = minecraft.options.getEffectiveRenderDistance();
        for (int cx = SectionPos.blockToSectionCoord(min.getX()); cx <= SectionPos.blockToSectionCoord(max.getX()); cx++) {
            for (int cz = SectionPos.blockToSectionCoord(min.getZ()); cz <= SectionPos.blockToSectionCoord(max.getZ()); cz++) {
                if (viewerChunk.getChessboardDistance(new ChunkPos(cx, cz)) > renderDistance) continue;
                if (!level.hasChunk(cx, cz)) return false;
            }
        }
        return level.hasChunkAt(beast);
    }

    /** Whether every section of the structure in render distance has its light. */
    private boolean lightArrived(Minecraft minecraft, ClientLevel level) {
        int renderDistance = minecraft.options.getEffectiveRenderDistance();
        for (BlockPos origin : sectionOrigins()) {
            if (viewerChunk.getChessboardDistance(new ChunkPos(origin)) > renderDistance) continue;
            if (!level.getLightEngine().lightOnInSection(SectionPos.of(origin))) return false;
        }
        return true;
    }

    /**
     * Sends the structure's sections to the builder, a batch per call, then waits until
     * they are all built. The same two calls the renderer makes for a section it sees:
     * build, then mark clean, so it does not build it a second time when it comes into view.
     */
    private boolean build(LevelRenderer renderer) {
        ViewArea viewArea = ((LevelRendererAccessor) renderer).ultrabeasts$getViewArea();
        if (viewArea == null) return false;

        if (sections == null) {
            sections = new ArrayList<>();
            for (BlockPos origin : sectionOrigins()) {
                if (slotFor(viewArea, origin) != null) {
                    sections.add(origin);
                }
            }
        }

        SectionRenderDispatcher dispatcher = renderer.getSectionRenderDispatcher();
        RenderRegionCache cache = null;
        int sent = 0;
        built = 0;
        for (BlockPos origin : sections) {
            if (renderer.isSectionCompiled(origin)) {
                built++;
                continue;
            }
            if (sent >= BUILDS_PER_CALL || requested.contains(origin.asLong())) continue;

            SectionRenderDispatcher.RenderSection section = slotFor(viewArea, origin);
            if (section == null) continue;
            if (cache == null) {
                cache = new RenderRegionCache();
            }
            section.rebuildSectionAsync(dispatcher, cache);
            section.setNotDirty();
            requested.add(origin.asLong());
            sent++;
        }
        return built >= sections.size();
    }

    // ---- geometry --------------------------------------------------------

    /** Origins of every section the structure's box touches, bottom to top. */
    private List<BlockPos> sectionOrigins() {
        List<BlockPos> origins = new ArrayList<>();
        for (int sy = SectionPos.blockToSectionCoord(min.getY()); sy <= SectionPos.blockToSectionCoord(max.getY()); sy++) {
            for (int sx = SectionPos.blockToSectionCoord(min.getX()); sx <= SectionPos.blockToSectionCoord(max.getX()); sx++) {
                for (int sz = SectionPos.blockToSectionCoord(min.getZ()); sz <= SectionPos.blockToSectionCoord(max.getZ()); sz++) {
                    origins.add(SectionPos.of(sx, sy, sz).origin());
                }
            }
        }
        return origins;
    }

    /**
     * The renderer's section for an origin, or null if it has none. The grid wraps around
     * horizontally: outside the render distance the lookup returns the slot of another
     * section, which the origin check rejects.
     */
    private static SectionRenderDispatcher.RenderSection slotFor(ViewArea viewArea, BlockPos origin) {
        SectionRenderDispatcher.RenderSection section = ((ViewAreaInvoker) viewArea).ultrabeasts$getRenderSectionAt(origin);
        return section != null && section.getOrigin().equals(origin) ? section : null;
    }
}
