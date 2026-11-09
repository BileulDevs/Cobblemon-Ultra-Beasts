package dev.darcosse.ultrabeasts.registry;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.darcosse.ultrabeasts.cinematic.CinematicShot;
import dev.darcosse.ultrabeasts.cinematic.CinematicShotStore;
import dev.darcosse.ultrabeasts.dimension.UltraSpaceStructureManager;
import dev.darcosse.ultrabeasts.entity.WormholeEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * {@code /ultrabeasts cinematic <ultra_beast> ...}: records the shots of a structure's tour
 * from the operator's own camera, and replays the cinematic to check them.
 * <ul>
 *   <li>{@code start}: marks where the next shot starts (position and look);</li>
 *   <li>{@code end <seconds>}: ends it where the operator now stands, and saves it;</li>
 *   <li>{@code undo}: removes the last recorded shot;</li>
 *   <li>{@code reset}: deletes the recorded shots, back to the mod's defaults;</li>
 *   <li>{@code list}: lists the shots in use;</li>
 *   <li>{@code play}: plays the cinematic again, as on arrival.</li>
 * </ul>
 * The first shot recorded for a structure replaces its defaults rather than adding to them:
 * a tour is designed as a whole. Recording happens in spectator mode, flying through the
 * structure. Server side; shots are saved by {@link CinematicShotStore}.
 * <p>
 * Not registered at the moment: see {@code ModCommands.CINEMATIC_COMMANDS_ENABLED}. Shots
 * then come only from the bundled files or from hand-written config files.
 */
final class CinematicCommands {

    private static final String ULTRA_BEAST_ARGUMENT = "ultra_beast";
    private static final String SECONDS_ARGUMENT = "seconds";

    /** Milliseconds per second, for the duration argument. */
    private static final double MS_PER_SECOND = 1000.0;

    /**
     * Shot starts marked and not yet ended, per operator. A few entries at most, one per
     * operator recording; an entry left by someone who logged out is a few dozen bytes and is
     * overwritten by their next {@code start}.
     */
    private static final Map<UUID, PendingStart> PENDING = new HashMap<>();

    /** A marked start, and the structure it was marked for. */
    private record PendingStart(String structureKey, CinematicShot.Pose pose) {
    }

    /** Static command handlers only, never instantiated. */
    private CinematicCommands() {
    }

    /** The {@code cinematic} branch, without its permission check, which the caller adds. */
    static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("cinematic")
                .then(Commands.argument(ULTRA_BEAST_ARGUMENT, StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                UltraSpaceStructureManager.structureKeys(), builder))
                        .then(Commands.literal("start").executes(CinematicCommands::start))
                        .then(Commands.literal("end")
                                .then(Commands.argument(SECONDS_ARGUMENT, DoubleArgumentType.doubleArg(
                                                CinematicShot.MIN_DURATION_MS / MS_PER_SECOND,
                                                CinematicShot.MAX_DURATION_MS / MS_PER_SECOND))
                                        .executes(CinematicCommands::end)))
                        .then(Commands.literal("undo").executes(CinematicCommands::undo))
                        .then(Commands.literal("reset").executes(CinematicCommands::reset))
                        .then(Commands.literal("list").executes(CinematicCommands::list))
                        .then(Commands.literal("play").executes(CinematicCommands::play)));
    }

    /** {@code start}: remembers the operator's camera as the start of the next shot. */
    private static int start(CommandContext<CommandSourceStack> context) {
        String key = structureKey(context);
        ServerPlayer player = playerInUltraSpace(context, key);
        if (key == null || player == null) {
            return 0;
        }
        PENDING.put(player.getUUID(), new PendingStart(key, poseOf(player)));
        context.getSource().sendSuccess(() -> Component.translatable("command.ultrabeasts.cinematic.start", key), false);
        return 1;
    }

    /** {@code end <seconds>}: closes the pending shot at the operator's camera and saves it. */
    private static int end(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String key = structureKey(context);
        ServerPlayer player = playerInUltraSpace(context, key);
        if (key == null || player == null) {
            return 0;
        }
        PendingStart pending = PENDING.get(player.getUUID());
        if (pending == null || !pending.structureKey().equals(key)) {
            source.sendFailure(Component.translatable("command.ultrabeasts.cinematic.no_start", key));
            return 0;
        }

        List<CinematicShot> shots = recorded(source, key);
        if (shots.size() >= CinematicShotStore.MAX_SHOTS) {
            source.sendFailure(Component.translatable("command.ultrabeasts.cinematic.full", key,
                    CinematicShotStore.MAX_SHOTS));
            return 0;
        }
        double seconds = DoubleArgumentType.getDouble(context, SECONDS_ARGUMENT);
        shots.add(new CinematicShot(pending.pose(), poseOf(player), (int) Math.round(seconds * MS_PER_SECOND)));
        if (!save(source, key, shots)) {
            return 0;
        }
        PENDING.remove(player.getUUID());

        int number = shots.size();
        source.sendSuccess(() -> Component.translatable("command.ultrabeasts.cinematic.added", number, key,
                format(seconds)), false);
        if (number == 1) {
            // First shot: say where the file is, so it can be copied into the mod's defaults.
            String file = CinematicShotStore.overrideFile(key).toString();
            source.sendSuccess(() -> Component.translatable("command.ultrabeasts.cinematic.file", file), false);
        }
        return 1;
    }

    /** {@code undo}: removes the last recorded shot; the defaults return once none is left. */
    private static int undo(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String key = structureKey(context);
        if (key == null) {
            return 0;
        }
        List<CinematicShot> shots = recorded(source, key);
        if (shots.isEmpty()) {
            source.sendFailure(Component.translatable("command.ultrabeasts.cinematic.nothing_to_undo", key));
            return 0;
        }
        shots.remove(shots.size() - 1);
        try {
            if (shots.isEmpty()) {
                // An empty file would mean "no tour"; without one the defaults apply, which is
                // what undoing every recorded shot should come back to.
                CinematicShotStore.reset(key);
            } else {
                CinematicShotStore.save(key, shots);
            }
        } catch (IOException e) {
            source.sendFailure(Component.translatable("command.ultrabeasts.cinematic.error", e.getMessage()));
            return 0;
        }
        int left = shots.size();
        source.sendSuccess(() -> Component.translatable("command.ultrabeasts.cinematic.undo", key, left), false);
        return 1;
    }

    /** {@code reset}: deletes the recorded shots, so the mod's defaults apply again. */
    private static int reset(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String key = structureKey(context);
        if (key == null) {
            return 0;
        }
        try {
            boolean deleted = CinematicShotStore.reset(key);
            source.sendSuccess(() -> Component.translatable(deleted
                    ? "command.ultrabeasts.cinematic.reset"
                    : "command.ultrabeasts.cinematic.reset_none", key), false);
            return 1;
        } catch (IOException e) {
            source.sendFailure(Component.translatable("command.ultrabeasts.cinematic.error", e.getMessage()));
            return 0;
        }
    }

    /** {@code list}: the shots in use, recorded or default, one line each. */
    private static int list(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String key = structureKey(context);
        if (key == null) {
            return 0;
        }
        List<CinematicShot> shots = CinematicShotStore.load(source.getServer(), key);
        String header = CinematicShotStore.hasRecorded(key)
                ? "command.ultrabeasts.cinematic.list.recorded"
                : "command.ultrabeasts.cinematic.list.defaults";
        int count = shots.size();
        source.sendSuccess(() -> Component.translatable(header, key, count), false);
        for (int i = 0; i < shots.size(); i++) {
            CinematicShot shot = shots.get(i);
            int number = i + 1;
            source.sendSuccess(() -> Component.translatable("command.ultrabeasts.cinematic.list.entry", number,
                    format(shot.durationMs() / MS_PER_SECOND), format(shot.from().position()),
                    format(shot.to().position())), false);
        }
        return 1;
    }

    /**
     * {@code play}: sends the cinematic to the operator, as on arrival. Films whatever stands
     * in Ultra-Space with the given structure's shots: the operator vouches that it matches.
     * The close-up is aimed from where the operator stands, as it is from the arrival point.
     */
    private static int play(CommandContext<CommandSourceStack> context) {
        String key = structureKey(context);
        ServerPlayer player = playerInUltraSpace(context, key);
        if (key == null || player == null) {
            return 0;
        }
        if (!WormholeEntity.sendEncounterCinematic(player, player.serverLevel(), key)) {
            context.getSource().sendFailure(Component.translatable("command.ultrabeasts.cinematic.play.failed", key));
            return 0;
        }
        return 1;
    }

    /** The structure argument, lower-cased; null, with an error sent, if no structure has that name. */
    private static String structureKey(CommandContext<CommandSourceStack> context) {
        String key = StringArgumentType.getString(context, ULTRA_BEAST_ARGUMENT).toLowerCase(Locale.ROOT);
        if (UltraSpaceStructureManager.getConfig(key) == null) {
            context.getSource().sendFailure(Component.translatable("command.ultrabeasts.wormhole.target.unknown", key));
            return null;
        }
        return key;
    }

    /**
     * The executing player, if they are in Ultra-Space; otherwise null, with an error sent.
     * Skipped (null, no message) when the structure argument was already rejected.
     */
    private static ServerPlayer playerInUltraSpace(CommandContext<CommandSourceStack> context, String key) {
        if (key == null) {
            return null;
        }
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("command.ultrabeasts.cinematic.player_only"));
            return null;
        }
        ServerLevel level = player.serverLevel();
        if (!level.dimension().equals(ModDimensions.ULTRA_SPACE_DIMENSION)) {
            source.sendFailure(Component.translatable("command.ultrabeasts.cinematic.not_in_ultra_space"));
            return null;
        }
        return player;
    }

    /** The recorded shots of a structure, as a list to edit; empty if none were recorded. */
    private static List<CinematicShot> recorded(CommandSourceStack source, String key) {
        if (!CinematicShotStore.hasRecorded(key)) {
            return new ArrayList<>();
        }
        return new ArrayList<>(CinematicShotStore.load(source.getServer(), key));
    }

    /** Saves the shots, reporting a failure to the operator. */
    private static boolean save(CommandSourceStack source, String key, List<CinematicShot> shots) {
        try {
            CinematicShotStore.save(key, shots);
            return true;
        } catch (IOException e) {
            source.sendFailure(Component.translatable("command.ultrabeasts.cinematic.error", e.getMessage()));
            return false;
        }
    }

    /** The player's camera: eye position and look, which is what the shot will show. */
    private static CinematicShot.Pose poseOf(ServerPlayer player) {
        return new CinematicShot.Pose(player.getEyePosition(), player.getYRot(), player.getXRot());
    }

    /** A duration for the chat, one decimal, the same in every language. */
    private static String format(double seconds) {
        return String.format(Locale.ROOT, "%.1f", seconds);
    }

    /** A position for the chat, one decimal per axis. */
    private static String format(Vec3 position) {
        return String.format(Locale.ROOT, "%.1f %.1f %.1f", position.x, position.y, position.z);
    }
}
