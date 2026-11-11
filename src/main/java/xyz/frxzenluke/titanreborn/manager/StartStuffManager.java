package xyz.frxzenluke.titanreborn.manager;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import xyz.frxzenluke.titanreborn.OneDayTitan;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class StartStuffManager {
    private static final Path CONFIG_FILE = Path.of("config", "startStuff.dat");

    public static void saveStartStuff(ServerPlayerEntity player) {
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            NbtCompound data = new NbtCompound();
            data.put("items", player.getInventory().writeNbt(new NbtList()));
            NbtIo.writeCompressed(data, CONFIG_FILE);
            player.sendMessage(Text.of(OneDayTitan.getPrefix() + "§aStart Equipment successfully saved!"), false);
        } catch (IOException e) {
            OneDayTitan.LOGGER.error("Could not save start equipment", e);
            player.sendMessage(Text.of("§cError saving start stuff! -> look console for more details"), false);
        }
    }

    public static void loadStartStuff(ServerPlayerEntity player) {
        if (!Files.exists(CONFIG_FILE)) {
            return;
        }
        try {
            NbtCompound data = NbtIo.readCompressed(CONFIG_FILE, NbtSizeTracker.ofUnlimitedBytes());
            player.getInventory().clear();
            player.getInventory().readNbt(data.getList("items", 10));
        } catch (IOException e) {
            OneDayTitan.LOGGER.error("Could not load start equipment", e);
        }
    }
}
