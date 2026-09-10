package dev.darcosse.ultrabeasts.handler;

import com.cobblemon.mod.common.api.events.pokemon.PokedexDataChangedEvent;
import com.cobblemon.mod.common.api.pokedex.PokedexEntryProgress;
import com.cobblemon.mod.common.pokemon.Pokemon;
import dev.darcosse.ultrabeasts.UltraBeasts;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class CaptureUltraBeastHandler {

    public static Function1<? super PokedexDataChangedEvent.Post, Unit> initialize() {
        return event -> {
            ServerPlayer player = event.getDataSource().getPokemon().getOwnerPlayer();

            Pokemon pokemon = event.getDataSource().getPokemon();

            if (pokemon.isUltraBeast()
                    && event.getPokedexManager().getKnowledgeForSpecies(pokemon.getSpecies().resourceIdentifier)
                    == PokedexEntryProgress.OWNED) {

                ResourceLocation advancementId =
                        ResourceLocation.fromNamespaceAndPath(UltraBeasts.MOD_ID, "ultra_beast_master");
                AdvancementHolder advancement = player.getServer().getAdvancements().get(advancementId);

                if (advancement != null) {
                    String criterion = pokemon.getSpecies().getName().toLowerCase();
                    player.getAdvancements().award(advancement, criterion);
                }
            }

            return Unit.INSTANCE;
        };
    }
}
