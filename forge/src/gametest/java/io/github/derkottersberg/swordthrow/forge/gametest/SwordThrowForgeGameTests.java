package io.github.derkottersberg.swordthrow.forge.gametest;

import io.github.derkottersberg.swordthrow.gametest.SwordThrowGameTestScenario;
import java.util.Map;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.gametest.ForgeGameTestHooks;
import net.minecraftforge.gametest.GameTest;
import net.minecraftforge.gametest.GameTestDontPrefix;
import net.minecraftforge.gametest.GameTestNamespace;
import net.minecraftforge.registries.RegisterEvent;

@GameTestNamespace("swordthrow")
@GameTestDontPrefix
public final class SwordThrowForgeGameTests {
    private static final Map<Identifier, ForgeGameTestHooks.TestReference> TESTS =
        ForgeGameTestHooks.gatherTests(SwordThrowForgeGameTests.class, null);

    private SwordThrowForgeGameTests() {
    }

    public static void register(BusGroup modBus) {
        RegisterEvent.getBus(modBus).addListener(SwordThrowForgeGameTests::registerTestFunctions);
    }

    private static void registerTestFunctions(RegisterEvent event) {
        if (event.getRegistryKey() != Registries.TEST_FUNCTION) {
            return;
        }
        for (Map.Entry<Identifier, ForgeGameTestHooks.TestReference> entry : TESTS.entrySet()) {
            event.register(Registries.TEST_FUNCTION, entry.getKey(), entry.getValue()::consumer);
        }
    }

    @GameTest(name = "authoritative_throw", maxTicks = 40)
    public static void authoritativeThrow(GameTestHelper helper) {
        SwordThrowGameTestScenario.validatesRulesCodecsAndAuthoritativeThrow(helper);
    }

    @GameTest(name = "partial_charge_power_and_tap_safety", maxTicks = 50)
    public static void partialChargePowerAndTapSafety(GameTestHelper helper) {
        SwordThrowGameTestScenario.validatesPartialChargePowerAndTapSafety(helper);
    }

    @GameTest(name = "changed_stack_rejected", maxTicks = 40)
    public static void changedStackRejected(GameTestHelper helper) {
        SwordThrowGameTestScenario.rejectsChangedStack(helper);
    }

    @GameTest(name = "missing_and_stale_sessions_rejected", maxTicks = 110)
    public static void missingAndStaleSessionsRejected(GameTestHelper helper) {
        SwordThrowGameTestScenario.rejectsMissingAndStaleSessions(helper);
    }

    @GameTest(name = "duplicate_start_preserves_charge", maxTicks = 40)
    public static void duplicateStartPreservesCharge(GameTestHelper helper) {
        SwordThrowGameTestScenario.duplicateStartDoesNotReset(helper);
    }

    @GameTest(name = "configured_damage_and_tag_precedence", maxTicks = 40)
    public static void configuredDamageAndTagPrecedence(GameTestHelper helper) {
        SwordThrowGameTestScenario.validatesConfiguredImpactDamageAndTagPrecedence(helper);
    }

    @GameTest(name = "embedding_bounce_and_pickup", maxTicks = 40)
    public static void embeddingBounceAndPickup(GameTestHelper helper) {
        SwordThrowGameTestScenario.validatesEmbeddingBounceAndComponentSafePickup(helper);
    }
}
