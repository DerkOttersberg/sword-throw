package io.github.derkottersberg.swordthrow.neoforge.gametest;

import io.github.derkottersberg.swordthrow.gametest.SwordThrowGameTestScenario;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class SwordThrowNeoForgeGameTests {
    private static final String MOD_ID = "swordthrow";
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
        DeferredRegister.create(Registries.TEST_FUNCTION, MOD_ID);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> AUTHORITATIVE_THROW =
        TEST_FUNCTIONS.register(
            "authoritative_throw",
            () -> SwordThrowGameTestScenario::validatesRulesCodecsAndAuthoritativeThrow
        );
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CHANGED_STACK_REJECTED =
        TEST_FUNCTIONS.register(
            "changed_stack_rejected",
            () -> SwordThrowGameTestScenario::rejectsChangedStack
        );
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DUPLICATE_START =
        TEST_FUNCTIONS.register(
            "duplicate_start_preserves_charge",
            () -> SwordThrowGameTestScenario::duplicateStartDoesNotReset
        );
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CONFIGURED_DAMAGE_AND_TAGS =
        TEST_FUNCTIONS.register(
            "configured_damage_and_tag_precedence",
            () -> SwordThrowGameTestScenario::validatesConfiguredImpactDamageAndTagPrecedence
        );
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> EMBEDDING_BOUNCE_AND_PICKUP =
        TEST_FUNCTIONS.register(
            "embedding_bounce_and_pickup",
            () -> SwordThrowGameTestScenario::validatesEmbeddingBounceAndComponentSafePickup
        );

    private SwordThrowNeoForgeGameTests() {
    }

    public static void register(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
        modEventBus.addListener(SwordThrowNeoForgeGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
            Identifier.fromNamespaceAndPath(MOD_ID, "default_environment"),
            new TestEnvironmentDefinition.AllOf()
        );
        registerTest(event, environment, "authoritative_throw", AUTHORITATIVE_THROW, 40);
        registerTest(event, environment, "changed_stack_rejected", CHANGED_STACK_REJECTED, 40);
        registerTest(event, environment, "duplicate_start_preserves_charge", DUPLICATE_START, 40);
        registerTest(event, environment, "configured_damage_and_tag_precedence", CONFIGURED_DAMAGE_AND_TAGS, 40);
        registerTest(event, environment, "embedding_bounce_and_pickup", EMBEDDING_BOUNCE_AND_PICKUP, 40);
    }

    private static void registerTest(
        RegisterGameTestsEvent event,
        Holder<TestEnvironmentDefinition<?>> environment,
        String name,
        DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> function,
        int maxTicks
    ) {
        TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
            environment,
            Identifier.withDefaultNamespace("empty"),
            maxTicks,
            0,
            true
        );
        event.registerTest(
            Identifier.fromNamespaceAndPath(MOD_ID, name),
            new FunctionGameTestInstance(function.getKey(), data)
        );
    }
}
