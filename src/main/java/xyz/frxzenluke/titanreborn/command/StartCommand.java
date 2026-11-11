package xyz.frxzenluke.titanreborn.command;

import com.google.gson.Gson;
import com.mojang.brigadier.Command;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameRules;
import net.minecraft.world.GameMode;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import net.minecraft.server.MinecraftServer;
import xyz.frxzenluke.titanreborn.OneDayTitan;
import xyz.frxzenluke.titanreborn.handler.ConfigHandler;
import xyz.frxzenluke.titanreborn.handler.GameState;
import xyz.frxzenluke.titanreborn.manager.PlayerManager;
import xyz.frxzenluke.titanreborn.manager.StartStuffManager;
import xyz.frxzenluke.titanreborn.manager.TeamManager;
import xyz.frxzenluke.titanreborn.manager.ServerScheduler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class StartCommand {
    private static final ServerScheduler scheduler = new ServerScheduler();
    public static final ServerScheduler protectionTimeScheduler = new ServerScheduler();

    private static boolean startedCountdown = false;
    public static boolean protectionTime = false;


    public static void register(Path configPath) {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("start")
                    .requires(source -> source.hasPermissionLevel(2))
                    .executes(context -> startCountdown(context.getSource(), configPath))
            );
        });
    }

    private static void startProtectionTime(ServerCommandSource source) {
        protectionTime = true;
        source.getServer().getPlayerManager().getPlayerList().forEach(player -> {
            player.playSoundToPlayer(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(),
                    SoundCategory.PLAYERS,
                    1.0F,
                    1.0F
            );
            player.sendMessage(Text.of(OneDayTitan.getPrefix() + "§cThe grace period lasts §b20 §cminutes!"), false);
        });

        protectionTimeScheduler.scheduleAtFixedRate(source.getServer(), new Runnable() {
            int countdown = 1200;
            @Override
            public void run() {
                if (countdown <= 0) {
                    protectionTime = false;
                    source.getServer().getPlayerManager().getPlayerList().forEach(player -> {
                        player.playSoundToPlayer(SoundEvents.ENTITY_ENDER_DRAGON_GROWL,
                                SoundCategory.PLAYERS,
                                1.0F,
                                1.0F
                        );
                        player.sendMessage(Text.of("§8✕ §7Grace Period ends in: §5NOW"), true);
                        player.sendMessage(Text.of(OneDayTitan.getPrefix() + "§cThe grace period is now over!"), false);
                    });
                    protectionTimeScheduler.shutdown();
                } else {
                    if(countdown == 900 || countdown == 600 || countdown == 300 || countdown == 240 || countdown == 180|| countdown == 120) {
                        source.getServer().getPlayerManager().getPlayerList().forEach(player -> {
                            player.playSoundToPlayer(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(),
                                    SoundCategory.PLAYERS,
                                    1.0F,
                                    1.0F
                            );
                            player.sendMessage(Text.of(OneDayTitan.getPrefix() + "§cThe grace period ends in §b" + (countdown / 60) + " §cminutes!"), false);
                        });
                    } else if (countdown == 60) {
                        source.getServer().getPlayerManager().getPlayerList().forEach(player -> {
                            player.playSoundToPlayer(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(),
                                    SoundCategory.PLAYERS,
                                    1.0F,
                                    1.0F
                            );
                            player.sendMessage(Text.of(OneDayTitan.getPrefix() + "§cThe grace period ends in §b" + (countdown / 60) + " §cminute!"), false);
                        });
                    } else if (countdown == 30 || countdown == 15 || countdown == 10 || countdown == 5 || countdown == 4 || countdown == 3 || countdown == 2) {
                        source.getServer().getPlayerManager().getPlayerList().forEach(player -> {
                            player.playSoundToPlayer(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(),
                                    SoundCategory.PLAYERS,
                                    1.0F,
                                    1.0F
                            );
                            player.sendMessage(Text.of(OneDayTitan.getPrefix() + "§cThe grace period ends in §b" + countdown + " §cseconds!"), false);
                        });
                    } else if (countdown == 1) {
                        source.getServer().getPlayerManager().getPlayerList().forEach(player -> {
                            player.playSoundToPlayer(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(),
                                    SoundCategory.PLAYERS,
                                    1.0F,
                                    1.0F
                            );
                            player.sendMessage(Text.of(OneDayTitan.getPrefix() + "§cThe grace period ends in §b" + countdown + " §csecond!"), false);
                        });
                    }
                    source.getServer().getPlayerManager().getPlayerList().forEach(player -> {
                        if(countdown > 60) {
                            player.sendMessage(Text.of("§8✕ §7Grace Period ends in: §5" + (int) (countdown / 60)  + " §7minutes"), true);
                        } else {
                            player.sendMessage(Text.of("§8✕ §7Grace Period ends in: §5" + countdown + " §7seconds"), true);
                        }
                    });
                    countdown--;
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    private static int startCountdown(ServerCommandSource source, Path configPath) {
        if(ConfigHandler.getState(configPath) == GameState.LOBBY && !startedCountdown) {
            startedCountdown = true;
            scheduler.scheduleAtFixedRate(source.getServer(), new Runnable() {
                int countdown = 30;

                @Override
                public void run() {
                    if (countdown <= 0) {
                        for (ServerWorld world : source.getServer().getWorlds()) {
                            if(world != null) {
                                world.setTimeOfDay(1000);
                                world.getGameRules().get(GameRules.ANNOUNCE_ADVANCEMENTS).set(false, world.getServer());
                                world.getGameRules().get(GameRules.DO_DAYLIGHT_CYCLE).set(true, world.getServer());
                                world.getGameRules().get(GameRules.DO_MOB_SPAWNING).set(true, world.getServer());
                                world.getGameRules().get(GameRules.DO_WEATHER_CYCLE).set(false, world.getServer());
                                WorldBorder worldBorder = world.getWorldBorder();
                                worldBorder.setSize(1500);
                            }
                        }
                        source.getServer().setDifficulty(Difficulty.NORMAL, true);
                        source.getServer().getPlayerManager().getPlayerList().forEach(player -> {
                            player.playSoundToPlayer(SoundEvents.ENTITY_ENDER_DRAGON_GROWL,
                                    SoundCategory.PLAYERS,
                                    1.0F,
                                    1.0F
                            );
                            player.getInventory().clear();
                            player.setExperiencePoints(0);
                            player.setExperienceLevel(0);
                            player.setHealth(20);
                            player.closeHandledScreen();
                            PlayerManager.getInstance().addPlayer(player.getUuid(), player.getName().getString());
                            player.getHungerManager().setSaturationLevel(5);
                            player.changeGameMode(GameMode.SURVIVAL);
                            if(!TeamManager.getInstance().isPlayerInAnyTeam(player.getUuid())) {
                                TeamManager.getInstance().createTeam(player.getName().getString(), player.getUuid(), null);
                            }
                            StartStuffManager.loadStartStuff(player);
                            player.sendMessage(Text.of(OneDayTitan.getPrefix() + "§bThis OneDayTitan starts §3now§b!"), false);
                            player.sendMessage(Text.of(OneDayTitan.getPrefix() + "§cThe WorldBorder has a Radius of §b" + source.getWorld().getWorldBorder().getSize() + " §cBlocks."), false);
                        });
                        clearSaveInv(source.getServer());
                        ConfigHandler.setState(configPath, GameState.STARTED);
                        startProtectionTime(source);
                        scheduler.shutdown();
                    } else {
                        if (countdown == 30 || countdown == 15 || countdown == 10 || countdown == 5 || countdown == 4 || countdown == 3 || countdown == 2) {
                            source.getServer().getPlayerManager().getPlayerList().forEach(player -> {
                                player.playSoundToPlayer(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                                        SoundCategory.PLAYERS,
                                        1.0F,
                                        1.0F
                                );
                                player.sendMessage(Text.of(OneDayTitan.getPrefix() + "§bThis OneDayTitan starts in §3" + countdown + " §bseconds!"), false);
                            });
                        } else if (countdown == 1) {
                            source.getServer().getPlayerManager().getPlayerList().forEach(player -> {
                                player.playSoundToPlayer(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                                        SoundCategory.PLAYERS,
                                        1.0F,
                                        1.0F
                                );
                                player.sendMessage(Text.of(OneDayTitan.getPrefix() + "§bThis OneDayTitan starts in §3" + countdown + " §bsecond!"), false);
                            });
                        }
                        source.getServer().getPlayerManager().getPlayerList().forEach(player -> {
                            player.setExperienceLevel(countdown);
                            player.setExperiencePoints(0);
                        });
                        countdown--;
                    }
                }
            }, 0, 1, TimeUnit.SECONDS);
        } else {
            source.sendFeedback(() -> Text.of(OneDayTitan.getPrefix() + "§cThis OneDayTitan has already began."), false);
        }
        return 1;
    }
    public static void clearSaveInv(MinecraftServer server) {
        if (server == null) {
            return;
        }
        for (ServerWorld world : server.getWorlds()) {
            try {
                PersistentStateManager stateManager = world.getPersistentStateManager();
                stateManager.set("titanfabric", null);
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }
    }
}
