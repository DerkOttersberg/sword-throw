package io.github.derkottersberg.swordthrow.mixin.client;

import io.github.derkottersberg.swordthrow.client.SwordThrowClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class HudMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void swordthrow$extractChargeBar(
        GuiGraphics graphics,
        float partialTick,
        CallbackInfo callback
    ) {
        SwordThrowClient.renderChargeBar(graphics);
    }
}
