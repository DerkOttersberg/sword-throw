package io.github.derkottersberg.swordthrow.client;

import static org.junit.jupiter.api.Assertions.*;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.world.entity.HumanoidArm;
import org.junit.jupiter.api.Test;

class ThirdPersonThrowPoseTest {
    @Test
    void siblingSleevesFollowBothModelsHandsAndAnimationPhasesExactlyOnce() {
        for (boolean slim : new boolean[] {false, true}) {
            for (HumanoidArm hand : HumanoidArm.values()) {
                PlayerModel<?> model = model(slim);
                ThrowPoseState pose = chargedPose();
                for (int phase = 0; phase < 8; phase++) {
                    resetArms(model);
                    float before = arm(model, hand).xRot;
                    pose.applyThirdPersonPose(42.5F + phase, hand, model);
                    assertPoseEquals(model.rightArm.storePose(), model.rightSleeve.storePose());
                    assertPoseEquals(model.leftArm.storePose(), model.leftSleeve.storePose());
                    if (phase == 0) {
                        assertNotEquals(before, arm(model, hand).xRot);
                        pose.releaseForward(1.0F);
                    }
                    assertTrue(Float.isFinite(model.rightArm.xRot));
                    assertTrue(Float.isFinite(model.leftArm.xRot));
                    pose.tick();
                }
                assertTrue(pose.isIdle());
            }
        }
    }

    @Test
    void copyingPosePreservesHiddenSkinLayers() {
        PlayerModel<?> model = model(true);
        model.rightSleeve.visible = false;
        model.leftSleeve.visible = false;
        model.rightSleeve.skipDraw = true;
        chargedPose().applyThirdPersonPose(42.5F, HumanoidArm.LEFT, model);
        assertPoseEquals(model.rightArm.storePose(), model.rightSleeve.storePose());
        assertFalse(model.rightSleeve.visible);
        assertFalse(model.leftSleeve.visible);
        assertTrue(model.rightSleeve.skipDraw);
    }

    @Test
    void resetPreventsAccumulationOrLeakingToTheNextPlayer() {
        PlayerModel<?> model = model(false);
        ThrowPoseState pose = chargedPose();
        var baseline = model.rightArm.storePose();
        pose.applyThirdPersonPose(42.5F, HumanoidArm.RIGHT, model);
        var charged = model.rightArm.storePose();
        resetArms(model);
        pose.applyThirdPersonPose(42.5F, HumanoidArm.RIGHT, model);
        assertPoseEquals(charged, model.rightArm.storePose());
        pose.cancel();
        resetArms(model);
        pose.applyThirdPersonPose(42.5F, HumanoidArm.RIGHT, model);
        assertPoseEquals(baseline, model.rightArm.storePose());
        assertPoseEquals(model.rightArm.storePose(), model.rightSleeve.storePose());
    }

    @Test
    void invalidHandAgeAndProgressCannotPoisonTheModel() {
        PlayerModel<?> model = model(false);
        var baseline = model.rightArm.storePose();
        ThrowPoseState pose = chargedPose();
        pose.applyThirdPersonPose(42.5F, null, model);
        assertPoseEquals(baseline, model.rightArm.storePose());
        pose.applyThirdPersonPose(Float.NaN, HumanoidArm.RIGHT, model);
        assertPoseEquals(baseline, model.rightArm.storePose());
        for (float invalid : new float[] {Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY}) {
            pose = new ThrowPoseState();
            pose.setChargeProgress(invalid);
            pose.tick();
            assertEquals(0.0F, pose.getChargeIndicatorProgress(1.0F));
            pose.releaseForward(invalid);
            pose.applyThirdPersonPose(42.5F, HumanoidArm.RIGHT, model);
            assertTrue(Float.isFinite(model.rightArm.xRot));
        }
    }

    private static void assertPoseEquals(net.minecraft.client.model.geom.PartPose expected,
                                         net.minecraft.client.model.geom.PartPose actual) {
        assertEquals(expected.x, actual.x);
        assertEquals(expected.y, actual.y);
        assertEquals(expected.z, actual.z);
        assertEquals(expected.xRot, actual.xRot);
        assertEquals(expected.yRot, actual.yRot);
        assertEquals(expected.zRot, actual.zRot);
    }

    private static PlayerModel<?> model(boolean slim) {
        return new PlayerModel<>(LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE, slim), 64, 64).bakeRoot(), slim);
    }
    private static ModelPart arm(PlayerModel<?> model, HumanoidArm hand) {
        return hand == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
    }
    private static void resetArms(PlayerModel<?> model) {
        model.rightArm.resetPose();
        model.leftArm.resetPose();
    }
    private static ThrowPoseState chargedPose() {
        ThrowPoseState pose = new ThrowPoseState();
        pose.beginCharge();
        pose.setChargeProgress(1.0F);
        for (int tick = 0; tick < 30; tick++) pose.tick();
        return pose;
    }
}
