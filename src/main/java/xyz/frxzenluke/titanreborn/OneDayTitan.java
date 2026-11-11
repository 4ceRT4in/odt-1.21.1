package xyz.frxzenluke.titanreborn;

import net.fabricmc.api.DedicatedServerModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.world.GameRules;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.frxzenluke.titanreborn.command.*;
import xyz.frxzenluke.titanreborn.event.PlayerDisconnectEvent;
import xyz.frxzenluke.titanreborn.event.PlayerJoinEvent;
import xyz.frxzenluke.titanreborn.handler.ConfigHandler;
import xyz.frxzenluke.titanreborn.handler.GameState;

import java.nio.file.Path;
import java.nio.file.Paths;

public class OneDayTitan implements DedicatedServerModInitializer {
	private static MinecraftServer serverInstance;
	public static final String MOD_ID = "onedaytitan";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static String Prefix = "§8[§5OneDayTitan§8] §r";
	private static final Path CONFIG_PATH = Paths.get("config", "config.json");
	public static boolean soloTeams = false;


	@Override
	public void onInitializeServer() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			serverInstance = server;
			if(ConfigHandler.getState(OneDayTitan.getConfigPath()) != GameState.STARTED) {
				for (ServerWorld world : server.getWorlds()) {
					if (world != null) {
						world.getGameRules().get(GameRules.ANNOUNCE_ADVANCEMENTS).set(false, world.getServer());
						world.getGameRules().get(GameRules.DO_DAYLIGHT_CYCLE).set(false, world.getServer());
						world.getGameRules().get(GameRules.DO_MOB_SPAWNING).set(false, world.getServer());
						world.getGameRules().get(GameRules.DO_WEATHER_CYCLE).set(false, world.getServer());
					}
				}
			}
		});
		ConfigHandler.loadConfig(CONFIG_PATH);
		StartCommand.register(CONFIG_PATH);
		StartFinaleCommand.register(CONFIG_PATH);
		AbortProtectionTimeCommand.register();
		StartStuffCommand.register();
		SetSpawnCommand.register();
		TeamSizeCommand.register();
		ReviveCommand.register();
		InvseeCommand.register();
		TeamCommand.register();
		ReloadCommand.register();
		SpawnCommand.register();
		ResetCommand.register();
		BorderCommand.register();
		ServerPlayConnectionEvents.JOIN.register(new PlayerJoinEvent());
		ServerPlayConnectionEvents.DISCONNECT.register(new PlayerDisconnectEvent());


		LOGGER.info("OneDayTitan initialized.");
	}

	public static MinecraftServer getServerInstance() {
		return serverInstance;
	}
	public static String getPrefix() {
		return Prefix;
	}
	public static Path getConfigPath() {
		return CONFIG_PATH;
	}
}
