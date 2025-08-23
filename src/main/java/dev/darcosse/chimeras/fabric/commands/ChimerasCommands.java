package dev.darcosse.chimeras.fabric.commands;

import dev.darcosse.chimeras.fabric.config.ConfigManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class ChimerasCommands {

    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            registerShinyCommands(dispatcher);
        });
    }

    private static void registerShinyCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("ultrabeast")
                .then(CommandManager.literal("reload")
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(ChimerasCommands::reloadConfig))
                .then(CommandManager.literal("info")
                        .executes(ChimerasCommands::showInfo))
        );
    }

    private static int reloadConfig(CommandContext<ServerCommandSource> context) {
        try {
            ConfigManager.reloadConfig();

            context.getSource().sendFeedback(
                    () -> Text.translatable("command.chimeras.reload.success"),
                    true
            );

            context.getSource().sendFeedback(
                    () -> Text.translatable("command.chimeras.reload.rate", ConfigManager.getCurrentWormholeSpawnChance()),
                    false
            );

            return 1;
        } catch (Exception e) {
            context.getSource().sendError(
                    Text.translatable("command.chimeras.reload.error", e.getMessage())
            );
            return 0;
        }
    }

    private static int showInfo(CommandContext<ServerCommandSource> context) {
        int chance = ConfigManager.getCurrentWormholeSpawnChance();
        double percentage = (1.0 / chance) * 100.0;

        context.getSource().sendFeedback(
                () -> Text.translatable("command.chimeras.info.header"),
                false
        );
        context.getSource().sendFeedback(
                () -> Text.translatable("command.chimeras.info.rate", chance,
                        String.format("%.3f", percentage)),
                false
        );

        return 1;
    }
}