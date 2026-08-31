package io.github.derkottersberg.swordthrow.mixin.client;

import io.github.derkottersberg.swordthrow.client.SwordThrowClient;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public abstract class HudMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void swordthrow$extractChargeBar(
        GuiGraphicsExtractor graphics,
        DeltaTracker deltaTracker,
        CallbackInfo callback
    ) {
        SwordThrowClient.renderChargeBar(graphics);
    }
}
