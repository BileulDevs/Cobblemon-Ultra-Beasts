package dev.darcosse.ultrabeasts.handler;

import com.cobblemon.mod.common.api.events.pokemon.PokedexDataChangedEvent;
import com.cobblemon.mod.common.api.pokedex.PokedexEntryProgress;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.util.PlayerExtensionsKt;
import dev.darcosse.ultrabeasts.UltraBeasts;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Ticks off one criterion of the "Ultra-Space Master" advancement each time a player's
 * Pokédex records an Ultra Beast species as owned.
 * <p>
 * Server side only: Pokédex events are fired by the server.
 */
public class CaptureUltraBeastHandler {

    /**
     * Advancement whose criteria are named after each Ultra Beast species, in lower case.
     * Must match {@code data/cobblemon_ultrabeast/advancement/ultra_beast_master.json}.
     */
    private static final ResourceLocation ULTRA_BEAST_MASTER_ID =
            ResourceLocation.fromNamespaceAndPath(UltraBeasts.MOD_ID, "ultra_beast_master");

    /**
     * Builds the listener subscribed to {@code CobblemonEvents.POKEDEX_DATA_CHANGED_POST}.
     *
     * @return a Kotlin function, since that is what Cobblemon's event bus expects
     */
    public static Function1<? super PokedexDataChangedEvent.Post, Unit> initialize() {
        return event -> {
            Pokemon pokemon = event.getDataSource().getPokemon();

            if (!pokemon.isUltraBeast()) {
                return Unit.INSTANCE;
            }

            if (event.getPokedexManager().getKnowledgeForSpecies(pokemon.getSpecies().resourceIdentifier)
                    != PokedexEntryProgress.OWNED) {
                return Unit.INSTANCE;
            }

            // The player is the owner of the Pokédex, not of the Pokémon. This event also fires
            // when a player merely SEES a wild Ultra Beast; that Pokémon has no owner, so the
            // previous getOwnerPlayer() lookup returned null and crashed on getServer() as soon
            // as the species was already owned in the Pokédex.
            ServerPlayer player = PlayerExtensionsKt.getPlayer(event.getPlayerUUID());
            if (player == null) {
                return Unit.INSTANCE;
            }

            AdvancementHolder advancement = player.getServer().getAdvancements().get(ULTRA_BEAST_MASTER_ID);
            if (advancement != null) {
                String criterion = pokemon.getSpecies().getName().toLowerCase();
                player.getAdvancements().award(advancement, criterion);
            }

            return Unit.INSTANCE;
        };
    }
}
