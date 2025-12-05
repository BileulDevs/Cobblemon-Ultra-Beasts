package dev.darcosse.chimeras.fabric.handler;

import com.cobblemon.mod.common.api.events.pokemon.PokedexDataChangedEvent;
import com.cobblemon.mod.common.api.events.pokemon.PokemonCapturedEvent;
import com.cobblemon.mod.common.api.pokedex.PokedexEntryProgress;
import com.cobblemon.mod.common.api.pokedex.PokedexManager;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;


public class CaptureChimeraHandler {
    public static Function1<? super PokedexDataChangedEvent.Post, Unit> registerGrantChimerasAdvancements() {
        return event -> {
            ServerPlayerEntity player = event.getDataSource().getPokemon().getOwnerPlayer();

            Pokemon pokemon = event.getDataSource().getPokemon();

            if (pokemon.isUltraBeast() && event.getPokedexManager().getKnowledgeForSpecies(pokemon.getSpecies().resourceIdentifier) == PokedexEntryProgress.CAUGHT) {

                Identifier advancementId = Identifier.of("cobblemon_ultrabeast", "ultra_beast_master");
                AdvancementEntry advancement = player.getServer().getAdvancementLoader().get(advancementId);

                if (advancement != null) {
                    String criterion = pokemon.getSpecies().getName().toLowerCase();
                    player.getAdvancementTracker().grantCriterion(advancement, criterion);
                }
            }

            return Unit.INSTANCE;
        };
    }
}
