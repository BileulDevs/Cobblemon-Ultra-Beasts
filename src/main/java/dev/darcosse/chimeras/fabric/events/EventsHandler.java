package dev.darcosse.chimeras.fabric.events;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import dev.darcosse.chimeras.fabric.Chimeras;
import dev.darcosse.chimeras.fabric.handler.CaptureChimeraHandler;

public class EventsHandler {
    public static void initializeEvents() {
        CobblemonEvents.POKEDEX_DATA_CHANGED_POST.subscribe(Priority.HIGHEST, CaptureChimeraHandler.registerGrantChimerasAdvancements());
        Chimeras.LOGGER.info("Logged Wormhole events");
    }
}
