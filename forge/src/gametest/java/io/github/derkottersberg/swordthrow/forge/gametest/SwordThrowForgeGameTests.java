package io.github.derkottersberg.swordthrow.forge.gametest;

import io.github.derkottersberg.swordthrow.gametest.SwordThrowGameTestScenario;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.GameTestDontPrefix;

@GameTestHolder(value = "swordthrow", namespace = "swordthrow")
@GameTestDontPrefix
public final class SwordThrowForgeGameTests {
    static {
        // A packet-capable in-process player: no client, desktop or network socket.
        SwordThrowGameTestScenario.usePlayerFactory(helper -> {
            var level = helper.getLevel();
            var channel = new io.netty.channel.embedded.EmbeddedChannel();
            var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
            channel.pipeline().addLast("packet_handler", connection);
            channel.pipeline().fireChannelActive();            var player = new net.minecraft.server.level.ServerPlayer(level.getServer(), level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "Throw-QA"), net.minecraft.server.level.ClientInformation.createDefault());
            player.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(), connection, player, net.minecraft.server.network.CommonListenerCookie.createInitial(player.getGameProfile(), false));
            return player;
        });
    }

    @GameTest(batch = "swordthrow", template = "empty", timeoutTicks = 40)
    public static void authoritativeThrow(GameTestHelper helper) {
        SwordThrowGameTestScenario.validatesRulesCodecsAndAuthoritativeThrow(helper);
    }

    @GameTest(batch = "swordthrow", template = "empty", timeoutTicks = 50)
    public static void partialChargePowerAndTapSafety(GameTestHelper helper) {
        SwordThrowGameTestScenario.validatesPartialChargePowerAndTapSafety(helper);
    }

    @GameTest(batch = "swordthrow", template = "empty", timeoutTicks = 40)
    public static void changedStackRejected(GameTestHelper helper) {
        SwordThrowGameTestScenario.rejectsChangedStack(helper);
    }

    @GameTest(batch = "swordthrow", template = "empty", timeoutTicks = 110)
    public static void missingAndStaleSessionsRejected(GameTestHelper helper) {
        SwordThrowGameTestScenario.rejectsMissingAndStaleSessions(helper);
    }

    @GameTest(batch = "swordthrow", template = "empty", timeoutTicks = 40)
    public static void duplicateStartPreservesCharge(GameTestHelper helper) {
        SwordThrowGameTestScenario.duplicateStartDoesNotReset(helper);
    }

    @GameTest(batch = "swordthrow", template = "empty", timeoutTicks = 40)
    public static void configuredDamageAndTagPrecedence(GameTestHelper helper) {
        SwordThrowGameTestScenario.validatesConfiguredImpactDamageAndTagPrecedence(helper);
    }

    @GameTest(batch = "swordthrow", template = "empty", timeoutTicks = 40)
    public static void embeddingBounceAndPickup(GameTestHelper helper) {
        SwordThrowGameTestScenario.validatesEmbeddingBounceAndComponentSafePickup(helper);
    }
}
