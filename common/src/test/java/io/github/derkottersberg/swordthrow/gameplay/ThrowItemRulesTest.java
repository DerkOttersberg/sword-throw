package io.github.derkottersberg.swordthrow.gameplay;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ThrowItemRulesTest {
    @Test
    void cannotThrowAlwaysWinsOverThrowable() {
        assertFalse(ThrowItemRules.decideThrowable(true, true, true, false));
        assertFalse(ThrowItemRules.decideThrowable(true, true, true, true));
    }

    @Test
    void throwableCanOptVanillaTridentsIntoSwordThrow() {
        assertFalse(ThrowItemRules.decideThrowable(true, false, false, true));
        assertTrue(ThrowItemRules.decideThrowable(true, false, true, true));
    }

    @Test
    void broadDefaultStillAllowsOrdinaryUntaggedItems() {
        assertTrue(ThrowItemRules.decideThrowable(true, false, false, false));
        assertFalse(ThrowItemRules.decideThrowable(false, false, true, false));
    }
}
