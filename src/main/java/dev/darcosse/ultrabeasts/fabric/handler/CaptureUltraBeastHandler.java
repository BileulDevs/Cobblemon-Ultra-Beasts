package dev.darcosse.ultrabeasts.fabric.handler;

import com.cobblemon.mod.common.api.events.pokemon.PokedexDataChangedEvent;
import com.cobblemon.mod.common.api.pokedex.PokedexEntryProgress;
import com.cobblemon.mod.common.api.pokedex.entry.PokedexEntry;
import com.cobblemon.mod.common.pokemon.Pokemon;
import dev.darcosse.ultrabeasts.fabric.UltraBeasts;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;


public class CaptureUltraBeastHandler {
    public static Function1<? super PokedexDataChangedEvent.Post, Unit> initialize() {
        return event -> {
            ServerPlayerEntity player = event.getDataSource().getPokemon().getOwnerPlayer();

            Pokemon pokemon = event.getDataSource().getPokemon();

            if (pokemon.isUltraBeast() && event.getPokedexManager().getKnowledgeForSpecies(pokemon.getSpecies().resourceIdentifier) == PokedexEntryProgress.OWNED) {

                Identifier advancementId = Identifier.of(UltraBeasts.MOD_ID, "ultra_beast_master");
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
