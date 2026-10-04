package io.github.derkottersberg.swordthrow.fabric.gametest;

import io.github.derkottersberg.swordthrow.gametest.SwordThrowGameTestScenario;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

@SuppressWarnings("removal")
public final class SwordThrowGameTests {
    @GameTest(maxTicks = 50)
    public void partialChargePowerAndTapSafety(GameTestHelper helper) {
        SwordThrowGameTestScenario.validatesPartialChargePowerAndTapSafety(helper);
    }

    @GameTest(maxTicks = 40)
    public void validatesChargeRegistersAndSpawnsProjectile(GameTestHelper helper) {
        SwordThrowGameTestScenario.validatesRulesCodecsAndAuthoritativeThrow(helper);
    }

    @GameTest(maxTicks = 50)
    public void duplicateStartDoesNotResetAuthoritativeCharge(GameTestHelper helper) {
        SwordThrowGameTestScenario.duplicateStartDoesNotReset(helper);
    }

    @GameTest(maxTicks = 40)
    public void changedHeldStackCancelsWithoutConsumingItems(GameTestHelper helper) {
        SwordThrowGameTestScenario.rejectsChangedStack(helper);
    }

    @GameTest(maxTicks = 110)
    public void missingAndStaleSessionsAreRejectedWithoutConsumingItems(GameTestHelper helper) {
        SwordThrowGameTestScenario.rejectsMissingAndStaleSessions(helper);
    }

    @GameTest(maxTicks = 40)
    public void configuredDamageAndTagPrecedence(GameTestHelper helper) {
        SwordThrowGameTestScenario.validatesConfiguredImpactDamageAndTagPrecedence(helper);
    }

    @GameTest(maxTicks = 40)
    public void embeddingBounceAndComponentSafePickup(GameTestHelper helper) {
        SwordThrowGameTestScenario.validatesEmbeddingBounceAndComponentSafePickup(helper);
    }
}
