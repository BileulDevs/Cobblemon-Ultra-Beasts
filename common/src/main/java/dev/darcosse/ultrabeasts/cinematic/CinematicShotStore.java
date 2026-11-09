package dev.darcosse.ultrabeasts.cinematic;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.darcosse.ultrabeasts.UltraBeasts;
import dev.darcosse.ultrabeasts.platform.Platform;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Where each structure's tour shots come from, and where the recording commands put them.
 * <p>
 * Two sources, the first found wins:
 * <ol>
 *   <li>{@code config/ultrabeasts/cinematics/<structure>.json}, written by
 *   {@code /ultrabeasts cinematic}: the shots recorded on this server;</li>
 *   <li>{@code data/cobblemon_ultrabeast/cinematic/<structure>.json}, shipped with the mod
 *   (and replaceable by a datapack): the defaults.</li>
 * </ol>
 * To make recorded shots the mod's defaults, copy the config file over the bundled one.
 * <p>
 * Server only. Read once per trip into Ultra-Space, a file of a few hundred bytes.
 */
public final class CinematicShotStore {

    /**
     * Version of the JSON format, written in every file and checked on read. Raise it on any
     * change to the fields; a file of another version is ignored with a warning.
     */
    public static final int FORMAT_VERSION = 1;

    /**
     * Most shots a tour may hold. Also bounds what the payload accepts: raising it lengthens
     * the tour the player cannot skip past without pressing a key.
     */
    public static final int MAX_SHOTS = 8;

    private static final String CONFIG_FOLDER = "ultrabeasts";
    private static final String CINEMATICS_FOLDER = "cinematics";
    private static final String RESOURCE_FOLDER = "cinematic/";
    private static final String EXTENSION = ".json";

    /** Milliseconds per second, for the "seconds" field humans edit. */
    private static final double MS_PER_SECOND = 1000.0;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Static helpers only, never instantiated. */
    private CinematicShotStore() {
    }

    /**
     * The structure's shots: the recorded ones, or the bundled defaults, or an empty list if
     * there are neither or both are broken. Never throws.
     */
    public static List<CinematicShot> load(MinecraftServer server, String structureKey) {
        Path file = overrideFile(structureKey);
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                return parse(reader, file.toString());
            } catch (IOException | RuntimeException e) {
                UltraBeasts.LOGGER.warn("Could not read cinematic shots from {}, using the defaults.", file, e);
            }
        }
        return loadDefaults(server, structureKey);
    }

    /** The bundled shots of the structure, ignoring any recorded file. */
    public static List<CinematicShot> loadDefaults(MinecraftServer server, String structureKey) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(UltraBeasts.MOD_ID,
                RESOURCE_FOLDER + structureKey + EXTENSION);
        Optional<Resource> resource = server.getResourceManager().getResource(id);
        if (resource.isEmpty()) {
            return List.of();
        }
        try (Reader reader = resource.get().openAsReader()) {
            return parse(reader, id.toString());
        } catch (IOException | RuntimeException e) {
            UltraBeasts.LOGGER.warn("Could not read the default cinematic shots {}.", id, e);
            return List.of();
        }
    }

    /** Whether shots were recorded for the structure on this server. */
    public static boolean hasRecorded(String structureKey) {
        return Files.exists(overrideFile(structureKey));
    }

    /** Writes the structure's recorded shots, replacing the previous file. */
    public static void save(String structureKey, List<CinematicShot> shots) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("version", FORMAT_VERSION);
        JsonArray array = new JsonArray();
        for (CinematicShot shot : shots) {
            JsonObject entry = new JsonObject();
            entry.add("from", poseToJson(shot.from()));
            entry.add("to", poseToJson(shot.to()));
            entry.addProperty("seconds", shot.durationMs() / MS_PER_SECOND);
            array.add(entry);
        }
        root.add("shots", array);

        Path file = overrideFile(structureKey);
        Files.createDirectories(file.getParent());
        Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
    }

    /** Deletes the recorded shots, so the defaults apply again. Returns whether a file was there. */
    public static boolean reset(String structureKey) throws IOException {
        return Files.deleteIfExists(overrideFile(structureKey));
    }

    /** The recorded file of a structure, whether or not it exists. */
    public static Path overrideFile(String structureKey) {
        return Platform.get().getConfigDir().resolve(CONFIG_FOLDER).resolve(CINEMATICS_FOLDER)
                .resolve(structureKey + EXTENSION);
    }

    /** Reads a file's shots, at most {@link #MAX_SHOTS}; throws on a malformed file. */
    private static List<CinematicShot> parse(Reader reader, String source) {
        JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
        int version = root.has("version") ? root.get("version").getAsInt() : -1;
        if (version != FORMAT_VERSION) {
            UltraBeasts.LOGGER.warn("Cinematic shots {} have format version {}, expected {}: ignored.",
                    source, version, FORMAT_VERSION);
            return List.of();
        }
        List<CinematicShot> shots = new ArrayList<>();
        for (JsonElement element : root.getAsJsonArray("shots")) {
            if (shots.size() >= MAX_SHOTS) {
                UltraBeasts.LOGGER.warn("Cinematic shots {} hold more than {} shots: the rest are ignored.",
                        source, MAX_SHOTS);
                break;
            }
            JsonObject entry = element.getAsJsonObject();
            int durationMs = (int) Math.round(entry.get("seconds").getAsDouble() * MS_PER_SECOND);
            shots.add(new CinematicShot(poseFromJson(entry.getAsJsonObject("from")),
                    poseFromJson(entry.getAsJsonObject("to")), durationMs));
        }
        return List.copyOf(shots);
    }

    /** A pose as {"x", "y", "z", "yaw", "pitch"}, rounded to keep the file readable. */
    private static JsonObject poseToJson(CinematicShot.Pose pose) {
        JsonObject json = new JsonObject();
        json.addProperty("x", round(pose.position().x));
        json.addProperty("y", round(pose.position().y));
        json.addProperty("z", round(pose.position().z));
        json.addProperty("yaw", round(pose.yaw()));
        json.addProperty("pitch", round(pose.pitch()));
        return json;
    }

    /** Reads a pose written by {@link #poseToJson}. */
    private static CinematicShot.Pose poseFromJson(JsonObject json) {
        Vec3 position = new Vec3(json.get("x").getAsDouble(), json.get("y").getAsDouble(), json.get("z").getAsDouble());
        return new CinematicShot.Pose(position, json.get("yaw").getAsFloat(), json.get("pitch").getAsFloat());
    }

    /** Two decimals: a hundredth of a block or a degree is invisible on screen. */
    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
