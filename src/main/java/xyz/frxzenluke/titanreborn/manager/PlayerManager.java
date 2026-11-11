package xyz.frxzenluke.titanreborn.manager;


import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.MinecraftServer;
import xyz.frxzenluke.titanreborn.manager.states.PlayerState;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerManager {
    private static PlayerManager instance;
    private Path alivePlayersFile;
    private JsonObject alivePlayersConfig;

    private PlayerManager() {
        alivePlayersFile = new File("config/players.json").toPath();
        if (!Files.exists(alivePlayersFile)) {
            try {
                Files.createDirectories(alivePlayersFile.getParent());
                Files.createFile(alivePlayersFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        loadAlivePlayers();
    }

    public static PlayerManager getInstance() {
        if (instance == null) {
            instance = new PlayerManager();
        }
        return instance;
    }

    public void loadAlivePlayers() {
        try (Reader reader = Files.newBufferedReader(alivePlayersFile)) {
            JsonElement element = JsonParser.parseReader(reader);
            if (element.isJsonObject()) {
                alivePlayersConfig = element.getAsJsonObject();
            } else {
                alivePlayersConfig = new JsonObject();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveAlivePlayers() {
        try (Writer writer = Files.newBufferedWriter(alivePlayersFile)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(alivePlayersConfig, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addPlayer(UUID uuid, String playerName) {
        JsonObject playerData = new JsonObject();
        playerData.addProperty("name", playerName);
        playerData.addProperty("state", PlayerState.ALIVE.toString());
        alivePlayersConfig.add(uuid.toString(), playerData);
        saveAlivePlayers();
    }

    public boolean isPlayerAdded(UUID uuid) {
        return alivePlayersConfig.has(uuid.toString());
    }

    public UUID getUUIDById(int id) {
        for (Map.Entry<String, JsonElement> entry : alivePlayersConfig.entrySet()) {
            JsonObject playerData = entry.getValue().getAsJsonObject();
            if (playerData.get("id").getAsInt() == id) {
                return UUID.fromString(entry.getKey());
            }
        }
        return null;
    }
    public UUID getUUIDByName(String playerName) {
        for (Map.Entry<String, JsonElement> entry : alivePlayersConfig.entrySet()) {
            JsonObject playerData = entry.getValue().getAsJsonObject();
            if (playerData.has("name") && playerData.get("name").getAsString().equalsIgnoreCase(playerName)) {
                return UUID.fromString(entry.getKey());
            }
        }
        return null;
    }

    public void removePlayer(UUID uuid) {
        alivePlayersConfig.remove(uuid.toString());
        saveAlivePlayers();
    }

    public void setPlayerState(UUID uuid, PlayerState state) {
        if (alivePlayersConfig.has(uuid.toString())) {
            JsonObject playerData = alivePlayersConfig.getAsJsonObject(uuid.toString());
            playerData.addProperty("state", state.toString());
            saveAlivePlayers();
        }
    }

    public PlayerState getPlayerState(UUID uuid) {
        if (alivePlayersConfig.has(uuid.toString())) {
            JsonObject playerData = alivePlayersConfig.getAsJsonObject(uuid.toString());
            return PlayerState.valueOf(playerData.get("state").getAsString());
        }
        return PlayerState.ALIVE;
    }
}