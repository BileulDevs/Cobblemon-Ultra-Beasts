package dev.darcosse.ultrabeasts.fabric.registry;

import dev.darcosse.ultrabeasts.fabric.UltraBeasts;
import dev.darcosse.ultrabeasts.fabric.config.ConfigManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import dev.darcosse.ultrabeasts.fabric.entity.WormholeEntity;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

public class ModCommands {

    public static void initialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            ultrabeastsCommand(dispatcher);
        });

        UltraBeasts.LOGGER.info("Registering Commands for " + UltraBeasts.MOD_ID);
    }

    private static void ultrabeastsCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("ultrabeasts")
                .then(CommandManager.literal("reload")
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(ModCommands::reloadConfig))
                .then(CommandManager.literal("info")
                        .executes(ModCommands::showInfo))
                .then(CommandManager.literal("wormhole")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.literal("spawn")
                                .executes(ModCommands::summonWormhole))
                        .then(CommandManager.literal("clear")
                                .executes(ModCommands::clearWormhole)))
        );
    }

    private static int reloadConfig(CommandContext<ServerCommandSource> context) {
        try {
            ConfigManager.reloadConfig();

            context.getSource().sendFeedback(
                    () -> Text.translatable("command.ultrabeasts.reload.success"),
                    true
            );

            context.getSource().sendFeedback(
                    () -> Text.translatable("command.ultrabeasts.reload.rate", ConfigManager.getCurrentWormholeSpawnChance()),
                    false
            );

            return 1;
        } catch (Exception e) {
            context.getSource().sendError(
                    Text.translatable("command.ultrabeasts.reload.error", e.getMessage())
            );
            return 0;
        }
    }

    private static int showInfo(CommandContext<ServerCommandSource> context) {
        int chance = ConfigManager.getCurrentWormholeSpawnChance();
        double percentage = (1.0 / chance) * 100.0;

        context.getSource().sendFeedback(
                () -> Text.translatable("command.ultrabeasts.info.header"),
                false
        );
        context.getSource().sendFeedback(
                () -> Text.translatable("command.ultrabeasts.info.rate", chance,
                        String.format("%.3f", percentage)),
                false
        );

        return 1;
    }

    private static int summonWormhole(CommandContext<ServerCommandSource> context) {
        try {
            ServerWorld world = context.getSource().getWorld();
            ServerPlayerEntity player = context.getSource().getPlayer();
            String executorName;

            if (context.getSource().getEntity() instanceof ServerPlayerEntity commandExecutor) {
                executorName = commandExecutor.getName().getString();
            } else {
                executorName = Text.translatable("command.ultrabeasts.generic.server_name").getString();
            }

            if (player != null) {
                WormholeEntity.forceSpawnToPlayer(world, player);

                context.getSource().sendFeedback(
                        () -> Text.translatable("command.ultrabeasts.wormhole.spawn.success", executorName),
                        true
                );
                return 1;
            } else {
                context.getSource().sendError(Text.translatable("command.ultrabeasts.wormhole.spawn.error"));
                return 0;
            }
        } catch (Exception e) {
            context.getSource().sendError(
                    Text.translatable("command.ultrabeasts.wormhole.spawn.failed", e.getMessage())
            );
            return 0;
        }
    }

    private static int clearWormhole(CommandContext<ServerCommandSource> context) {
        try {
            MinecraftServer server = context.getSource().getServer();
            String executorName;

            if (context.getSource().getEntity() instanceof ServerPlayerEntity player) {
                executorName = player.getName().getString();
            } else {
                executorName = "Server";
            }

            ModEvents.cleanUp(server);

            context.getSource().sendFeedback(
                    () -> Text.translatable("command.ultrabeasts.wormhole.clear.success", executorName),
                    true
            );

            return 1;
        } catch (Exception e) {
            context.getSource().sendError(
                    Text.translatable("command.ultrabeasts.wormhole.clear.error", e.getMessage())
            );
            return 0;
        }
    }
}