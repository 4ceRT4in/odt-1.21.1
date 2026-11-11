package xyz.frxzenluke.titanreborn.command;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;
import xyz.frxzenluke.titanreborn.manager.PlayerManager;
import xyz.frxzenluke.titanreborn.manager.TeamManager;

import java.nio.file.Path;

public class ReloadCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("reloadconfig")
                    .requires(source -> source.hasPermissionLevel(2))
                    .executes(context -> reload(context.getSource()))
            );
        });
    }
    private static int reload(ServerCommandSource source) {
        source.sendFeedback(() -> Text.of("§eReloading Files..."), false);
        TeamManager.getInstance().loadTeams();
        PlayerManager.getInstance().loadAlivePlayers();
        source.sendFeedback(() -> Text.of("§aReload done."), false);
        return 1;
    }
}
