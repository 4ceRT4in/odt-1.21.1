package xyz.frxzenluke.titanreborn.command;


import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.server.MinecraftServer;
import xyz.frxzenluke.titanreborn.OneDayTitan;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ResetCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    LiteralArgumentBuilder.<ServerCommandSource>literal("reset")
                            .requires(source -> source.hasPermissionLevel(4))
                            .executes(ResetCommand::execute)
                            .then(
                                    LiteralArgumentBuilder.<ServerCommandSource>literal("confirm")
                                            .executes(ResetCommand::confirmReset)
                            )
            );
        });
    }

    private static int execute(CommandContext<ServerCommandSource> context) {
        context.getSource().sendFeedback(() -> Text.of(OneDayTitan.getPrefix() + "§cYou need to confirm the reset by typing §e/reset confirm"), false);
        return 1;
    }

    private static int confirmReset(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        MinecraftServer server = source.getServer();

        deleteFile("config", "players.json");
        deleteFile("config", "teams.json");
        deleteFile("config", "spawn.json");
        deleteFile("config", "config.json");
        deleteWorldFolder();
        source.getServer().getPlayerManager().getPlayerList().forEach(player -> {
            player.networkHandler.disconnect(Text.of(OneDayTitan.getPrefix() + "\n§cServer Reset!"));
        });
        server.shutdown();
        return 1;
    }

    private static void deleteFile(String directory, String fileName) {
        Path configPath = Paths.get(directory, fileName);
        if (Files.exists(configPath)) {
            try {
                Files.delete(configPath);
            } catch (IOException e) {
                System.err.println("Failed to delete config file: " + fileName);
            }
        }
    }

    private static void deleteWorldFolder() {
        Path worldPath = Paths.get("world");
        if (Files.exists(worldPath)) {
            try {
                Files.walk(worldPath)
                        .sorted((path1, path2) -> path2.compareTo(path1))
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                System.err.println("Failed to delete file: " + path);
                            }
                        });
            } catch (IOException e) {
                System.err.println("Failed to delete world folder.");
            }
        }
    }
}