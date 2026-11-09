package dev.darcosse.ultrabeasts.registry;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.Species;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.darcosse.ultrabeasts.config.ConfigManager;
import dev.darcosse.ultrabeasts.dimension.UltraSpaceStructureManager;
import dev.darcosse.ultrabeasts.entity.WormholeEntity;
import dev.darcosse.ultrabeasts.handler.UltraSpaceSession;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;

/**
 * The {@code /ultrabeasts} command tree: config reload and info, and operator tools to spawn
 * wormholes, to a random or a chosen Ultra Beast, or clear them. Server side; every message goes through a translation key resolved by
 * the receiving client.
 */
public final class ModCommands {

    /**
     * Permission level of the operator sub-commands. 2 is the vanilla level of /tp and
     * /gamemode; lowering it lets regular players spawn wormholes on demand.
     */
    private static final int OPERATOR_LEVEL = 2;

    /** Name of the Ultra Beast argument, as shown in the command's usage. */
    private static final String ULTRA_BEAST_ARGUMENT = "ultra_beast";

    /** Static command handlers only, never instantiated. */
    private ModCommands() {
    }

    /** Registers the command tree. Called by each loader from its own command registration event. */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ultrabeasts")
                .then(Commands.literal("reload")
                        .requires(source -> source.hasPermission(OPERATOR_LEVEL))
                        .executes(ModCommands::reloadConfig))
                .then(Commands.literal("info")
                        .executes(ModCommands::showInfo))
                .then(Commands.literal("wormhole")
                        .requires(source -> source.hasPermission(OPERATOR_LEVEL))
                        .then(Commands.literal("spawn")
                                .executes(ModCommands::summonWormhole))
                        .then(Commands.literal("clear")
                                .executes(ModCommands::clearWormhole))
                        // Next to the literals: brigadier tries "spawn" and "clear" first, and
                        // no Ultra Beast bears either name.
                        .then(Commands.argument(ULTRA_BEAST_ARGUMENT, StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        UltraSpaceStructureManager.structureKeys(), builder))
                                .executes(ModCommands::summonWormholeTo)))
        );
    }

    /**
     * Name of whoever ran the command, as a component so the RECEIVING client translates it.
     * <p>
     * Calling {@code Component.translatable(...).getString()} on the server, as 4.0 did,
     * resolves the key with the server's language table. A dedicated server only loads
     * vanilla's English strings, not the mod's, so players saw the raw key
     * "command.ultrabeasts.generic.server_name". The clear command even used a hard-coded
     * "Server".
     */
    private static Component executorName(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return player.getDisplayName();
        }
        return Component.translatable("command.ultrabeasts.generic.server_name");
    }

    /**
     * The Ultra Beast's name, translated by the receiving client; the raw key if Cobblemon
     * does not know the species.
     */
    private static Component ultraBeastName(String structureKey) {
        Species species = PokemonSpecies.getByName(structureKey);
        return species != null ? Component.translatable(species.getTranslationKey()) : Component.literal(structureKey);
    }

    /** {@code /ultrabeasts reload}: re-reads the config file and reports the new spawn rate. */
    private static int reloadConfig(CommandContext<CommandSourceStack> context) {
        // No try/catch any more: loadConfig() logs and falls back to defaults instead of
        // throwing, so the old error branch could never run.
        ConfigManager.reloadConfig();

        context.getSource().sendSuccess(
                () -> Component.translatable("command.ultrabeasts.reload.success"),
                true
        );

        context.getSource().sendSuccess(
                () -> Component.translatable("command.ultrabeasts.reload.rate",
                        ConfigManager.getCurrentWormholeSpawnChance()),
                false
        );

        return 1;
    }

    /** {@code /ultrabeasts info}: shows the current spawn rate as "1/N" and as a percentage. */
    private static int showInfo(CommandContext<CommandSourceStack> context) {
        int chance = ConfigManager.getCurrentWormholeSpawnChance();
        double percentage = (1.0 / chance) * 100.0;

        context.getSource().sendSuccess(
                () -> Component.translatable("command.ultrabeasts.info.header"),
                false
        );
        context.getSource().sendSuccess(
                () -> Component.translatable("command.ultrabeasts.info.rate", chance,
                        String.format("%.3f", percentage)),
                false
        );

        return 1;
    }

    /** {@code /ultrabeasts wormhole spawn}: opens a wormhole to a random Ultra Beast on the executing player. */
    private static int summonWormhole(CommandContext<CommandSourceStack> context) {
        return summon(context.getSource(), null);
    }

    /**
     * {@code /ultrabeasts wormhole <ultra_beast>}: opens a wormhole on the executing player that
     * leads to the given Ultra Beast's structure. Meant for testing one structure on demand.
     */
    private static int summonWormholeTo(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String structureKey = StringArgumentType.getString(context, ULTRA_BEAST_ARGUMENT).toLowerCase(Locale.ROOT);
        if (UltraSpaceStructureManager.getConfig(structureKey) == null) {
            source.sendFailure(Component.translatable("command.ultrabeasts.wormhole.target.unknown", structureKey));
            return 0;
        }
        return summon(source, structureKey);
    }

    /**
     * Opens a wormhole on the executing player, leading to {@code structureKey}, or to a random
     * structure if it is null, and reports the outcome.
     */
    private static int summon(CommandSourceStack source, String structureKey) {
        ServerPlayer player = source.getPlayer();

        if (player == null) {
            source.sendFailure(Component.translatable("command.ultrabeasts.wormhole.spawn.error"));
            return 0;
        }

        try {
            ServerLevel level = source.getLevel();

            if (!WormholeEntity.forceSpawnToPlayer(level, player, structureKey)) {
                source.sendFailure(Component.translatable("command.ultrabeasts.wormhole.spawn.occupied"));
                return 0;
            }

            Component executor = executorName(source);
            if (structureKey == null) {
                source.sendSuccess(
                        () -> Component.translatable("command.ultrabeasts.wormhole.spawn.success", executor),
                        true
                );
            } else {
                Component beast = ultraBeastName(structureKey);
                source.sendSuccess(
                        () -> Component.translatable("command.ultrabeasts.wormhole.target.success", executor, beast),
                        true
                );
            }
            return 1;
        } catch (Exception e) {
            // Kept: spawning touches entities and the world, and an operator command should
            // report a failure rather than print a stack trace to whoever ran it.
            source.sendFailure(
                    Component.translatable("command.ultrabeasts.wormhole.spawn.failed", e.getMessage())
            );
            return 0;
        }
    }

    /**
     * {@code /ultrabeasts wormhole clear}: removes every mod entity, sends anyone in
     * Ultra-Space home and frees the dimension.
     */
    private static int clearWormhole(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        try {
            MinecraftServer server = source.getServer();

            ModEvents.cleanUp(server);

            int evicted = UltraSpaceSession.evictEveryoneAndRelease(server);

            Component executor = executorName(source);
            source.sendSuccess(
                    () -> Component.translatable("command.ultrabeasts.wormhole.clear.success", executor),
                    true
            );

            if (evicted > 0) {
                source.sendSuccess(
                        () -> Component.translatable("command.ultrabeasts.wormhole.clear.evicted", evicted),
                        true
                );
            }

            return 1;
        } catch (Exception e) {
            // Kept for the same reason as in summon.
            source.sendFailure(
                    Component.translatable("command.ultrabeasts.wormhole.clear.error", e.getMessage())
            );
            return 0;
        }
    }
}
