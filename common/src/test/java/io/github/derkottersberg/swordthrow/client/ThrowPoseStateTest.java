package io.github.derkottersberg.swordthrow.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ThrowPoseStateTest {
    @Test
    void playerPoseInstancesDoNotLeakIntoEachOther() {
        ThrowPoseState first = new ThrowPoseState();
        ThrowPoseState second = new ThrowPoseState();
        first.beginCharge();
        second.beginCharge();
        first.setChargeProgress(1.0F);
        second.setChargeProgress(0.2F);

        for (int tick = 0; tick < 8; tick++) {
            first.tick();
            second.tick();
        }

        assertTrue(first.getChargeIndicatorProgress(1.0F) > second.getChargeIndicatorProgress(1.0F));
        first.cancel();
        assertTrue(first.isIdle());
        assertFalse(second.isIdle());
    }

    @Test
    void authoritativeReleaseAnimatesThenExpires() {
        ThrowPoseState state = new ThrowPoseState();
        state.releaseForward(1.0F);
        assertTrue(state.isOffHandVisible());

        for (int tick = 0; tick < 6; tick++) {
            state.tick();
        }

        assertTrue(state.isIdle());
    }
}
