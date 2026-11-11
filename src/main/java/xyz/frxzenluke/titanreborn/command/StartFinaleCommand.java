package xyz.frxzenluke.titanreborn.command;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.text.Text;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import net.minecraft.server.MinecraftServer;
import xyz.frxzenluke.titanreborn.OneDayTitan;
import xyz.frxzenluke.titanreborn.event.PlayerJoinEvent;
import xyz.frxzenluke.titanreborn.handler.ConfigHandler;
import xyz.frxzenluke.titanreborn.handler.GameState;
import xyz.frxzenluke.titanreborn.manager.PlayerManager;
import xyz.frxzenluke.titanreborn.manager.StartStuffManager;
import xyz.frxzenluke.titanreborn.manager.TeamManager;
import xyz.frxzenluke.titanreborn.manager.ServerScheduler;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public class StartFinaleCommand {
    private static final ServerScheduler scheduler = new ServerScheduler();
    public static final ServerScheduler protectionTimeScheduler = new ServerScheduler();

    public static boolean isFinaleStarted = false;

    public static void register(Path configPath) {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("startfinale")
                    .requires(source -> source.hasPermissionLevel(2))
                    .executes(context -> startCountdown(context.getSource(), configPath))
            );
        });
    }

    private static void startProtectionTime(ServerCommandSource source) {
        source.getServer().getPlayerManager().getPlayerList().forEach(player -> {
            player.playSoundToPlayer(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(),
                    SoundCategory.PLAYERS,
                    1.0F,
                    1.0F
            );
            player.sendMessage(Text.of(OneDayTitan.getPrefix() + "§cThe grace period lasts §b30 §cseconds!"), false);
        });

        protectionTimeScheduler.scheduleAtFixedRate(source.getServer(), new Runnable() {
            int countdown = 30;
            @Override
            public void run() {
                if (countdown <= 0) {
                    StartCommand.protectionTime = false;
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
                    if (countdown == 30 || countdown == 15 || countdown == 10 || countdown == 5 || countdown == 4 || countdown == 3 || countdown == 2) {
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
                        player.sendMessage(Text.of("§8✕ §7Grace Period ends in: §5" + countdown + " §7seconds"), true);
                    });
                    countdown--;
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    private static int startCountdown(ServerCommandSource source, Path configPath) {
        if(ConfigHandler.getState(configPath) == GameState.STARTED) {
            if(isFinaleStarted) {
                source.sendFeedback(() -> Text.of(OneDayTitan.getPrefix() + "§cThe Finale was already started!"), false);
                return 0;
            }
            isFinaleStarted = true;
            scheduler.scheduleAtFixedRate(source.getServer(), new Runnable() {
                int countdown = 30;

                @Override
                public void run() {
                    if (countdown <= 0) {
                        for (ServerWorld world : source.getServer().getWorlds()) {
                            if(world != null) {
                                WorldBorder worldBorder = world.getWorldBorder();
                                worldBorder.setSize(200);
                            }
                        }
                        StartCommand.protectionTimeScheduler.cancelAll();
                        StartCommand.protectionTime = true;
                        BlockPos spawnPos = PlayerJoinEvent.readSpawnCoordinates();
                        source.getServer().getPlayerManager().getPlayerList().forEach(player -> {
                            player.playSoundToPlayer(SoundEvents.ENTITY_ENDER_DRAGON_GROWL,
                                    SoundCategory.PLAYERS,
                                    1.0F,
                                    1.0F
                            );
                            if (spawnPos != null) {
                                ServerWorld world = source.getServer().getOverworld();
                                player.teleport(world, spawnPos.getX(), spawnPos.getY() + 10, spawnPos.getZ(), 0, 0);
                            }
                            player.sendMessage(Text.of(OneDayTitan.getPrefix() + "§bThe Finale starts §3now§b!"), false);
                            player.sendMessage(Text.of(OneDayTitan.getPrefix() + "§cThe WorldBorder has a Radius of §b" + source.getWorld().getWorldBorder().getSize() + " §cBlocks."), false);
                        });
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
                                player.sendMessage(Text.of(OneDayTitan.getPrefix() + "§bThe Finale starts in §3" + countdown + " §bseconds!"), false);
                            });
                        } else if (countdown == 1) {
                            source.getServer().getPlayerManager().getPlayerList().forEach(player -> {
                                player.playSoundToPlayer(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                                        SoundCategory.PLAYERS,
                                        1.0F,
                                        1.0F
                                );
                                player.sendMessage(Text.of(OneDayTitan.getPrefix() + "§bThe Finale starts in §3" + countdown + " §bsecond!"), false);
                            });
                        }
                        countdown--;
                    }
                }
            }, 0, 1, TimeUnit.SECONDS);
        } else {
            source.sendFeedback(() -> Text.of(OneDayTitan.getPrefix() + "§cThe game must be started in order for the finale to happen!"), false);
        }
        return 1;
    }
}
