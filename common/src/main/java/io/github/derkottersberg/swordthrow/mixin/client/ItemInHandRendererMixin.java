package io.github.derkottersberg.swordthrow.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.derkottersberg.swordthrow.client.ThrowPoseState;
import io.github.derkottersberg.swordthrow.client.SwordThrowClient;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.player.AbstractClientPlayer;
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
        method = "renderArmWithItem",
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
        MultiBufferSource buffers,
        int packedLight,
        CallbackInfo callback
    ) {
        if (player.isInvisible()) {
            return;
        }

        if (hand == InteractionHand.MAIN_HAND && !item.isEmpty()) {
            SwordThrowClient.localPoseState().applyMainHandPose(
                player.tickCount + partialTick, player.getMainArm(), poseStack, partialTick);
            return;
        }

        ThrowPoseState poseState = SwordThrowClient.localPoseState();
        if (hand == InteractionHand.OFF_HAND && item.isEmpty() && poseState.isOffHandVisible()) {
            HumanoidArm offArm = player.getMainArm().getOpposite();
            poseState.applyOffHandAimContext(player.tickCount + partialTick, poseStack, offArm, partialTick);
            ((ItemInHandRendererAccessor) (Object) this).swordthrow$renderPlayerArm(
                poseStack,
                buffers,
                packedLight,
                0.0F,
                swingProgress,
                offArm
            );
        }
    }
}
