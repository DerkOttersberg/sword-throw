package io.github.derkottersberg.swordthrow.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.derkottersberg.swordthrow.client.ThrowPoseState;
import io.github.derkottersberg.swordthrow.client.SwordThrowClient;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class ItemInHandRendererMixin {
    @Inject(
        method = "submitArmWithItem",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", shift = At.Shift.AFTER)
    )
    private void swordthrow$applyThrowPose(
        PlayerRenderState player,
        FirstPersonHandsAndItemsRenderState hands,
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
        if (!player.hasPlayer || player.avatarRenderState == null || player.avatarRenderState.isInvisible) {
            return;
        }

        if (hand == InteractionHand.MAIN_HAND && !item.isEmpty()) {
            SwordThrowClient.localPoseState().applyMainHandPose(
                player.avatarRenderState.ageInTicks, player.avatarRenderState.mainArm, poseStack, partialTick);
            return;
        }

        ThrowPoseState poseState = SwordThrowClient.localPoseState();
        if (hand == InteractionHand.OFF_HAND && item.isEmpty() && poseState.isOffHandVisible()) {
            HumanoidArm offArm = player.avatarRenderState.mainArm.getOpposite();
            poseState.applyOffHandAimContext(player.avatarRenderState.ageInTicks, poseStack, offArm, partialTick);
            ((ItemInHandRendererAccessor) (Object) this).swordthrow$renderPlayerArm(
                poseStack,
                collector,
                packedLight,
                0.0F,
                swingProgress,
                offArm,
                player
            );
        }
    }
}
