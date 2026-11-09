package dev.darcosse.ultrabeasts.mixin.client;

import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Calls the view area's section lookup, which vanilla keeps protected. Used by
 * {@code StructurePreloader}.
 */
@Mixin(ViewArea.class)
public interface ViewAreaInvoker {

    /**
     * The render section slot for a block position. Beware: the grid wraps around
     * horizontally, so a position outside the render distance returns another section's
     * slot; check its origin. Null outside the world's height.
     */
    @Invoker("getRenderSectionAt")
    SectionRenderDispatcher.RenderSection ultrabeasts$getRenderSectionAt(BlockPos pos);
}
