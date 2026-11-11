package xyz.frxzenluke.titanreborn.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.util.Identifier;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import net.minecraft.server.MinecraftServer;
import xyz.frxzenluke.titanreborn.OneDayTitan;
import xyz.frxzenluke.titanreborn.handler.ConfigHandler;
import xyz.frxzenluke.titanreborn.handler.GameState;
import xyz.frxzenluke.titanreborn.manager.TeamManager;
import xyz.frxzenluke.titanreborn.manager.InviteItem;

public class TeamSizeCommand {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    LiteralArgumentBuilder.<ServerCommandSource>literal("teamsize").requires(source -> source.hasPermissionLevel(2))
                            .then(
                                    LiteralArgumentBuilder.<ServerCommandSource>literal("solos")
                                            .executes(TeamSizeCommand::solos)
                            ).then(
                                    LiteralArgumentBuilder.<ServerCommandSource>literal("duos")
                                            .executes(TeamSizeCommand::duos)
                            )
            );
        });
    }
    private static int solos(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        if (ConfigHandler.getState(OneDayTitan.getConfigPath()) == GameState.STARTED) {
            source.sendFeedback(() -> Text.of(OneDayTitan.getPrefix() + "§cThis game already started!"), false);
            return 0;
        }
        if(OneDayTitan.soloTeams) {
            source.sendFeedback(() -> Text.of(OneDayTitan.getPrefix() + "§cThe team size is already solos!"), false);
            return 0;
        }
        MinecraftServer server = context.getSource().getServer();
        OneDayTitan.soloTeams = true;
        TeamManager.getInstance().removeAllTeams();
        server.getPlayerManager().getPlayerList().forEach(playerAll -> {
            playerAll.playSoundToPlayer(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                    SoundCategory.PLAYERS,
                    1.0F,
                    1.0F
            );
            playerAll.getInventory().clear();
            playerAll.sendMessage(Text.of(OneDayTitan.getPrefix() + "§bThe team size was changed to §3Solo§b!"), false);
        });
        return 1;
    }
    private static int duos(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        if (ConfigHandler.getState(OneDayTitan.getConfigPath()) == GameState.STARTED) {
            source.sendFeedback(() -> Text.of(OneDayTitan.getPrefix() + "§cThis game already started!"), false);
            return 0;
        }
        if(!OneDayTitan.soloTeams) {
            source.sendFeedback(() -> Text.of(OneDayTitan.getPrefix() + "§cThe team size is already duos!"), false);
            return 0;
        }
        OneDayTitan.soloTeams = false;
        MinecraftServer server = context.getSource().getServer();
        server.getPlayerManager().getPlayerList().forEach(playerAll -> {
            playerAll.playSoundToPlayer(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                    SoundCategory.PLAYERS,
                    1.0F,
                    1.0F
            );
            playerAll.getInventory().setStack(0, InviteItem.create());
            playerAll.sendMessage(Text.of(OneDayTitan.getPrefix() + "§bThe team size was changed to §3Duos§b!"), false);
        });
        return 1;
    }
}
