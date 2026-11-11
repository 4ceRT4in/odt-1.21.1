package xyz.frxzenluke.titanreborn.event;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.text.Text;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.MinecraftServer;
import xyz.frxzenluke.titanreborn.OneDayTitan;
import xyz.frxzenluke.titanreborn.handler.ConfigHandler;
import xyz.frxzenluke.titanreborn.manager.PlayerManager;
import xyz.frxzenluke.titanreborn.manager.TeamManager;
import xyz.frxzenluke.titanreborn.manager.ServerScheduler;
import xyz.frxzenluke.titanreborn.manager.states.PlayerState;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class PlayerDisconnectEvent implements ServerPlayConnectionEvents.Disconnect{
    private static final ServerScheduler reconnectScheduler = new ServerScheduler();
    static final Map<UUID, ServerScheduler.TaskHandle> disconnectTimers = new HashMap<>();


    @Override
    public void onPlayDisconnect(ServerPlayNetworkHandler handler, MinecraftServer server) {
        switch (ConfigHandler.getState(OneDayTitan.getConfigPath())) {
            case LOBBY -> {
                if (TeamManager.getInstance().isPlayerInAnyTeam(handler.player.getUuid())) {
                    TeamManager.getInstance().removeTeam(TeamManager.getInstance().getTeamNameById(TeamManager.getInstance().getNextTeamId() - 1));
                }
                server.getPlayerManager().getPlayerList().forEach(player -> {
                    player.sendMessage(Text.of(OneDayTitan.getPrefix() + "§5" + handler.player.getName().getString() + " §bhas left the game."), false);
                });
            }
            case STARTED -> {
                if (PlayerManager.getInstance().isPlayerAdded(handler.player.getUuid()) && PlayerManager.getInstance().getPlayerState(handler.player.getUuid()) == PlayerState.ALIVE) {
                    UUID playerId = handler.player.getUuid();
                    String playerName = handler.player.getName().getString();
                    ServerScheduler.TaskHandle timer = reconnectScheduler.schedule(server, () -> {
                        PlayerManager.getInstance().setPlayerState(playerId, PlayerState.DEAD);
                        server.getPlayerManager().getPlayerList().forEach(player -> {
                            player.sendMessage(Text.of(OneDayTitan.getPrefix() + "§5" + playerName + " §chasn't reconnected for 5 minutes, therefore he is now removed from this OneDayTitan!"), false);
                        });
                        disconnectTimers.remove(playerId);
                    }, 5, TimeUnit.MINUTES);
                    disconnectTimers.put(playerId, timer);
                    server.getPlayerManager().getPlayerList().forEach(player -> {
                        player.sendMessage(Text.of(OneDayTitan.getPrefix() + "§5" + handler.player.getName().getString() + " §bhas left the game. He has §55 §bminutes to reconnect!"), false);
                    });
                }
            }
        }
    }
}
