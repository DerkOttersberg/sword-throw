package io.github.derkottersberg.swordthrow.mixin.client;

import io.github.derkottersberg.swordthrow.client.SwordThrowClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public class LocalPlayerMixin {

    @Inject(method = "dropItem", at = @At("HEAD"), cancellable = true)
    private void swordthrow$redirectDropKeyToThrow(LocalPlayer player, boolean dropAll, CallbackInfo cir) {
        if (dropAll) {
            return;
        }

        if (SwordThrowClient.consumeSingleItemDropBypass()) {
            return;
        }

        if (SwordThrowClient.shouldInterceptDropKey(Minecraft.getInstance())) {
            cir.cancel();
        }
    }
}
