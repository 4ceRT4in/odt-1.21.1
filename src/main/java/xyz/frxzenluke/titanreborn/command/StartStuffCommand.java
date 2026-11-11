package xyz.frxzenluke.titanreborn.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import xyz.frxzenluke.titanreborn.manager.StartStuffManager;

public class StartStuffCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    LiteralArgumentBuilder.<ServerCommandSource>literal("setstartstuff").requires(source -> source.hasPermissionLevel(2))
                            .executes(StartStuffCommand::execute)
            );
        });
    }

    private static int execute(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        if (source.getEntity() instanceof ServerPlayerEntity player) {
            StartStuffManager.saveStartStuff(player);
        }
        return 1;
    }
}