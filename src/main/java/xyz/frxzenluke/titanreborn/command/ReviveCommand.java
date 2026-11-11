package xyz.frxzenluke.titanreborn.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.world.GameMode;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.command.CommandSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.text.Text;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.core.jmx.Server;
import xyz.frxzenluke.titanreborn.OneDayTitan;
import xyz.frxzenluke.titanreborn.event.PlayerJoinEvent;
import xyz.frxzenluke.titanreborn.manager.PlayerManager;
import xyz.frxzenluke.titanreborn.manager.StartStuffManager;
import xyz.frxzenluke.titanreborn.manager.TeamManager;
import xyz.frxzenluke.titanreborn.manager.states.PlayerState;
import xyz.frxzenluke.titanreborn.manager.states.TeamState;

import java.util.UUID;


public class ReviveCommand {
    private static final SuggestionProvider<ServerCommandSource> SUGGEST_PLAYER_NAMES = (context, builder) -> {
        return CommandSource.suggestMatching(
                context.getSource().getServer().getPlayerManager().getPlayerList().stream()
                        .map(player -> player.getGameProfile().getName()),
                builder
        );
    };

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("revive")
                    .requires(source -> source.hasPermissionLevel(2))
                    .then(CommandManager.argument("player", StringArgumentType.string())
                            .suggests(SUGGEST_PLAYER_NAMES)
                            .executes(ReviveCommand::execute)
                    )
            );
        });
    }

    private static int execute(CommandContext<ServerCommandSource> context) {
        String playerName = StringArgumentType.getString(context, "player");
        ServerCommandSource source = context.getSource();
        PlayerManager playerManager = PlayerManager.getInstance();
        UUID playerUUID = playerManager.getUUIDByName(playerName);
        if (playerUUID == null) {
            source.sendError(Text.of(OneDayTitan.getPrefix() + "§cThis player couldn't be found"));
            return 0;
        }

        if (playerManager.getPlayerState(playerUUID) == PlayerState.ALIVE) {
            source.sendError(Text.of(OneDayTitan.getPrefix() + "§cThis player is still alive"));
            return 0;
        }
        playerManager.setPlayerState(playerUUID, PlayerState.ALIVE);
        source.sendFeedback(() -> Text.of(OneDayTitan.getPrefix() + "§7The player §5" + playerName + " §7was revived"), false);
        ServerPlayerEntity targetPlayer = source.getServer().getPlayerManager().getPlayer(playerUUID);
        if(targetPlayer != null) {
            BlockPos spawnPos = PlayerJoinEvent.readSpawnCoordinates();
            if (spawnPos != null) {
                ServerWorld world = source.getServer().getOverworld();
                targetPlayer.teleport(world, spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), 0, 0);
            }
            targetPlayer.changeGameMode(GameMode.SURVIVAL);
            targetPlayer.sendMessage(Text.of(OneDayTitan.getPrefix() + "§bYou were §5revived§b!"), false);
            targetPlayer.playSoundToPlayer(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0F, 1.0F);
        }
        String teamKey = getTeamKeyByPlayer(playerUUID, TeamManager.getInstance());
        if (teamKey != null) {
            TeamState teamState = TeamManager.getInstance().getTeamState(teamKey);
            if (teamState == TeamState.DEAD) {
                TeamManager.getInstance().setTeamState(teamKey, TeamState.ALIVE);
            }
        }
        return 1;
    }

    private static String getTeamKeyByPlayer(UUID playerUUID, TeamManager teamManager) {
        for (String teamKey : teamManager.getAllTeams()) {
            if (teamManager.getPlayersInTeam(teamKey).contains(playerUUID.toString())) {
                return teamKey;
            }
        }
        return null;
    }
}