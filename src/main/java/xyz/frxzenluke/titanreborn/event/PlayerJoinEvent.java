package xyz.frxzenluke.titanreborn.event;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.GameMode;
import net.minecraft.util.math.BlockPos;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.Identifier;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import net.minecraft.server.MinecraftServer;
import xyz.frxzenluke.titanreborn.OneDayTitan;
import xyz.frxzenluke.titanreborn.handler.ConfigHandler;
import xyz.frxzenluke.titanreborn.manager.PlayerManager;
import xyz.frxzenluke.titanreborn.manager.StartStuffManager;
import xyz.frxzenluke.titanreborn.manager.InviteItem;
import xyz.frxzenluke.titanreborn.manager.states.PlayerState;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.UUID;

public class PlayerJoinEvent implements ServerPlayConnectionEvents.Join {

    @Override
    public void onPlayReady(ServerPlayNetworkHandler handler, PacketSender sender, MinecraftServer server) {
        switch (ConfigHandler.getState(OneDayTitan.getConfigPath())) {
            case LOBBY -> {
                removePlayerFromAllTeams(handler.player, server);
                handler.player.changeGameMode(GameMode.ADVENTURE);
                handler.player.getInventory().clear();
                handler.player.setHealth(20);
                handler.player.getHungerManager().setSaturationLevel(10000);
                handler.player.getHungerManager().setFoodLevel(20);
                handler.player.setHealth(20);
                handler.player.setExperienceLevel(0);
                handler.player.setExperiencePoints(0);
                handler.player.playSoundToPlayer(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                        SoundCategory.PLAYERS,
                        1.0F,
                        1.0F
                );
                if(!OneDayTitan.soloTeams) {
                    handler.player.getInventory().setStack(0, InviteItem.create());
                }
                BlockPos spawnPos = readSpawnCoordinates();
                if (spawnPos != null) {
                    ServerWorld world = server.getOverworld();
                    handler.player.teleport(world, spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), 0, 0);
                }
                server.getPlayerManager().getPlayerList().forEach(player -> {
                    player.sendMessage(Text.of(OneDayTitan.getPrefix() + "§5" + handler.getPlayer().getName().getString() + " §bhas joined the game."), false);
                });
            }
            case STARTED -> {
                if(!PlayerManager.getInstance().isPlayerAdded(handler.player.getUuid()) || PlayerManager.getInstance().getPlayerState(handler.player.getUuid()) != PlayerState.ALIVE) {
                    handler.player.playSoundToPlayer(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(),
                            SoundCategory.PLAYERS,
                            1.0F,
                            1.0F
                    );
                    handler.player.sendMessage(Text.of(OneDayTitan.getPrefix() + "§cYou joined a running game, therefore you are now a spectator..."), false);
                    handler.player.changeGameMode(GameMode.SPECTATOR);
                } else if (PlayerManager.getInstance().isPlayerAdded(handler.player.getUuid()) && PlayerManager.getInstance().getPlayerState(handler.player.getUuid()) == PlayerState.ALIVE) {
                    handler.player.playSoundToPlayer(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                            SoundCategory.PLAYERS,
                            1.0F,
                            1.0F
                    );
                    if (PlayerDisconnectEvent.disconnectTimers.containsKey(handler.player.getUuid())) {
                        PlayerDisconnectEvent.disconnectTimers.get(handler.player.getUuid()).cancel();
                        PlayerDisconnectEvent.disconnectTimers.remove(handler.player.getUuid());
                    }
                    server.getPlayerManager().getPlayerList().forEach(player -> {
                        player.sendMessage(Text.of(OneDayTitan.getPrefix() + "§5" + handler.player.getName().getString() + " §bhas reconnected."), false);
                    });
                }
            }
        }
    }

    public static BlockPos readSpawnCoordinates() {
        File configDir = new File("config");
        File spawnFile = new File(configDir, "spawn.json");

        if (spawnFile.exists()) {
            try (FileReader reader = new FileReader(spawnFile)) {
                JsonObject spawnData = new Gson().fromJson(reader, JsonObject.class);
                int x = spawnData.get("x").getAsInt();
                int y = spawnData.get("y").getAsInt();
                int z = spawnData.get("z").getAsInt();
                return new BlockPos(x, y, z);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private static void removePlayerFromAllTeams(PlayerEntity player, MinecraftServer server) {
        ServerScoreboard scoreboard = server.getScoreboard();
        for (Team team : scoreboard.getTeams()) {
            if (team.getPlayerList().contains(player.getName().getString())) {
                scoreboard.removeScoreHolderFromTeam(player.getName().getString(), team);
            }
        }
    }
}
