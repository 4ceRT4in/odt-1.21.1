package xyz.frxzenluke.titanreborn.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandlerType;
import xyz.frxzenluke.titanreborn.OneDayTitan;

public class InvseeCommand {
    private static final SuggestionProvider<ServerCommandSource> SUGGEST_PLAYER_NAMES = (context, builder) -> {
        return CommandSource.suggestMatching(
                context.getSource().getServer().getPlayerManager().getPlayerList().stream()
                        .map(player -> player.getGameProfile().getName()),
                builder
        );
    };
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("invsee")
                    .requires(source -> source.hasPermissionLevel(2))
                    .then(CommandManager.argument("player", StringArgumentType.string())
                            .suggests(SUGGEST_PLAYER_NAMES)
                            .executes(InvseeCommand::execute)
                    )
            );
        });
    }
    private static int execute(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String targetPlayerName = StringArgumentType.getString(context, "player");
        ServerPlayerEntity targetPlayer = context.getSource().getServer()
                .getPlayerManager().getPlayer(targetPlayerName);

        if (targetPlayer == null) {
            context.getSource().sendFeedback(() -> Text.of(OneDayTitan.getPrefix() + "§cThis player is not online!"), false);
            return 0;
        }

        ServerPlayerEntity sourcePlayer = context.getSource().getPlayer();
        if (sourcePlayer != null) {
            sourcePlayer.openHandledScreen(new NamedScreenHandlerFactory() {
                @Override
                public Text getDisplayName() {
                    return Text.of(targetPlayerName);
                }
                @Override
                public ScreenHandler createMenu(int syncId,
                                                net.minecraft.entity.player.PlayerInventory playerInventory,
                                                net.minecraft.entity.player.PlayerEntity player) {
                    return new GenericContainerScreenHandler(
                            ScreenHandlerType.GENERIC_9X4,
                            syncId,
                            playerInventory,
                            targetPlayer.getInventory(),
                            4
                    );
                }
            });
        }

        return 1;
    }
}