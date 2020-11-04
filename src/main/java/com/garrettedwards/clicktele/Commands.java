package com.garrettedwards.clicktele;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;

import java.time.Duration;

public class Commands {

    public static void registerSetCooldownCommand(CommandDispatcher<ServerCommandSource> dispatcher, Config config) {
        dispatcher.register(
                CommandManager.literal("ctcd")
                        .requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(4))
                        .then(CommandManager.argument("seconds", IntegerArgumentType.integer(0))
                                .executes(context -> {
                                    int seconds = context.getArgument("seconds", int.class);
                                    long previous = config.getCooldownSeconds().getSeconds();
                                    config.setCooldownSeconds(Duration.ofSeconds(seconds));
                                    context.getSource().sendFeedback(new LiteralText("Compass teleport cooldown set from " + previous + " to " + seconds + " seconds"), true);
                                    return Command.SINGLE_SUCCESS;
                                }))
        );
    }

    public static void registerSetDistanceCommand(CommandDispatcher<ServerCommandSource> dispatcher, Config config) {
        dispatcher.register(
                CommandManager.literal("ctd")
                        .requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(4))
                        .then(CommandManager.argument("distance", IntegerArgumentType.integer(5, 256))
                                .executes(context -> {
                                    int distance = context.getArgument("distance", int.class);
                                    int previous = config.getMaxDistance();
                                    config.setMaxDistance(distance);
                                    context.getSource().sendFeedback(new LiteralText("Compass teleport maximum distance set from " + previous + " to " + distance + " blocks"), true);
                                    return Command.SINGLE_SUCCESS;
                                }))
        );
    }

}
