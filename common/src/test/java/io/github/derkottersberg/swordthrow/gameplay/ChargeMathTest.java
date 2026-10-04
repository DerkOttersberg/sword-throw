package io.github.derkottersberg.swordthrow.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ChargeMathTest {
    @Test
    void acceptsPartialHoldsButNotDropTapsOrForgedTime() {
        assertFalse(ChargeMath.canRelease(-1));
        assertFalse(ChargeMath.canRelease(0));
        assertFalse(ChargeMath.canRelease(1));
        assertTrue(ChargeMath.canRelease(2));
        assertTrue(ChargeMath.canRelease(5));
        assertTrue(ChargeMath.canRelease(15));
        assertTrue(ChargeMath.canRelease(30));
        assertFalse(ChargeMath.canRelease(ChargeMath.effectiveCharge(1, 30)));
        assertFalse(ChargeMath.canRelease(ChargeMath.effectiveCharge(30, 1)));
    }

    @Test
    void partialPowerScalesContinuouslyUpToFullCharge() {
        assertEquals(1.18F, ChargeMath.speed(2), 0.0001F);
        assertEquals(1.30F, ChargeMath.speed(5), 0.0001F);
        assertEquals(1.70F, ChargeMath.speed(15), 0.0001F);
        assertEquals(1.0F, ChargeMath.progress(30));
        for (int ticks = ChargeMath.MIN_CHARGE_TICKS; ticks < ChargeMath.MAX_CHARGE_TICKS; ticks++) {
            assertTrue(ChargeMath.speed(ticks + 1) > ChargeMath.speed(ticks));
            assertTrue(ChargeMath.cooldown(ticks + 1) >= ChargeMath.cooldown(ticks));
        }
    }

    @Test
    void neverTrustsChargeBeyondEitherClock() {
        assertEquals(12, ChargeMath.effectiveCharge(12, 30));
        assertEquals(9, ChargeMath.effectiveCharge(30, 9));
        assertEquals(30, ChargeMath.effectiveCharge(200, 200));
        assertEquals(0, ChargeMath.effectiveCharge(-5, 20));
    }

    @Test
    void preservesOriginalThrowTuningAtChargeEndpoints() {
        assertEquals(1.10F, ChargeMath.speed(0), 0.0001F);
        assertEquals(2.30F, ChargeMath.speed(30), 0.0001F);
        assertEquals(10, ChargeMath.cooldown(0));
        assertEquals(18, ChargeMath.cooldown(30));
    }

    @Test
    void rejectsMissingOrStaleSessions() {
        assertFalse(ChargeMath.isSessionFresh(-1));
        assertTrue(ChargeMath.isSessionFresh(ChargeMath.MAX_SESSION_AGE_TICKS));
        assertFalse(ChargeMath.isSessionFresh(ChargeMath.MAX_SESSION_AGE_TICKS + 1L));
    }

    @Test
    void exposesTheSameBoundedChargeForPoseSynchronization() {
        assertEquals(0, ChargeMath.clampCharge(-1));
        assertEquals(14, ChargeMath.clampCharge(14));
        assertEquals(ChargeMath.MAX_CHARGE_TICKS, ChargeMath.clampCharge(999));
    }
}
