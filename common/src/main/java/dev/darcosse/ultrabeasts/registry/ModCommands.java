package dev.darcosse.ultrabeasts.registry;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import dev.darcosse.ultrabeasts.config.ConfigManager;
import dev.darcosse.ultrabeasts.entity.WormholeEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class ModCommands {

    /** Called by each loader from its own command registration event. */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ultrabeasts")
                .then(Commands.literal("reload")
                        .requires(source -> source.hasPermission(2))
                        .executes(ModCommands::reloadConfig))
                .then(Commands.literal("info")
                        .executes(ModCommands::showInfo))
                .then(Commands.literal("wormhole")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("spawn")
                                .executes(ModCommands::summonWormhole))
                        .then(Commands.literal("clear")
                                .executes(ModCommands::clearWormhole)))
        );
    }

    private static int reloadConfig(CommandContext<CommandSourceStack> context) {
        try {
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
        } catch (Exception e) {
            context.getSource().sendFailure(
                    Component.translatable("command.ultrabeasts.reload.error", e.getMessage())
            );
            return 0;
        }
    }

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

    private static int summonWormhole(CommandContext<CommandSourceStack> context) {
        try {
            ServerLevel level = context.getSource().getLevel();
            ServerPlayer player = context.getSource().getPlayer();
            String executorName;

            if (context.getSource().getEntity() instanceof ServerPlayer commandExecutor) {
                executorName = commandExecutor.getName().getString();
            } else {
                executorName = Component.translatable("command.ultrabeasts.generic.server_name").getString();
            }

            if (player != null) {
                WormholeEntity.forceSpawnToPlayer(level, player);

                context.getSource().sendSuccess(
                        () -> Component.translatable("command.ultrabeasts.wormhole.spawn.success", executorName),
                        true
                );
                return 1;
            } else {
                context.getSource().sendFailure(
                        Component.translatable("command.ultrabeasts.wormhole.spawn.error"));
                return 0;
            }
        } catch (Exception e) {
            context.getSource().sendFailure(
                    Component.translatable("command.ultrabeasts.wormhole.spawn.failed", e.getMessage())
            );
            return 0;
        }
    }

    private static int clearWormhole(CommandContext<CommandSourceStack> context) {
        try {
            MinecraftServer server = context.getSource().getServer();
            String executorName;

            if (context.getSource().getEntity() instanceof ServerPlayer player) {
                executorName = player.getName().getString();
            } else {
                executorName = "Server";
            }

            ModEvents.cleanUp(server);

            context.getSource().sendSuccess(
                    () -> Component.translatable("command.ultrabeasts.wormhole.clear.success", executorName),
                    true
            );

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(
                    Component.translatable("command.ultrabeasts.wormhole.clear.error", e.getMessage())
            );
            return 0;
        }
    }
}
