package io.github.derkottersberg.swordthrow.mixin.client;

import io.github.derkottersberg.swordthrow.client.SwordThrowClient;
import io.github.derkottersberg.swordthrow.client.ThrowPoseState;
import io.github.derkottersberg.swordthrow.client.config.SwordThrowClientConfig;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void swordthrow$applyThirdPersonChargePose(LivingEntity entity, float limbSwing, float limbAmount,
        float age, float headYaw, float headPitch, CallbackInfo callback) {
        if (!(entity instanceof AbstractClientPlayer player) || !SwordThrowClientConfig.get().thirdPersonAnimationsEnabled()) {
            return;
        }

        ThrowPoseState poseState = SwordThrowClient.poseStateFor(player.getId());
        if (poseState == null) {
            return;
        }

        PlayerModel<?> model = (PlayerModel<?>) (Object) this;
        poseState.applyThirdPersonPose(player, age, model);
    }
}
