package dev.darcosse.ultrabeasts.mixin;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import dev.darcosse.ultrabeasts.registry.ModDimensions;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets the client see the Ultra Beast from the arrival point, for the encounter cinematic's
 * close-up.
 * <p>
 * The server only sends an entity to players within its type's tracking range. Cobblemon
 * leaves Pokémon at the vanilla default of 5 chunks (80 blocks, measured horizontally),
 * while two structures put their Ultra Beast farther from the arrival point: celesteela
 * (about 91 blocks) and xurkitree (about 80). There the beast never reached the client, and
 * the close-up filmed an empty spot.
 * <p>
 * Only Pokémon in Ultra-Space are affected: a single player and a single Pokémon live there,
 * so the wider range costs nothing measurable. The value stays capped by the player's view
 * distance in {@code updatePlayer}, which this does not touch.
 */
@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public abstract class TrackedEntityMixin {

    /**
     * Tracking range granted to Pokémon in Ultra-Space, in blocks. 160 (10 chunks) covers
     * the farthest Ultra Beast with room to spare. Raising it has no effect beyond the
     * player's view distance; lowering it below 92 brings back the empty close-up on
     * celesteela.
     */
    private static final int ULTRA_SPACE_POKEMON_RANGE = 160;

    @Shadow
    @Final
    Entity entity;

    /**
     * Widens the range after vanilla computed it. Runs whenever the server re-checks who
     * sees the entity, so it only tests a type and a dimension key before leaving.
     */
    @Inject(method = "getEffectiveRange", at = @At("RETURN"), cancellable = true)
    private void ultrabeasts$widenUltraSpacePokemonRange(CallbackInfoReturnable<Integer> cir) {
        if (entity instanceof PokemonEntity
                && entity.level().dimension().equals(ModDimensions.ULTRA_SPACE_DIMENSION)
                && cir.getReturnValueI() < ULTRA_SPACE_POKEMON_RANGE) {
            cir.setReturnValue(ULTRA_SPACE_POKEMON_RANGE);
        }
    }
}
