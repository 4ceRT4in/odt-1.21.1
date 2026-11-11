package xyz.frxzenluke.titanreborn.command;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.text.Text;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import net.minecraft.server.MinecraftServer;
import xyz.frxzenluke.titanreborn.OneDayTitan;
import xyz.frxzenluke.titanreborn.manager.StartStuffManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class SetSpawnCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    LiteralArgumentBuilder.<ServerCommandSource>literal("setspawn").requires(source -> source.hasPermissionLevel(2))
                            .executes(SetSpawnCommand::execute)
            );
        });
    }
    private static int execute(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        if (source.getEntity() instanceof ServerPlayerEntity player) {
            Vec3d pos = player.getPos();
            BlockPos spawnPos = BlockPos.ofFloored(pos);
            File configDir = new File("config");
            if (!configDir.exists()) {
                configDir.mkdirs();
            }
            File spawnFile = new File(configDir, "spawn.json");
            JsonObject spawnData = new JsonObject();
            spawnData.addProperty("x", spawnPos.getX());
            spawnData.addProperty("y", spawnPos.getY());
            spawnData.addProperty("z", spawnPos.getZ());
            try (FileWriter writer = new FileWriter(spawnFile)) {
                Gson gson = new Gson();
                gson.toJson(spawnData, writer);
            } catch (IOException e) {
                e.printStackTrace();
                source.sendFeedback(() -> Text.of("§cFailed to save spawn coordinates."), false);
                return 1;
            }
            player.playSoundToPlayer(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                    SoundCategory.PLAYERS,
                    1.0F,
                    1.0F
            );
            MinecraftServer server = source.getServer();
            server.getWorlds().forEach(world -> {
                WorldBorder worldBorder = world.getWorldBorder();
                worldBorder.setCenter(spawnPos.getX(), spawnPos.getZ());
                worldBorder.setSize(50);
                world.setSpawnPos(spawnPos, 0);
            });
            source.sendFeedback(() -> Text.of(OneDayTitan.getPrefix() + "§aThe spawn has successfully been set to your current location."), false);
        }

        return 1;
    }
}
