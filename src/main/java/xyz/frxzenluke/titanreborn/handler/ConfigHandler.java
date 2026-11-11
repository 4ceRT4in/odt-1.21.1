package xyz.frxzenluke.titanreborn.handler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigHandler {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final GameState DEFAULT_STATE = GameState.LOBBY;

    public static void loadConfig(Path configPath) {
        try {
            if (!Files.exists(configPath)) {
                createDefaultConfig(configPath);
            } else {
                String content = new String(Files.readAllBytes(configPath));
                JsonObject jsonObject = GSON.fromJson(content, JsonObject.class);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void createDefaultConfig(Path configPath) {
        try {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("state", DEFAULT_STATE.name());

            Files.createDirectories(configPath.getParent());
            Files.write(configPath, GSON.toJson(jsonObject).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void setState(Path configPath, GameState state) {
        try {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("state", state.name());

            Files.write(configPath, GSON.toJson(jsonObject).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static GameState getState(Path configPath) {
        try {
            if (Files.exists(configPath)) {
                String content = new String(Files.readAllBytes(configPath));
                JsonObject jsonObject = GSON.fromJson(content, JsonObject.class);
                return GameState.fromString(jsonObject.get("state").getAsString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return DEFAULT_STATE;
    }
}