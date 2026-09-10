package dev.darcosse.ultrabeasts.handler;

import dev.darcosse.ultrabeasts.registry.ModDimensions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionHand;

/**
 * Loader-independent logic. Each platform module wires the two predicates.
 */
public class UnbreakableBlocksHandler {

    /** @return true when the block break should be allowed. */
    public static boolean allowBlockBreak(Level level) {
        return !level.dimension().equals(ModDimensions.ULTRA_SPACE_DIMENSION);
    }

    /** @return true when the right click should be cancelled. */
    public static boolean shouldCancelUseBlock(Player player, Level level, InteractionHand hand) {
        if (!level.dimension().equals(ModDimensions.ULTRA_SPACE_DIMENSION)) {
            return false;
        }

        ItemStack itemStack = player.getItemInHand(hand);
        return itemStack.getItem() instanceof BlockItem;
    }
}
