package xyz.frxzenluke.titanreborn.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.text.Text;
import xyz.frxzenluke.titanreborn.OneDayTitan;

import static xyz.frxzenluke.titanreborn.event.PlayerJoinEvent.readSpawnCoordinates;

public class SpawnCommand {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    LiteralArgumentBuilder.<ServerCommandSource>literal("spawn")
                            .executes(SpawnCommand::execute)
            );
        });
    }
    private static int execute(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        BlockPos spawnPos = readSpawnCoordinates();
        if (spawnPos != null) {
            source.sendFeedback(() -> Text.of(OneDayTitan.getPrefix() + "§7The Spawn is located at §5" + spawnPos.getX() + "§7, §5" + spawnPos.getY() + "§7, §5" + spawnPos.getZ()), false);
        } else {
            source.sendFeedback(() -> Text.of(OneDayTitan.getPrefix() + "§cThe Spawn Location hasn't been set!"), false);
        }
        return 1;
    }
}
