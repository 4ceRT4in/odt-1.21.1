package xyz.frxzenluke.titanreborn.mixin;

import net.minecraft.server.PlayerManager;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerManager.class)
public class BroadcastChatMessageMixin {
    @Inject(method = "broadcast(Lnet/minecraft/text/Text;Z)V", at = @At("HEAD"), cancellable = true)
    private void filterBroadcastMessages(Text message, boolean overlay, CallbackInfo ci) {
        if (message.getContent() instanceof TranslatableTextContent content) {
            String key = content.getKey();
            if (key.equals("multiplayer.player.joined")
                    || key.equals("multiplayer.player.left")
                    || key.equals("multiplayer.player.joined.renamed")
                    || key.startsWith("death.")) {
                ci.cancel();
            }
        }
    }
}
