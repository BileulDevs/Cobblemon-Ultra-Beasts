package dev.darcosse.ultrabeasts.mixin.client;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ViewArea;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reads the level renderer's grid of render sections, which vanilla keeps private. Used by
 * {@code StructurePreloader} to build the structure's sections before the encounter
 * cinematic films them.
 */
@Mixin(LevelRenderer.class)
public interface LevelRendererAccessor {

    /** The grid of render sections around the camera; null before the first level loads. */
    @Accessor("viewArea")
    ViewArea ultrabeasts$getViewArea();
}
