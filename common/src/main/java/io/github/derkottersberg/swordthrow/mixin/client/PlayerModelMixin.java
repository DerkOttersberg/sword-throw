package io.github.derkottersberg.swordthrow.mixin.client;

import io.github.derkottersberg.swordthrow.client.SwordThrowClient;
import io.github.derkottersberg.swordthrow.client.ThrowPoseState;
import io.github.derkottersberg.swordthrow.client.config.SwordThrowClientConfig;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"))
    private void swordthrow$applyThirdPersonChargePose(AvatarRenderState state, CallbackInfo callback) {
        if (!SwordThrowClientConfig.get().thirdPersonAnimationsEnabled()) {
            return;
        }

        ThrowPoseState poseState = SwordThrowClient.poseStateFor(state.id);
        if (poseState == null) {
            return;
        }

        PlayerModel model = (PlayerModel) (Object) this;
        poseState.applyThirdPersonPose(state, model);
    }
}
