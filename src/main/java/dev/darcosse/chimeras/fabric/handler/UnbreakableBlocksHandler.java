package dev.darcosse.chimeras.fabric.handler;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;

public class UnbreakableBlocksHandler {

    // RegistryKey de votre dimension des chimères
    public static final RegistryKey<World> CHIMERAS_DIMENSION =
            RegistryKey.of(RegistryKeys.WORLD, Identifier.of("cobblemon_ultrabeast", "chimeras_dimension"));

    public static void initialize() {
        // Empêcher la casse de blocs
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            // Si c'est dans votre dimension, empêcher la casse
            if (world.getRegistryKey().equals(CHIMERAS_DIMENSION)) {
                return false; // Empêche la casse
            }
            return true; // Autorise la casse dans les autres dimensions
        });

        // Empêcher le placement de blocs (optionnel)
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.getRegistryKey().equals(CHIMERAS_DIMENSION)) {
                // Autoriser seulement certaines actions, bloquer le placement
                ItemStack itemStack = player.getStackInHand(hand);

                // Autoriser l'utilisation d'items qui ne placent pas de blocs
                if (!(itemStack.getItem() instanceof BlockItem)) {
                    return ActionResult.PASS; // Autoriser l'utilisation
                }

                return ActionResult.FAIL; // Empêcher le placement de blocs
            }
            return ActionResult.PASS;
        });

//        // Empêcher les explosions de détruire les blocs
//        ExplosionEvents.BEFORE.register((world, explosion) -> {
//            if (world.getRegistryKey().equals(CHIMERAS_DIMENSION)) {
//                return false; // Empêche les dégâts d'explosion
//            }
//            return true;
//        });
    }
}