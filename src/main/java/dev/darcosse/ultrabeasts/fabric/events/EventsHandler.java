package dev.darcosse.ultrabeasts.fabric.events;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import dev.darcosse.ultrabeasts.fabric.UltraBeasts;
import dev.darcosse.ultrabeasts.fabric.handler.CaptureChimeraHandler;

public class EventsHandler {
    public static void initializeEvents() {
        CobblemonEvents.POKEDEX_DATA_CHANGED_POST.subscribe(Priority.HIGHEST, CaptureChimeraHandler.registerGrantChimerasAdvancements());

        UltraBeasts.LOGGER.info("Registered Ultra-Beasts events");
    }
}
