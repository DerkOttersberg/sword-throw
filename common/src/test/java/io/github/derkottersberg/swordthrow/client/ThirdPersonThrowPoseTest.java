package io.github.derkottersberg.swordthrow.client;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.HumanoidArm;
import org.junit.jupiter.api.Test;

class ThirdPersonThrowPoseTest {
    @Test
    void sleevesInheritOnceForBothModelsHandsAndAnimationPhases() {
        for (boolean slim : new boolean[] {false, true}) {
            for (HumanoidArm mainArm : HumanoidArm.values()) {
                PlayerModel model = model(slim);
                assertSame(model.rightSleeve, model.rightArm.getChild("right_sleeve"));
                assertSame(model.leftSleeve, model.leftArm.getChild("left_sleeve"));
                ThrowPoseState pose = chargedPose();
                AvatarRenderState state = state(mainArm);
                for (int phase = 0; phase < 8; phase++) {
                    model.setupAnim(state);
                    var rightLocal = model.rightSleeve.storePose();
                    var leftLocal = model.leftSleeve.storePose();
                    float before = model.getArm(mainArm).xRot;
                    pose.applyThirdPersonPose(state, model);
                    assertEquals(rightLocal, model.rightSleeve.storePose());
                    assertEquals(leftLocal, model.leftSleeve.storePose());
                    if (phase == 0) {
                        assertNotEquals(before, model.getArm(mainArm).xRot);
                        pose.releaseForward(1.0F);
                    }
                    assertTrue(Float.isFinite(model.rightArm.xRot));
                    assertTrue(Float.isFinite(model.leftArm.xRot));
                    pose.tick();
                    state.ageInTicks += 1.0F;
                }
                assertTrue(pose.isIdle());
            }
        }
    }

    @Test
    void respectsHiddenSleevesAndCustomLocalTransforms() {
        PlayerModel model = model(true);
        AvatarRenderState state = state(HumanoidArm.LEFT);
        state.showLeftSleeve = false;
        state.showRightSleeve = false;
        model.setupAnim(state);
        model.rightSleeve.setPos(0.1F, 0.2F, 0.3F);
        model.rightSleeve.setRotation(0.04F, 0.05F, 0.06F);
        model.rightSleeve.xScale = 1.1F;
        model.rightSleeve.skipDraw = true;
        var local = model.rightSleeve.storePose();
        chargedPose().applyThirdPersonPose(state, model);
        assertEquals(local, model.rightSleeve.storePose());
        assertFalse(model.rightSleeve.visible);
        assertFalse(model.leftSleeve.visible);
        assertTrue(model.rightSleeve.skipDraw);
    }

    @Test
    void resetPreventsAccumulationOrLeakingToTheNextPlayer() {
        PlayerModel model = model(false);
        AvatarRenderState state = state(HumanoidArm.RIGHT);
        ThrowPoseState pose = chargedPose();
        model.setupAnim(state);
        var baseline = model.rightArm.storePose();
        pose.applyThirdPersonPose(state, model);
        var charged = model.rightArm.storePose();
        model.setupAnim(state);
        pose.applyThirdPersonPose(state, model);
        assertEquals(charged, model.rightArm.storePose());
        pose.cancel();
        model.setupAnim(state);
        pose.applyThirdPersonPose(state, model);
        assertEquals(baseline, model.rightArm.storePose());
        assertEquals(model(false).rightSleeve.storePose(), model.rightSleeve.storePose());
    }

    @Test
    void ignoresSpectatorsMissingHandAndInvalidAge() {
        PlayerModel model = model(false);
        AvatarRenderState state = state(HumanoidArm.RIGHT);
        var baseline = model.rightArm.storePose();
        ThrowPoseState pose = chargedPose();
        state.isSpectator = true;
        pose.applyThirdPersonPose(state, model);
        assertEquals(baseline, model.rightArm.storePose());
        state.isSpectator = false;
        state.mainArm = null;
        pose.applyThirdPersonPose(state, model);
        assertEquals(baseline, model.rightArm.storePose());
        state.mainArm = HumanoidArm.RIGHT;
        state.ageInTicks = Float.NaN;
        pose.applyThirdPersonPose(state, model);
        assertEquals(baseline, model.rightArm.storePose());
    }

    @Test
    void rejectsNonFiniteProgressBeforeItCanPoisonTheModel() {
        for (float invalid : new float[] {Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY}) {
            ThrowPoseState pose = new ThrowPoseState();
            pose.setChargeProgress(invalid);
            pose.tick();
            assertEquals(0.0F, pose.getChargeIndicatorProgress(1.0F));
            pose.releaseForward(invalid);
            PlayerModel model = model(false);
            pose.applyThirdPersonPose(state(HumanoidArm.RIGHT), model);
            assertTrue(Float.isFinite(model.rightArm.xRot));
            assertTrue(Float.isFinite(model.leftArm.xRot));
        }
    }

    private static PlayerModel model(boolean slim) {
        return new PlayerModel(LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE, slim), 64, 64).bakeRoot(), slim);
    }

    private static AvatarRenderState state(HumanoidArm mainArm) {
        AvatarRenderState state = new AvatarRenderState();
        state.mainArm = mainArm;
        state.ageInTicks = 42.5F;
        state.showLeftSleeve = true;
        state.showRightSleeve = true;
        return state;
    }

    private static ThrowPoseState chargedPose() {
        ThrowPoseState pose = new ThrowPoseState();
        pose.beginCharge();
        pose.setChargeProgress(1.0F);
        for (int tick = 0; tick < 30; tick++) pose.tick();
        return pose;
    }
}
