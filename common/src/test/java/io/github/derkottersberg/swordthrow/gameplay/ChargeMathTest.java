package io.github.derkottersberg.swordthrow.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ChargeMathTest {
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
}
