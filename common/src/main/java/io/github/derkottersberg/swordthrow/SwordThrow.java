package io.github.derkottersberg.swordthrow;

import io.github.derkottersberg.swordthrow.entity.ModEntities;
import io.github.derkottersberg.swordthrow.entity.ThrownSwordEntity;
import io.github.derkottersberg.swordthrow.gameplay.ChargeMath;
import io.github.derkottersberg.swordthrow.internal.PlatformServices;
import io.github.derkottersberg.swordthrow.network.ThrowActionPayload;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SwordThrow {
    public static final String MOD_ID = "swordthrow";
    public static final String VERSION = "2.0.0+mc26.2";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final Map<UUID, ChargeSession> CHARGE_SESSIONS = new HashMap<>();
    private static PlatformServices platformServices;
    private static boolean initialized;

    private SwordThrow() {
    }

    public static void initialize(PlatformServices services) {
        if (initialized) {
            throw new IllegalStateException("Sword Throw initialized twice");
        }
        platformServices = Objects.requireNonNull(services, "services");
        initialized = true;
        ModEntities.initialize(services);
        LOGGER.info("Sword Throw {} initialized on {}", VERSION, services.loaderName());
    }

    public static int getEnchantmentLevel(ItemStack stack, Holder<Enchantment> enchantment) {
        return requirePlatformServices().getEnchantmentLevel(stack, enchantment);
    }

    public static SoundType getSoundType(
        BlockState state,
        LevelReader level,
        BlockPos pos,
        Entity entity
    ) {
        return requirePlatformServices().getSoundType(state, level, pos, entity);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static void handleThrowAction(ServerPlayer player, ThrowActionPayload payload) {
        if (player == null) {
            return;
        }

        switch (payload.action()) {
            case START -> beginCharge(player);
            case CANCEL -> clearCharge(player);
            case RELEASE -> releaseCharge(player, payload.clientChargeTicks());
        }
    }

    public static void clearCharge(ServerPlayer player) {
        if (player != null) {
            CHARGE_SESSIONS.remove(player.getUUID());
        }
    }

    private static void beginCharge(ServerPlayer player) {
        if (!canStartThrow(player)) {
            CHARGE_SESSIONS.remove(player.getUUID());
            return;
        }

        ItemStack held = player.getMainHandItem();
        CHARGE_SESSIONS.put(player.getUUID(), new ChargeSession(
            player.level().getGameTime(),
            held.copy(),
            held.getCount()
        ));
    }

    private static void releaseCharge(ServerPlayer player, int clientChargeTicks) {
        ChargeSession session = CHARGE_SESSIONS.remove(player.getUUID());
        if (session == null || !canStartThrow(player)) {
            return;
        }

        long elapsedTicks = player.level().getGameTime() - session.startedAtGameTick();
        if (!ChargeMath.isSessionFresh(elapsedTicks)) {
            return;
        }

        ItemStack held = player.getMainHandItem();
        if (held.getCount() != session.stackCount()
            || !ItemStack.isSameItemSameComponents(held, session.stackSnapshot())) {
            return;
        }

        int chargeTicks = ChargeMath.effectiveCharge(elapsedTicks, clientChargeTicks);
        if (chargeTicks < ChargeMath.MIN_CHARGE_TICKS) {
            return;
        }

        ItemStack thrownStack = held.copy();
        ThrownSwordEntity projectile = new ThrownSwordEntity(player.level(), player, thrownStack);
        projectile.shootFromRotation(
            player,
            player.getXRot(),
            player.getYRot(),
            0.0F,
            ChargeMath.speed(chargeTicks),
            0.75F
        );

        // Do not remove the player's item unless the entity was accepted by the level.
        if (!player.level().addFreshEntity(projectile)) {
            LOGGER.warn("Rejected thrown-item spawn for {}", player.getGameProfile().name());
            return;
        }

        if (!player.getAbilities().instabuild) {
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }

        float chargeProgress = ChargeMath.progress(chargeTicks);
        playThrowSound(player, projectile, chargeProgress);
        player.getCooldowns().addCooldown(thrownStack, ChargeMath.cooldown(chargeTicks));
    }

    private static boolean canStartThrow(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator()) {
            return false;
        }

        ItemStack held = player.getMainHandItem();
        return canThrow(held) && !player.getCooldowns().isOnCooldown(held);
    }

    public static boolean canThrow(ItemStack stack) {
        return stack != null && !stack.isEmpty() && !isVanillaTrident(stack);
    }

    private static boolean isVanillaTrident(ItemStack stack) {
        return stack.getItem() instanceof TridentItem || stack.is(Items.TRIDENT);
    }

    private static void playThrowSound(ServerPlayer player, ThrownSwordEntity projectile, float chargeProgress) {
        float baseVolume = 0.45F + chargeProgress * 0.3F;
        float randomPitch = 0.92F + player.getRandom().nextFloat() * 0.12F;

        player.level().playSound(
            null,
            player.getX(),
            player.getY(),
            player.getZ(),
            SoundEvents.ARROW_SHOOT,
            SoundSource.PLAYERS,
            baseVolume,
            randomPitch
        );

        if (projectile.usesPointFirstFlight()) {
            player.level().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.TRIDENT_THROW,
                SoundSource.PLAYERS,
                0.8F + chargeProgress * 0.25F,
                0.95F + player.getRandom().nextFloat() * 0.08F
            );
        } else {
            player.level().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS,
                0.18F + chargeProgress * 0.16F,
                0.8F + player.getRandom().nextFloat() * 0.15F
            );
        }
    }

    private static PlatformServices requirePlatformServices() {
        if (platformServices == null) {
            throw new IllegalStateException("Sword Throw platform services are not initialized");
        }
        return platformServices;
    }

    private record ChargeSession(long startedAtGameTick, ItemStack stackSnapshot, int stackCount) {
    }
}
