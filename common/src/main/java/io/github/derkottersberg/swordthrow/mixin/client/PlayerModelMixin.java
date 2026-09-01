package io.github.derkottersberg.swordthrow.mixin.client;

import io.github.derkottersberg.swordthrow.client.SwordThrowClient;
import io.github.derkottersberg.swordthrow.client.ThrowPoseState;
import io.github.derkottersberg.swordthrow.client.config.SwordThrowClientConfig;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.HumanoidArm;
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

        HumanoidArm mainArm = state.mainArm;
        if (mainArm == null) {
            return;
        }

        PlayerModel model = (PlayerModel) (Object) this;
        HumanoidArm offArm = mainArm.getOpposite();
        poseState.applyThirdPersonMainHandPose(state.ageInTicks, mainArm, model.getArm(mainArm));
        poseState.applyThirdPersonOffHandPose(state.ageInTicks, offArm, model.getArm(offArm));

        swordthrow$copyPartTransform(model.rightSleeve, model.rightArm);
        swordthrow$copyPartTransform(model.leftSleeve, model.leftArm);
    }

    private static void swordthrow$copyPartTransform(ModelPart target, ModelPart source) {
        target.x = source.x;
        target.y = source.y;
        target.z = source.z;
        target.xRot = source.xRot;
        target.yRot = source.yRot;
        target.zRot = source.zRot;
        target.xScale = source.xScale;
        target.yScale = source.yScale;
        target.zScale = source.zScale;
        target.visible = source.visible;
        target.skipDraw = source.skipDraw;
    }
}
