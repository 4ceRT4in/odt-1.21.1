package xyz.frxzenluke.titanreborn.mixin;

import net.minecraft.world.GameMode;
import net.minecraft.text.Text;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import xyz.frxzenluke.titanreborn.OneDayTitan;
import xyz.frxzenluke.titanreborn.handler.ConfigHandler;
import xyz.frxzenluke.titanreborn.handler.GameState;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class MixinServerPlayNetworkHandler implements ServerPlayPacketListener {

    @Shadow
    public ServerPlayerEntity player;

    @Inject(method = "onChatMessage", at = @At("HEAD"), cancellable = true)
    public void onChatMessage(ChatMessageC2SPacket packet, CallbackInfo ci) {
        if(packet.chatMessage().startsWith("/me")) {
            ci.cancel();
            return;
        }
        if(packet.chatMessage().startsWith("/")){
            return;
        }
        if(packet.chatMessage().contains("nigger") || packet.chatMessage().contains("nigge") || packet.chatMessage().contains("nigga") || packet.chatMessage().contains("negger") || packet.chatMessage().contains("negga") || packet.chatMessage().contains("niga")) {
            player.sendMessage(Text.of(OneDayTitan.getPrefix() + "§cWe do not support racism!"), false);
            ci.cancel();
            return;
        }
        if(player.hasPermissionLevel(2)) {
            OneDayTitan.getServerInstance().getPlayerManager().broadcast(Text.of("§cStaff §8| §c" + player.getName().getString() + " §8» §f" + packet.chatMessage()), false);
        } else {
            OneDayTitan.getServerInstance().getPlayerManager().broadcast(Text.of("§7" + player.getName().getString() + " §8» §f" + packet.chatMessage()), false);
        }
        ci.cancel();
    }


    @Inject(at = @At("HEAD"), method = "onPlayerAction", cancellable = true)
    public void onPlayerAction(PlayerActionC2SPacket packet, CallbackInfo ci) {
        if(ConfigHandler.getState(OneDayTitan.getConfigPath()) == GameState.LOBBY) {
            if ((packet.getAction() == PlayerActionC2SPacket.Action.DROP_ITEM || packet.getAction() == PlayerActionC2SPacket.Action.DROP_ALL_ITEMS) || packet.getAction() == PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND) {
                if (player.interactionManager.getGameMode() != GameMode.CREATIVE) {
                    ci.cancel();
                    syncInventory();
                }
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "onClickSlot", cancellable = true)
    public void onClickSlot(ClickSlotC2SPacket packet, CallbackInfo ci) {
        if(ConfigHandler.getState(OneDayTitan.getConfigPath()) == GameState.LOBBY && player.interactionManager.getGameMode() != GameMode.CREATIVE) {
            ci.cancel();
            syncInventory();
        }
    }

    private void syncInventory() {
        ServerPlayerEntity player = ((ServerPlayNetworkHandler)(Object)this).player;
        player.networkHandler.sendPacket(new InventoryS2CPacket(
                player.currentScreenHandler.syncId,
                0,
                player.currentScreenHandler.getStacks(),
                player.currentScreenHandler.getCursorStack()
        ));
    }
}
