package xyz.frxzenluke.titanreborn.manager;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import net.minecraft.text.Text;
import net.minecraft.scoreboard.Team;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import xyz.frxzenluke.titanreborn.OneDayTitan;
import xyz.frxzenluke.titanreborn.manager.states.PlayerState;
import xyz.frxzenluke.titanreborn.manager.states.TeamState;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class TeamManager {
    private static TeamManager instance;
    private MinecraftServer server;
    private Path teamsFile;
    private JsonObject teamsConfig;

    private TeamManager() {
        server = OneDayTitan.getServerInstance();
        teamsFile = new File("config/teams.json").toPath();
        if (!Files.exists(teamsFile)) {
            try {
                Files.createDirectories(teamsFile.getParent());
                Files.createFile(teamsFile);
                teamsConfig = new JsonObject();
                saveTeams();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            loadTeams();
        }
    }

    public static TeamManager getInstance() {
        if (instance == null) {
            instance = new TeamManager();
        }
        return instance;
    }

    public void loadTeams() {
        try (Reader reader = Files.newBufferedReader(teamsFile)) {
            JsonElement element = JsonParser.parseReader(reader);
            if (element.isJsonObject()) {
                teamsConfig = element.getAsJsonObject();
            } else {
                teamsConfig = new JsonObject();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveTeams() {
        try (Writer writer = Files.newBufferedWriter(teamsFile)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(teamsConfig, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getNextTeamId() {
        return teamsConfig.size();
    }
    public void removeAllTeams() {
        if (server != null) {
            ServerScoreboard scoreboard = server.getScoreboard();
            for (String key : new HashSet<>(teamsConfig.keySet())) {
                JsonObject teamData = teamsConfig.getAsJsonObject(key);
                String teamName = teamData.get("name").getAsString();

                Team scoreboardTeam = scoreboard.getTeam(teamName);
                if (scoreboardTeam != null) {
                    scoreboard.removeTeam(scoreboardTeam);
                }

                teamsConfig.remove(key);
            }
            saveTeams();
        }
    }
    public void createTeam(String teamName, UUID player1, UUID player2) {
        int teamId = getNextTeamId();
        JsonObject teamData = new JsonObject();
        teamData.addProperty("name", teamName);
        teamData.addProperty("id", teamId);
        teamData.addProperty("state", TeamState.ALIVE.toString());

        JsonArray players = new JsonArray();
        players.add(player1.toString());
        if (player2 != null) {
            players.add(player2.toString());
        }
        teamData.add("players", players);

        teamsConfig.add(String.valueOf(teamId), teamData);
        createScoreboardTeam(teamName, teamId, player1, player2);
        saveTeams();
    }
    private void createScoreboardTeam(String teamName, int teamId, UUID player1, UUID player2) {
        if (server == null) {
            System.out.println("Server not Found -> Critcal Error, please do not ignore!");
            return;
        }

        ServerScoreboard scoreboard = server.getScoreboard();
        Team scoreboardTeam = scoreboard.getTeam(teamName);
        if (scoreboardTeam == null) {
            scoreboardTeam = scoreboard.addTeam(teamName);
            scoreboardTeam.setFriendlyFireAllowed(false);
            scoreboardTeam.setPrefix(Text.of("§f(§5" + teamId + "§f) "));
        }
        ServerPlayerEntity playerEntity1 = server.getPlayerManager().getPlayer(player1);
        if (playerEntity1 != null) {
            scoreboard.addScoreHolderToTeam(playerEntity1.getName().getString(), scoreboardTeam);
        }

        if (player2 != null) {
            ServerPlayerEntity playerEntity2 = server.getPlayerManager().getPlayer(player2);
            if (playerEntity2 != null) {
                scoreboard.addScoreHolderToTeam(playerEntity2.getName().getString(), scoreboardTeam);
            }
        }
    }
    public boolean isTeamExists(String teamName) {
        for (String key : teamsConfig.keySet()) {
            JsonObject teamData = teamsConfig.getAsJsonObject(key);
            if (teamData.get("name").getAsString().equalsIgnoreCase(teamName)) {
                return true;
            }
        }
        return false;
    }

    public Set<String> getAllTeams() {
        return teamsConfig.keySet();
    }

    public String getTeamNameById(int teamId) {
        JsonObject teamData = teamsConfig.getAsJsonObject(String.valueOf(teamId));
        if (teamData != null) {
            return teamData.get("name").getAsString();
        }
        return null;
    }

    public String getTeamByPlayer(UUID playerUuid) {
        for (String key : teamsConfig.keySet()) {
            JsonObject teamData = teamsConfig.getAsJsonObject(key);
            JsonArray players = teamData.getAsJsonArray("players");
            for (JsonElement playerElement : players) {
                if (playerElement.getAsString().equals(playerUuid.toString())) {
                    return teamData.get("name").getAsString();
                }
            }
        }
        return null;
    }

    public List<String> getPlayersInTeam(String teamKey) {
        JsonObject teamData = teamsConfig.getAsJsonObject(teamKey);
        if (teamData != null && teamData.has("players")) {
            JsonArray playersArray = teamData.getAsJsonArray("players");
            List<String> players = new ArrayList<>();
            for (JsonElement playerElement : playersArray) {
                players.add(playerElement.getAsString());
            }
            return players;
        }
        return Collections.emptyList();
    }

    public void removeTeam(String teamName) {
        String teamKey = null;
        for (String key : teamsConfig.keySet()) {
            JsonObject teamData = teamsConfig.getAsJsonObject(key);
            if (teamData.get("name").getAsString().equalsIgnoreCase(teamName)) {
                teamKey = key;
                break;
            }
        }

        if (teamKey != null) {
            teamsConfig.remove(teamKey);
            if (server != null) {
                ServerScoreboard scoreboard = server.getScoreboard();
                Team scoreboardTeam = scoreboard.getTeam(teamName);
                if (scoreboardTeam != null) {
                    scoreboard.removeTeam(scoreboardTeam);
                }
            }
            saveTeams();
        }
    }

    public void setTeamState(String teamId, TeamState state) {
        JsonObject teamData = teamsConfig.getAsJsonObject(teamId);
        if (teamData != null) {
            teamData.addProperty("state", state.toString());
            saveTeams();
        }
    }

    public boolean isPlayerInAnyTeam(UUID playerUuid) {
        for (String key : teamsConfig.keySet()) {
            JsonObject teamData = teamsConfig.getAsJsonObject(key);
            JsonArray players = teamData.getAsJsonArray("players");
            for (JsonElement playerElement : players) {
                if (playerElement.getAsString().equals(playerUuid.toString())) {
                    return true;
                }
            }
        }
        return false;
    }

    public TeamState getTeamState(String teamId) {
        JsonObject teamData = teamsConfig.getAsJsonObject(teamId);
        if (teamData != null && teamData.has("state")) {
            return TeamState.valueOf(teamData.get("state").getAsString());
        }
        return TeamState.ALIVE;
    }
}