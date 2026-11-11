package xyz.frxzenluke.titanreborn.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import xyz.frxzenluke.titanreborn.OneDayTitan;

public class AbortProtectionTimeCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    LiteralArgumentBuilder.<ServerCommandSource>literal("abortprotectiontime").requires(source -> source.hasPermissionLevel(2))
                            .executes(AbortProtectionTimeCommand::execute)
            );
        });
    }
    private static int execute(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        if(StartCommand.protectionTime) {
            StartCommand.protectionTime = false;
            StartCommand.protectionTimeScheduler.cancelAll();
            StartFinaleCommand.protectionTimeScheduler.cancelAll();
            source.getServer().getPlayerManager().getPlayerList().forEach(player -> {
                player.playSoundToPlayer(SoundEvents.ENTITY_ENDER_DRAGON_GROWL,
                        SoundCategory.PLAYERS,
                        1.0F,
                        1.0F
                );
                player.sendMessage(Text.of(OneDayTitan.getPrefix() + "§cThe grace period is now over!"), false);
                player.sendMessage(Text.of(OneDayTitan.getPrefix() + "§cThe grace period was forcefully ended by a Staff Member."), false);
            });
        }
        return 1;
    }
}
