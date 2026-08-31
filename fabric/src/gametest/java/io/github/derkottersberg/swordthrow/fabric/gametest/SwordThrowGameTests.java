package io.github.derkottersberg.swordthrow.fabric.gametest;

import io.github.derkottersberg.swordthrow.SwordThrow;
import io.github.derkottersberg.swordthrow.entity.ModEntities;
import io.github.derkottersberg.swordthrow.entity.ThrownSwordEntity;
import io.github.derkottersberg.swordthrow.gameplay.ChargeMath;
import io.github.derkottersberg.swordthrow.network.ThrowActionPayload;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@SuppressWarnings("removal")
public final class SwordThrowGameTests {
    @GameTest(maxTicks = 40)
    public void validatesChargeRegistersAndSpawnsProjectile(GameTestHelper helper) {
        helper.assertValueEqual(
            BuiltInRegistries.ENTITY_TYPE.getKey(ModEntities.thrownSword()),
            SwordThrow.id("thrown_sword"),
            "The preserved thrown-sword entity ID changed"
        );

        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
            Unpooled.buffer(),
            helper.getLevel().registryAccess()
        );
        try {
            ThrowActionPayload expected = ThrowActionPayload.release(30);
            ThrowActionPayload.STREAM_CODEC.encode(buffer, expected);
            helper.assertValueEqual(
                ThrowActionPayload.STREAM_CODEC.decode(buffer),
                expected,
                "The throw-action payload codec changed its contents"
            );
        } finally {
            buffer.release();
        }

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
        SwordThrow.handleThrowAction(player, ThrowActionPayload.start());

        helper.runAfterDelay(16L, () -> {
            // The client exaggerates a full charge; the server should cap it to elapsed time.
            SwordThrow.handleThrowAction(player, ThrowActionPayload.release(30));
            ThrownSwordEntity projectile = null;
            for (Entity entity : helper.getLevel().getAllEntities()) {
                if (entity instanceof ThrownSwordEntity thrownSword) {
                    projectile = thrownSword;
                    break;
                }
            }

            helper.assertTrue(projectile != null, "A valid authoritative charge did not spawn a projectile");
            helper.assertTrue(player.getMainHandItem().isEmpty(), "The spawned projectile did not transfer the held stack");
            helper.assertTrue(
                projectile.getDeltaMovement().length() <= ChargeMath.speed(16) + 0.01F,
                "The server accepted the client's exaggerated charge speed"
            );
            helper.succeed();
        });
    }
}
