package io.github.derkottersberg.swordthrow.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.derkottersberg.swordthrow.client.ThrowPoseState;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
    @Inject(
        method = "submitArmWithItem",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", shift = At.Shift.AFTER)
    )
    private void swordthrow$applyThrowPose(
        AbstractClientPlayer player,
        float partialTick,
        float pitch,
        InteractionHand hand,
        float swingProgress,
        ItemStack item,
        float equipProgress,
        PoseStack poseStack,
        SubmitNodeCollector collector,
        int packedLight,
        CallbackInfo callback
    ) {
        if (player.isInvisible()) {
            return;
        }

        if (hand == InteractionHand.MAIN_HAND && !item.isEmpty()) {
            ThrowPoseState.applyMainHandPose(player, poseStack, partialTick);
            return;
        }

        if (hand == InteractionHand.OFF_HAND && item.isEmpty() && ThrowPoseState.isOffHandVisible()) {
            HumanoidArm offArm = player.getMainArm().getOpposite();
            ThrowPoseState.applyOffHandAimContext(player, poseStack, offArm, partialTick);
            ((ItemInHandRendererAccessor) (Object) this).swordthrow$renderPlayerArm(
                poseStack,
                collector,
                packedLight,
                0.0F,
                swingProgress,
                offArm
            );
        }
    }
}
