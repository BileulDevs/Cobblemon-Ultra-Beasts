package dev.darcosse.chimeras.fabric.handler;

import com.cobblemon.mod.common.api.events.pokemon.PokemonCapturedEvent;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;


public class CaptureChimeraHandler {
    public static Function1<? super PokemonCapturedEvent, Unit> registerGrantChimerasAdvancements() {
        return event -> {
            ServerPlayerEntity player = event.getPlayer();

            if (event.getPokemon().isUltraBeast()) {
                Identifier advancementId = Identifier.of("cobblemon_chimeras", "ultra_beast_master");
                AdvancementEntry advancement = player.getServer().getAdvancementLoader().get(advancementId);

                if (advancement != null) {
                    String criterion = event.getPokemon().getSpecies().getName().toLowerCase();

                    player.getAdvancementTracker().grantCriterion(advancement, criterion);
                }
            }

            return Unit.INSTANCE;
        };
    }

}
