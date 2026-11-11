package xyz.frxzenluke.titanreborn.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import xyz.frxzenluke.titanreborn.OneDayTitan;
import xyz.frxzenluke.titanreborn.handler.ConfigHandler;
import xyz.frxzenluke.titanreborn.handler.GameState;
import xyz.frxzenluke.titanreborn.manager.TeamManager;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;
import static com.mojang.brigadier.builder.RequiredArgumentBuilder.argument;

public class TeamCommand {


    private static final SuggestionProvider<ServerCommandSource> SUGGEST_PLAYER_NAMES = (context, builder) -> {
        return CommandSource.suggestMatching(context.getSource().getServer().getPlayerManager().getPlayerList().stream()
                .map(player -> player.getGameProfile().getName()), builder);
    };

    private static final ConcurrentHashMap<UUID, List<UUID>> invites = new ConcurrentHashMap<>();

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("tteam")
                    .then(CommandManager.literal("invite")
                            .then(CommandManager.argument("player", StringArgumentType.string()).suggests(SUGGEST_PLAYER_NAMES)
                                    .executes(context -> {
                                        ServerPlayerEntity inviter = context.getSource().getPlayer();
                                        if (!ConfigHandler.getState(OneDayTitan.getConfigPath()).equals(GameState.LOBBY)) {
                                            inviter.sendMessage(Text.of(OneDayTitan.getPrefix() + "§cThis game already started!"), false);
                                            return 0;
                                        }
                                        if(OneDayTitan.soloTeams) {
                                            inviter.sendMessage(Text.of(OneDayTitan.getPrefix() + "§cTeams are disabled for this OneDayTitan!"), false);
                                            return 0;
                                        }
                                        String targetName = StringArgumentType.getString(context, "player");
                                        ServerPlayerEntity invitee = context.getSource().getServer().getPlayerManager().getPlayer(targetName);

                                        if (invitee != null && invitee.getUuid().equals(inviter.getUuid())) {
                                            inviter.sendMessage(Text.of(OneDayTitan.getPrefix() + "§cYou cannot invite yourself!"), false);
                                            return 0;
                                        }

                                        if (invitee == null) {
                                            inviter.sendMessage(Text.of(OneDayTitan.getPrefix() + "§cThis player is not online"), false);
                                            return 0;
                                        }

                                        TeamManager teamManager = TeamManager.getInstance();
                                        if (teamManager.isPlayerInAnyTeam(inviter.getUuid())) {
                                            inviter.sendMessage(Text.of(OneDayTitan.getPrefix() + "§cYou are already in a team! Use /tteam leave to leave."), false);
                                            return 0;
                                        }
                                        if (teamManager.isPlayerInAnyTeam(invitee.getUuid())) {
                                            inviter.sendMessage(Text.of(OneDayTitan.getPrefix() + "§c" + targetName + " is already in a team!"), false);
                                            return 0;
                                        }

                                        List<UUID> currentInviters = invites.computeIfAbsent(invitee.getUuid(), k -> new ArrayList<>());

                                        if (currentInviters.contains(inviter.getUuid())) {
                                            inviter.sendMessage(Text.of(OneDayTitan.getPrefix() + "§cYou have already invited " + targetName), false);
                                            return 0;
                                        }

                                        invitee.playSoundToPlayer(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0F, 1.0F);
                                        inviter.playSoundToPlayer(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0F, 1.0F);
                                        invites.computeIfAbsent(invitee.getUuid(), k -> new ArrayList<>()).add(inviter.getUuid());
                                        inviter.sendMessage(Text.of(OneDayTitan.getPrefix() + "§7You've invited §5" + targetName + " §7to team with you!"), false);
                                        invitee.sendMessage(Text.of(OneDayTitan.getPrefix() + "§7You've been invited by §5" + inviter.getName().getString()
                                                + "§7. Use §a/tteam accept " + inviter.getName().getString() + " §7or hit him back within the next §530 §7seconds"), false);

                                        CompletableFuture.delayedExecutor(30, TimeUnit.SECONDS).execute(() -> {
                                            invites.computeIfPresent(invitee.getUuid(), (key, value) -> {
                                                value.remove(inviter.getUuid());
                                                if (value.isEmpty()) {
                                                    inviter.sendMessage(Text.of(OneDayTitan.getPrefix() + "§cYour invite to " + targetName + " has expired"), false);
                                                    invitee.sendMessage(Text.of(OneDayTitan.getPrefix() + "§cThe invite from " + inviter.getName().getString() + " has expired"), false);
                                                    return null;
                                                }
                                                return value;
                                            });
                                        });

                                        return 1;
                                    })
                            )
                    )
                    .then(CommandManager.literal("accept")
                            .then(CommandManager.argument("inviter", StringArgumentType.string()).suggests(SUGGEST_PLAYER_NAMES)
                                    .executes(context -> {
                                        ServerPlayerEntity invitee = context.getSource().getPlayer();
                                        if (!ConfigHandler.getState(OneDayTitan.getConfigPath()).equals(GameState.LOBBY)) {
                                            invitee.sendMessage(Text.of(OneDayTitan.getPrefix() + "§cThis game already started!"), false);
                                            return 0;
                                        }
                                        if(OneDayTitan.soloTeams) {
                                            invitee.sendMessage(Text.of(OneDayTitan.getPrefix() + "§cTeams are disabled for this OneDayTitan!"), false);
                                            return 0;
                                        }
                                        String inviterName = StringArgumentType.getString(context, "inviter");
                                        ServerPlayerEntity inviter = context.getSource().getServer().getPlayerManager().getPlayer(inviterName);

                                        if (inviter == null || !invites.containsKey(invitee.getUuid()) || !invites.get(invitee.getUuid()).contains(inviter.getUuid())) {
                                            invitee.sendMessage(Text.of(OneDayTitan.getPrefix() + "§cYou haven't got an invite from " + inviterName), false);
                                            return 0;
                                        }

                                        TeamManager teamManager = TeamManager.getInstance();
                                        teamManager.createTeam(inviter.getName().getString() + "'s Team", inviter.getUuid(), invitee.getUuid());
                                        invites.get(invitee.getUuid()).remove(inviter.getUuid());
                                        if (invites.get(invitee.getUuid()).isEmpty()) {
                                            invites.remove(invitee.getUuid());
                                        }
                                        invitee.playSoundToPlayer(SoundEvents.ENTITY_HORSE_SADDLE, SoundCategory.PLAYERS, 1.0F, 1.0F);
                                        inviter.playSoundToPlayer(SoundEvents.ENTITY_HORSE_SADDLE, SoundCategory.PLAYERS, 1.0F, 1.0F);
                                        inviter.sendMessage(Text.of(OneDayTitan.getPrefix() + "§a" + invitee.getName().getString() + " has joined your team!"), false);
                                        invitee.sendMessage(Text.of(OneDayTitan.getPrefix() + "§7You have joined §5" + inviter.getName().getString() + "§7's §7team!"), false);
                                        return 1;
                                    })
                            )
                    )
                    .then(CommandManager.literal("leave")
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                TeamManager teamManager = TeamManager.getInstance();
                                if (!ConfigHandler.getState(OneDayTitan.getConfigPath()).equals(GameState.LOBBY)) {
                                    player.sendMessage(Text.of(OneDayTitan.getPrefix() + "§cThis game already started, therefore you cannot leave your current team!"), false);
                                    return 0;
                                }
                                if (teamManager.isPlayerInAnyTeam(player.getUuid())) {
                                    teamManager.removeTeam(teamManager.getTeamByPlayer(player.getUuid()));
                                    player.playSoundToPlayer(SoundEvents.ENTITY_HORSE_SADDLE, SoundCategory.PLAYERS, 1.0F, 1.0F);
                                    player.sendMessage(Text.of(OneDayTitan.getPrefix() + "§cYou have left your team"), false);
                                } else {
                                    player.sendMessage(Text.of(OneDayTitan.getPrefix() + "§cYou're not in a team!"), false);
                                }

                                return 1;
                            })
                    )
            );
        });
    }
    public static UUID getInviterForPlayer(UUID inviteeUuid) {
        return invites.getOrDefault(inviteeUuid, Collections.emptyList())
                .stream()
                .findFirst()
                .orElse(null);
    }
}