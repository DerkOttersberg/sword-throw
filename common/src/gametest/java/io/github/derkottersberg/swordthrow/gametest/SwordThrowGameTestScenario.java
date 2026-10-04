package io.github.derkottersberg.swordthrow.gametest;

import io.github.derkottersberg.swordthrow.SwordThrow;
import io.github.derkottersberg.swordthrow.config.SwordThrowGameTestConfigAccess;
import io.github.derkottersberg.swordthrow.config.SwordThrowServerConfig;
import io.github.derkottersberg.swordthrow.entity.ModEntities;
import io.github.derkottersberg.swordthrow.entity.ThrownSwordEntity;
import io.github.derkottersberg.swordthrow.gameplay.ChargeMath;
import io.github.derkottersberg.swordthrow.gameplay.SwordThrowItemTags;
import io.github.derkottersberg.swordthrow.gameplay.ThrowItemRules;
import io.github.derkottersberg.swordthrow.internal.PlatformServices;
import io.github.derkottersberg.swordthrow.network.ThrowActionPayload;
import io.github.derkottersberg.swordthrow.network.ThrowStatePayload;
import io.netty.buffer.Unpooled;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/** Loader-neutral live scenarios wrapped by Fabric, Forge, and NeoForge test registration. */
public final class SwordThrowGameTestScenario {
    private SwordThrowGameTestScenario() {
    }

    public static void validatesRulesCodecsAndAuthoritativeThrow(GameTestHelper helper) {
        helper.assertValueEqual(
            BuiltInRegistries.ENTITY_TYPE.getKey(ModEntities.thrownSword()),
            SwordThrow.id("thrown_sword"),
            "The preserved thrown-sword entity ID changed"
        );
        helper.assertTrue(SwordThrow.canThrow(new ItemStack(Items.FEATHER)), "Untagged items stopped being throwable");
        helper.assertTrue(ThrowItemRules.isSpear(new ItemStack(Items.TRIDENT)), "The default spear tag is missing trident");
        helper.assertTrue(ThrowItemRules.canEmbed(new ItemStack(Items.DIAMOND_SWORD)), "The embeddable tag is missing swords");

        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
            Unpooled.buffer(),
            helper.getLevel().registryAccess()
        );
        try {
            ThrowActionPayload action = ThrowActionPayload.release(30);
            ThrowActionPayload.STREAM_CODEC.encode(buffer, action);
            helper.assertValueEqual(
                ThrowActionPayload.STREAM_CODEC.decode(buffer),
                action,
                "The throw-action payload codec changed contents"
            );
            ThrowStatePayload state = ThrowStatePayload.release(42, 27);
            ThrowStatePayload.STREAM_CODEC.encode(buffer, state);
            helper.assertValueEqual(
                ThrowStatePayload.STREAM_CODEC.decode(buffer),
                state,
                "The throw-state payload codec changed contents"
            );
        } finally {
            buffer.release();
        }

        ServerPlayer player = makePlayerInTest(helper);
        ItemStack originalStack = new ItemStack(Items.DIAMOND_SWORD, 3);
        originalStack.set(DataComponents.CUSTOM_NAME, Component.literal("Conserved throw"));
        originalStack.enchant(
            helper.getLevel()
                .registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.UNBREAKING),
            2
        );
        player.setItemInHand(InteractionHand.MAIN_HAND, originalStack.copy());
        SwordThrow.handleThrowAction(player, ThrowActionPayload.start());

        helper.runAfterDelay(16L, () -> {
            SwordThrow.handleThrowAction(player, ThrowActionPayload.release(30));
            ThrownSwordEntity projectile = findProjectile(helper, player);
            helper.assertTrue(projectile != null, "A valid authoritative charge did not spawn a projectile");
            helper.assertTrue(player.getMainHandItem().isEmpty(), "The spawned projectile did not receive the held stack");
            helper.assertValueEqual(
                BuiltInRegistries.ITEM.getKey(projectile.getItem().getItem()),
                BuiltInRegistries.ITEM.getKey(originalStack.getItem()),
                "The projectile changed the thrown item's registry ID"
            );
            helper.assertTrue(
                ItemStack.isSameItemSameComponents(projectile.getItem(), originalStack),
                "The projectile did not preserve all item components and enchantments"
            );
            helper.assertValueEqual(
                projectile.getThrownStackCount(),
                originalStack.getCount(),
                "The projectile did not preserve the represented stack count"
            );
            helper.assertTrue(
                projectile.getDeltaMovement().length() <= ChargeMath.speed(20) + 0.01F,
                "The server trusted the client's full-charge claim after only a short server charge"
            );

            ThrownSwordEntity reloaded = saveAndReload(helper, projectile);
            helper.assertTrue(reloaded != null, "The thrown entity did not reload from its preserved registry ID");
            helper.assertTrue(
                ItemStack.isSameItemSameComponents(reloaded.getItem(), originalStack),
                "Save/reload changed the thrown item's components or enchantments"
            );
            helper.assertValueEqual(
                reloaded.getThrownStackCount(),
                originalStack.getCount(),
                "Save/reload changed the represented stack count"
            );
            helper.succeed();
        });
    }

    public static void rejectsChangedStack(GameTestHelper helper) {
        ServerPlayer player = makePlayerInTest(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        SwordThrow.handleThrowAction(player, ThrowActionPayload.start());

        helper.runAfterDelay(16L, () -> {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
            SwordThrow.handleThrowAction(player, ThrowActionPayload.release(30));
            helper.assertTrue(findProjectile(helper, player) == null, "A changed held stack was accepted");
            helper.assertValueEqual(
                player.getMainHandItem().getItem(),
                Items.DIAMOND_SWORD,
                "A rejected throw consumed the replacement stack"
            );
            helper.succeed();
        });
    }

    public static void validatesPartialChargePowerAndTapSafety(GameTestHelper helper) {
        ServerPlayer tap = makePlayerInTest(helper, 1);
        ServerPlayer minimum = makePlayerInTest(helper, 1);
        ServerPlayer partial = makePlayerInTest(helper, 2);
        ServerPlayer full = makePlayerInTest(helper, 2);
        for (ServerPlayer player : List.of(tap, minimum, partial, full)) {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
            SwordThrow.handleThrowAction(player, ThrowActionPayload.start());
        }
        double[] partialSpeed = new double[1];
        helper.runAfterDelay(1L, () -> {
            SwordThrow.handleThrowAction(tap, ThrowActionPayload.release(30));
            helper.assertTrue(findProjectile(helper, tap) == null, "A forged full-power tap spawned a projectile");
            helper.assertTrue(tap.getMainHandItem().is(Items.IRON_SWORD), "A rejected tap consumed its item");
        });
        helper.runAfterDelay(2L, () -> {
            SwordThrow.handleThrowAction(minimum, ThrowActionPayload.release(2));
            ThrownSwordEntity projectile = findProjectile(helper, minimum);
            helper.assertTrue(projectile != null, "The minimum deliberate hold did not throw");
            helper.assertTrue(minimum.getMainHandItem().isEmpty(), "The minimum throw duplicated its held item");
            helper.assertTrue(Math.abs(projectile.getDeltaMovement().length() - ChargeMath.speed(2)) < 0.05D,
                "The minimum throw used the wrong power");
        });
        helper.runAfterDelay(5L, () -> {
            SwordThrow.handleThrowAction(partial, ThrowActionPayload.release(5));
            ThrownSwordEntity projectile = findProjectile(helper, partial);
            helper.assertTrue(projectile != null, "A short partial hold did not throw");
            helper.assertTrue(partial.getMainHandItem().isEmpty(), "The partial throw duplicated its held item");
            partialSpeed[0] = projectile.getDeltaMovement().length();
            helper.assertTrue(Math.abs(partialSpeed[0] - ChargeMath.speed(5)) < 0.05D,
                "The partial throw used full-power or incorrect velocity");
        });
        helper.runAfterDelay(30L, () -> {
            SwordThrow.handleThrowAction(full, ThrowActionPayload.release(30));
            ThrownSwordEntity projectile = findProjectile(helper, full);
            helper.assertTrue(projectile != null, "A full hold did not throw");
            helper.assertTrue(full.getMainHandItem().isEmpty(), "The full throw duplicated its held item");
            double fullSpeed = projectile.getDeltaMovement().length();
            helper.assertTrue(Math.abs(fullSpeed - ChargeMath.speed(30)) < 0.05D, "Full-power tuning changed");
            helper.assertTrue(fullSpeed > partialSpeed[0] + 0.8D, "A longer hold did not increase throw power");
            helper.succeed();
        });
    }

    public static void duplicateStartDoesNotReset(GameTestHelper helper) {
        ServerPlayer player = makePlayerInTest(helper, 1);
        ServerPlayer lateObserver = makePlayerInTest(helper, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GOLDEN_SWORD));
        int startedAtServerTick = helper.getLevel().getServer().getTickCount();
        SwordThrow.handleThrowAction(player, ThrowActionPayload.start());

        helper.runAfterDelay(10L, () -> {
            SwordThrow.handleThrowAction(player, ThrowActionPayload.start());
            int expectedChargeTicks = ChargeMath.clampCharge(
                (long)helper.getLevel().getServer().getTickCount() - startedAtServerTick
            );
            DeliveryCapture capture = captureDirectDelivery(() ->
                SwordThrow.syncActiveChargeTo(lateObserver, player));
            helper.assertTrue(capture.accepted(), "The active charge was rejected for a newly tracking player");
            helper.assertValueEqual(
                capture.deliveries().size(),
                1,
                "Late tracking did not attempt exactly one direct payload delivery"
            );
            DirectDelivery delivery = capture.deliveries().getFirst();
            helper.assertTrue(
                delivery.target() == lateObserver,
                "Late tracking delivered the charge snapshot to the wrong observer"
            );
            helper.assertTrue(
                delivery.payload() instanceof ThrowStatePayload,
                "Late tracking attempted to deliver the wrong payload type"
            );
            ThrowStatePayload state = (ThrowStatePayload)delivery.payload();
            helper.assertValueEqual(state.playerEntityId(), player.getId(), "The late snapshot named the wrong player");
            helper.assertValueEqual(
                state.phase(),
                ThrowStatePayload.Phase.CHARGING,
                "The late snapshot did not report an active charge"
            );
            helper.assertValueEqual(
                state.chargeTicks(),
                expectedChargeTicks,
                "The late snapshot did not contain the authoritative server charge time"
            );
        });
        helper.runAfterDelay(16L, () -> {
            SwordThrow.handleThrowAction(player, ThrowActionPayload.release(16));
            helper.assertTrue(
                player.getMainHandItem().isEmpty(),
                "A duplicate START reset the valid authoritative charge"
            );
            helper.assertTrue(findProjectile(helper, player) != null, "The duplicate-start player did not throw");
            helper.succeed();
        });
    }

    public static void rejectsMissingAndStaleSessions(GameTestHelper helper) {
        ServerPlayer stalePlayer = makePlayerInTest(helper, 1);
        ItemStack staleStack = new ItemStack(Items.IRON_AXE);
        staleStack.set(DataComponents.CUSTOM_NAME, Component.literal("Stale session must conserve me"));
        stalePlayer.setItemInHand(InteractionHand.MAIN_HAND, staleStack.copy());
        SwordThrow.handleThrowAction(stalePlayer, ThrowActionPayload.start());

        helper.runAfterDelay(ChargeMath.MAX_SESSION_AGE_TICKS + 2L, () -> {
            SwordThrow.handleThrowAction(stalePlayer, ThrowActionPayload.release(ChargeMath.MAX_CHARGE_TICKS));
            helper.assertTrue(findProjectile(helper, stalePlayer) == null, "A stale charge spawned a projectile");
            helper.assertTrue(
                ItemStack.matches(stalePlayer.getMainHandItem(), staleStack),
                "Rejecting a stale charge changed or consumed the held stack"
            );

            ServerPlayer missingPlayer = makePlayerInTest(helper, 2);
            ItemStack missingStack = new ItemStack(Items.DIAMOND_PICKAXE);
            missingStack.set(DataComponents.CUSTOM_NAME, Component.literal("Missing session must conserve me"));
            missingPlayer.setItemInHand(InteractionHand.MAIN_HAND, missingStack.copy());
            SwordThrow.handleThrowAction(missingPlayer, ThrowActionPayload.release(ChargeMath.MAX_CHARGE_TICKS));
            helper.assertTrue(
                findProjectile(helper, missingPlayer) == null,
                "A RELEASE without a server charge session spawned a projectile"
            );
            helper.assertTrue(
                ItemStack.matches(missingPlayer.getMainHandItem(), missingStack),
                "Rejecting a RELEASE without START changed or consumed the held stack"
            );
            helper.succeed();
        });
    }

    public static void validatesConfiguredImpactDamageAndTagPrecedence(GameTestHelper helper) {
        ItemStack nativeSpear = new ItemStack(Items.DIAMOND_SPEAR);
        helper.assertTrue(nativeSpear.is(ItemTags.SPEARS), "The vanilla 26.2 spear tag is missing diamond spears");
        helper.assertTrue(
            nativeSpear.is(SwordThrowItemTags.SPEARS) && ThrowItemRules.isSpear(nativeSpear),
            "swordthrow:spears did not inherit the native Minecraft spear tag"
        );
        helper.assertTrue(SwordThrow.canThrow(nativeSpear), "A native 26.2 spear is not throwable");
        helper.assertTrue(ThrowItemRules.canEmbed(nativeSpear), "A native 26.2 spear is not embeddable");

        ItemStack explicitlyAllowedTrident = new ItemStack(Items.TRIDENT);
        helper.assertTrue(
            explicitlyAllowedTrident.is(SwordThrowItemTags.THROWABLE),
            "The GameTest throwable override did not load"
        );
        helper.assertTrue(
            SwordThrow.canThrow(explicitlyAllowedTrident),
            "The throwable tag did not opt a vanilla trident into Sword Throw"
        );

        ItemStack explicitlyBlockedStick = new ItemStack(Items.STICK);
        helper.assertTrue(
            explicitlyBlockedStick.is(SwordThrowItemTags.THROWABLE)
                && explicitlyBlockedStick.is(SwordThrowItemTags.CANNOT_THROW),
            "The GameTest overlapping throwable/cannot_throw tags did not load"
        );
        helper.assertTrue(
            !SwordThrow.canThrow(explicitlyBlockedStick),
            "cannot_throw did not take precedence over throwable"
        );

        ItemStack untaggedItem = new ItemStack(Items.FEATHER);
        helper.assertTrue(
            !untaggedItem.is(SwordThrowItemTags.THROWABLE)
                && !untaggedItem.is(SwordThrowItemTags.CANNOT_THROW)
                && SwordThrow.canThrow(untaggedItem),
            "The broad untagged-item default changed"
        );

        ServerPlayer owner = makePlayerInTest(helper, 1);
        LivingEntity target = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(2, 2, 1));
        ItemStack thrownStack = new ItemStack(Items.FEATHER);
        float configuredStandardMultiplier = 4.0F;
        float configuredSpearMultiplier = 0.25F;
        float impactSpeed = 2.0F;
        SwordThrowGameTestConfigAccess.replace(
            new SwordThrowServerConfig.ConfigData(configuredStandardMultiplier, configuredSpearMultiplier)
        );
        try {
            TestThrownSwordEntity projectile = new TestThrownSwordEntity(
                helper.getLevel(),
                owner,
                thrownStack
            );
            projectile.setPos(owner.position());
            projectile.setDeltaMovement(new Vec3(impactSpeed, 0.0D, 0.0D));
            helper.assertTrue(helper.getLevel().addFreshEntity(projectile), "Could not add the impact test projectile");

            float healthBefore = target.getHealth();
            projectile.hitTarget(target);
            float actualDamage = healthBefore - target.getHealth();
            float expectedDamage = 0.6F
                * (0.65F + impactSpeed * 0.7F)
                * 1.35F
                * configuredStandardMultiplier;
            helper.assertTrue(
                Math.abs(actualDamage - expectedDamage) < 0.01F,
                "Configured impact damage was " + actualDamage + "; expected " + expectedDamage
            );
            projectile.discard();
            target.discard();

            LivingEntity spearTarget = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(2, 2, 1));
            TestThrownSwordEntity spearProjectile = new TestThrownSwordEntity(
                helper.getLevel(),
                owner,
                nativeSpear
            );
            spearProjectile.setPos(owner.position());
            spearProjectile.setDeltaMovement(new Vec3(impactSpeed, 0.0D, 0.0D));
            helper.assertTrue(
                helper.getLevel().addFreshEntity(spearProjectile),
                "Could not add the native spear impact test projectile"
            );

            float spearHealthBefore = spearTarget.getHealth();
            spearProjectile.hitTarget(spearTarget);
            float actualSpearDamage = spearHealthBefore - spearTarget.getHealth();
            float spearBaseline = 4.0F;
            float expectedSpearDamage = Math.max(spearBaseline * 1.45F, spearBaseline + 2.5F)
                * (0.65F + impactSpeed * 0.7F)
                * 1.35F
                * configuredSpearMultiplier;
            helper.assertTrue(
                Math.abs(actualSpearDamage - expectedSpearDamage) < 0.01F,
                "Native spear impact damage was " + actualSpearDamage + "; expected " + expectedSpearDamage
            );
        } finally {
            SwordThrowGameTestConfigAccess.reset();
        }
        helper.succeed();
    }

    public static void validatesEmbeddingBounceAndComponentSafePickup(GameTestHelper helper) {
        BlockPos wallRelative = new BlockPos(2, 2, 1);
        helper.setBlock(wallRelative, Blocks.STONE);
        BlockPos wall = helper.absolutePos(wallRelative);
        Vec3 westFace = new Vec3(wall.getX(), wall.getY() + 0.5D, wall.getZ() + 0.5D);
        BlockHitResult headOnHit = new BlockHitResult(westFace, Direction.WEST, wall, false);
        ServerPlayer picker = makePlayerInTest(helper, 1);

        ItemStack embeddedStack = new ItemStack(Items.TRIDENT);
        embeddedStack.set(DataComponents.CUSTOM_NAME, Component.literal("Component-safe pickup"));
        embeddedStack.enchant(
            helper.getLevel()
                .registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.UNBREAKING),
            3
        );
        TestThrownSwordEntity embedded = new TestThrownSwordEntity(helper.getLevel(), picker, embeddedStack);
        embedded.setPos(westFace.add(-1.0D, 0.0D, 0.0D));
        embedded.setDeltaMovement(new Vec3(1.0D, 0.0D, 0.0D));
        helper.assertTrue(helper.getLevel().addFreshEntity(embedded), "Could not add the embedding test projectile");
        embedded.hitBlock(headOnHit);
        helper.assertTrue(embedded.isEmbedded(), "An embeddable spear bounced instead of embedding");
        helper.assertTrue(
            ItemStack.isSameItemSameComponents(embedded.getItem(), embeddedStack),
            "Embedding changed the projectile's item components"
        );

        embedded.playerTouch(picker);
        ItemStack recovered = picker.getInventory()
            .getNonEquipmentItems()
            .stream()
            .filter(stack -> ItemStack.isSameItemSameComponents(stack, embeddedStack))
            .findFirst()
            .orElse(ItemStack.EMPTY);
        helper.assertTrue(!recovered.isEmpty(), "The player could not pick up the embedded projectile");
        helper.assertValueEqual(recovered.getCount(), embeddedStack.getCount(), "Pickup changed the stack count");
        helper.assertTrue(embedded.isRemoved(), "A fully picked-up projectile remained in the level");

        ItemStack bouncingStack = new ItemStack(Items.FEATHER);
        bouncingStack.set(DataComponents.CUSTOM_NAME, Component.literal("Component-safe bounce"));
        TestThrownSwordEntity bouncing = new TestThrownSwordEntity(helper.getLevel(), picker, bouncingStack);
        bouncing.setPos(westFace.add(-1.0D, 0.0D, 0.0D));
        bouncing.setDeltaMovement(new Vec3(1.0D, 0.0D, 0.0D));
        helper.assertTrue(helper.getLevel().addFreshEntity(bouncing), "Could not add the bounce test projectile");
        bouncing.hitBlock(headOnHit);
        helper.assertTrue(!bouncing.isEmbedded(), "A non-embeddable item embedded in a block");
        helper.assertTrue(!bouncing.isRemoved(), "A viable bounce discarded the projectile");
        helper.assertTrue(bouncing.getDeltaMovement().x < 0.0D, "The block impact did not reflect the projectile");
        helper.assertTrue(
            ItemStack.isSameItemSameComponents(bouncing.getItem(), bouncingStack),
            "Bouncing changed the projectile's item components"
        );
        bouncing.discard();
        helper.succeed();
    }

    private static ServerPlayer makePlayerInTest(GameTestHelper helper) {
        return makePlayerInTest(helper, 1);
    }

    private static ServerPlayer makePlayerInTest(GameTestHelper helper, int localX) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.getAbilities().instabuild = false;
        BlockPos position = helper.absolutePos(new BlockPos(localX, 2, 1));
        player.setPos(position.getX() + 0.5D, position.getY(), position.getZ() + 0.5D);
        return player;
    }

    private static ThrownSwordEntity findProjectile(GameTestHelper helper, ServerPlayer owner) {
        for (Entity entity : helper.getLevel().getAllEntities()) {
            if (entity instanceof ThrownSwordEntity thrownSword && thrownSword.getOwner() == owner) {
                return thrownSword;
            }
        }
        return null;
    }

    private static ThrownSwordEntity saveAndReload(GameTestHelper helper, ThrownSwordEntity projectile) {
        TagValueOutput output = TagValueOutput.createWithContext(
            ProblemReporter.DISCARDING,
            helper.getLevel().registryAccess()
        );
        projectile.save(output);
        CompoundTag saved = output.buildResult();
        Entity reloaded = EntityType.loadEntityRecursive(
            ModEntities.thrownSword(),
            saved,
            helper.getLevel(),
            EntitySpawnReason.LOAD,
            entity -> entity
        );
        return reloaded instanceof ThrownSwordEntity thrownSword ? thrownSword : null;
    }

    private static DeliveryCapture captureDirectDelivery(BooleanSupplier action) {
        try {
            Field servicesField = SwordThrow.class.getDeclaredField("platformServices");
            servicesField.setAccessible(true);
            PlatformServices delegate = (PlatformServices)servicesField.get(null);
            List<DirectDelivery> deliveries = new ArrayList<>();
            PlatformServices recordingServices = (PlatformServices)Proxy.newProxyInstance(
                PlatformServices.class.getClassLoader(),
                new Class<?>[] {PlatformServices.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("sendToPlayer")
                        && arguments != null
                        && arguments.length == 2
                        && arguments[0] instanceof ServerPlayer target
                        && arguments[1] instanceof CustomPacketPayload payload) {
                        deliveries.add(new DirectDelivery(target, payload));
                    }
                    try {
                        return method.invoke(delegate, arguments);
                    } catch (InvocationTargetException exception) {
                        throw exception.getCause();
                    }
                }
            );

            servicesField.set(null, recordingServices);
            boolean accepted;
            try {
                accepted = action.getAsBoolean();
            } finally {
                servicesField.set(null, delegate);
            }
            return new DeliveryCapture(accepted, List.copyOf(deliveries));
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not install the GameTest network-delivery probe", exception);
        }
    }

    private record DirectDelivery(ServerPlayer target, CustomPacketPayload payload) {
    }

    private record DeliveryCapture(boolean accepted, List<DirectDelivery> deliveries) {
    }

    private static final class TestThrownSwordEntity extends ThrownSwordEntity {
        private TestThrownSwordEntity(ServerLevel level, LivingEntity owner, ItemStack stack) {
            super(level, owner, stack);
        }

        private void hitTarget(Entity target) {
            this.onHitEntity(new EntityHitResult(target));
        }

        private void hitBlock(BlockHitResult hitResult) {
            this.onHitBlock(hitResult);
        }
    }
}
