package xyz.frxzenluke.titanreborn.handler;


public enum GameState {
    STARTED,
    LOBBY;
    public static GameState fromString(String value) {
        try {
            return GameState.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            return LOBBY;
        }
    }
}