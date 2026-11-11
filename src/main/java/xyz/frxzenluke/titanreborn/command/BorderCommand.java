package xyz.frxzenluke.titanreborn.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import xyz.frxzenluke.titanreborn.OneDayTitan;
import xyz.frxzenluke.titanreborn.manager.StartStuffManager;

import java.nio.file.Path;

public class BorderCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    LiteralArgumentBuilder.<ServerCommandSource>literal("border")
                            .executes(BorderCommand::execute)
            );
        });
    }
    private static int execute(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        source.sendFeedback(() -> Text.of(OneDayTitan.getPrefix() + "§7The current radius of the WorldBorder §7is §5" + source.getWorld().getWorldBorder().getSize() + " §7blocks"), false);
        return 1;
    }
}
