package io.github.derkottersberg.swordthrow.neoforge.gametest;

import io.github.derkottersberg.swordthrow.gametest.SwordThrowGameTestScenario;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("swordthrow")
@PrefixGameTestTemplate(false)
public final class SwordThrowNeoForgeGameTests {
    public static void register(IEventBus bus) {
        bus.addListener((RegisterGameTestsEvent event) -> event.register(SwordThrowNeoForgeGameTests.class));
    }
    @GameTest(template = "empty",timeoutTicks=40)
    public static void authoritativeThrow(GameTestHelper helper) {
        SwordThrowGameTestScenario.validatesRulesCodecsAndAuthoritativeThrow(helper);
    }
    @GameTest(template = "empty",timeoutTicks=50)
    public static void partialChargePowerAndTapSafety(GameTestHelper helper) {
        SwordThrowGameTestScenario.validatesPartialChargePowerAndTapSafety(helper);
    }
    @GameTest(template = "empty",timeoutTicks=40)
    public static void changedStackRejected(GameTestHelper helper) {
        SwordThrowGameTestScenario.rejectsChangedStack(helper);
    }
    @GameTest(template = "empty",timeoutTicks=110)
    public static void missingAndStaleSessionsRejected(GameTestHelper helper) {
        SwordThrowGameTestScenario.rejectsMissingAndStaleSessions(helper);
    }
    @GameTest(template = "empty",timeoutTicks=40)
    public static void duplicateStartPreservesCharge(GameTestHelper helper) {
        SwordThrowGameTestScenario.duplicateStartDoesNotReset(helper);
    }
    @GameTest(template = "empty",timeoutTicks=40)
    public static void configuredDamageAndTagPrecedence(GameTestHelper helper) {
        SwordThrowGameTestScenario.validatesConfiguredImpactDamageAndTagPrecedence(helper);
    }
    @GameTest(template = "empty",timeoutTicks=40)
    public static void embeddingBounceAndPickup(GameTestHelper helper) {
        SwordThrowGameTestScenario.validatesEmbeddingBounceAndComponentSafePickup(helper);
    }
}
