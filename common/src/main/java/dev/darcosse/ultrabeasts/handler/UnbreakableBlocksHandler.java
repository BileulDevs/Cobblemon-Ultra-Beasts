package dev.darcosse.ultrabeasts.handler;

import dev.darcosse.ultrabeasts.registry.ModDimensions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionHand;

/**
 * Keeps Ultra-Space structures intact: blocks cannot be broken or placed there.
 * <p>
 * Loader-independent predicates; each loader module wires them to its own events. Server
 * side only.
 */
public final class UnbreakableBlocksHandler {

    /** Static predicates only, never instantiated. */
    private UnbreakableBlocksHandler() {
    }

    /** @return true when breaking a block in this level is allowed */
    public static boolean allowBlockBreak(Level level) {
        return !level.dimension().equals(ModDimensions.ULTRA_SPACE_DIMENSION);
    }

    /**
     * Blocks placement only: a right click with anything but a block item still goes through,
     * so doors, levers and Poké Balls keep working inside the structures.
     *
     * @return true when the right click should be cancelled
     */
    public static boolean shouldCancelUseBlock(Player player, Level level, InteractionHand hand) {
        if (!level.dimension().equals(ModDimensions.ULTRA_SPACE_DIMENSION)) {
            return false;
        }

        ItemStack itemStack = player.getItemInHand(hand);
        return itemStack.getItem() instanceof BlockItem;
    }
}
