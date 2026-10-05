package io.github.derkottersberg.swordthrow;

import io.github.derkottersberg.swordthrow.entity.ModEntities;
import io.github.derkottersberg.swordthrow.entity.ThrownSwordEntity;
import io.github.derkottersberg.swordthrow.config.SwordThrowServerConfig;
import io.github.derkottersberg.swordthrow.gameplay.ChargeMath;
import io.github.derkottersberg.swordthrow.gameplay.ThrowItemRules;
import io.github.derkottersberg.swordthrow.internal.PlatformServices;
import io.github.derkottersberg.swordthrow.network.ThrowActionPayload;
import io.github.derkottersberg.swordthrow.network.ThrowStatePayload;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SwordThrow {
    public static final String MOD_ID = "swordthrow";
    public static final String VERSION = "2.1.1+mc1.21.1";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final int POSE_HEARTBEAT_TICKS = 10;

    private static final Map<ServerPlayer, ChargeSession> CHARGE_SESSIONS = new HashMap<>();
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
        SwordThrowServerConfig.initialize(services.configDirectory());
        ModEntities.initialize(services);
        LOGGER.info("Sword Throw {} initialized on {}", VERSION, services.loaderName());
    }

    public static int getEnchantmentLevel(ItemStack stack, net.minecraft.core.Holder<Enchantment> enchantment) {
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

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
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
        if (player != null && CHARGE_SESSIONS.remove(player) != null) {
            broadcastPoseState(player, ThrowStatePayload.cancel(player.getId()));
        }
    }

    public static void clearAllCharges() {
        CHARGE_SESSIONS.clear();
    }

    public static void tickServer(MinecraftServer server) {
        int serverTick = server.getTickCount();
        Iterator<Map.Entry<ServerPlayer, ChargeSession>> iterator = CHARGE_SESSIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ServerPlayer, ChargeSession> entry = iterator.next();
            ServerPlayer player = entry.getKey();
            if (player.isRemoved() || player.level().getServer() != server) {
                iterator.remove();
                broadcastPoseState(player, ThrowStatePayload.cancel(player.getId()));
                continue;
            }

            ChargeSession session = entry.getValue();
            long elapsedTicks = (long)serverTick - session.startedAtServerTick();
            if (!isSessionValid(player, session, elapsedTicks)) {
                iterator.remove();
                broadcastPoseState(player, ThrowStatePayload.cancel(player.getId()));
                continue;
            }

            if (elapsedTicks % POSE_HEARTBEAT_TICKS == 0L) {
                broadcastPoseState(
                    player,
                    ThrowStatePayload.charging(player.getId(), ChargeMath.clampCharge(elapsedTicks))
                );
            }
        }
    }

    /** Immediately brings a newly tracking client up to date with an active throw charge. */
    public static boolean syncActiveChargeTo(ServerPlayer observer, Entity tracked) {
        if (observer == null || observer.connection == null || !(tracked instanceof ServerPlayer source)) {
            return false;
        }

        ChargeSession session = CHARGE_SESSIONS.get(source);
        if (session == null) {
            return false;
        }

        long elapsedTicks = (long)currentServerTick(source) - session.startedAtServerTick();
        if (!isSessionValid(source, session, elapsedTicks)) {
            return false;
        }

        requirePlatformServices().sendToPlayer(
            observer,
            ThrowStatePayload.charging(source.getId(), ChargeMath.clampCharge(elapsedTicks))
        );
        return true;
    }

    private static void beginCharge(ServerPlayer player) {
        ChargeSession previous = CHARGE_SESSIONS.get(player);
        if (previous != null) {
            long elapsedTicks = currentServerTick(player) - previous.startedAtServerTick();
            if (isSessionValid(player, previous, elapsedTicks)) {
                return;
            }
            CHARGE_SESSIONS.remove(player);
            broadcastPoseState(player, ThrowStatePayload.cancel(player.getId()));
        }

        if (!canStartThrow(player)) {
            broadcastPoseState(player, ThrowStatePayload.cancel(player.getId()));
            return;
        }

        ItemStack held = player.getMainHandItem();
        CHARGE_SESSIONS.put(player, new ChargeSession(
            currentServerTick(player),
            player.level().dimension(),
            held.copy(),
            held.getCount()
        ));
        broadcastPoseState(player, ThrowStatePayload.charging(player.getId(), 0));
    }

    private static void releaseCharge(ServerPlayer player, int clientChargeTicks) {
        ChargeSession session = CHARGE_SESSIONS.remove(player);
        if (session == null) {
            broadcastPoseState(player, ThrowStatePayload.cancel(player.getId()));
            return;
        }

        long elapsedTicks = currentServerTick(player) - session.startedAtServerTick();
        if (!isSessionValid(player, session, elapsedTicks)) {
            broadcastPoseState(player, ThrowStatePayload.cancel(player.getId()));
            return;
        }

        ItemStack held = player.getMainHandItem();
        if (held.getCount() != session.stackCount()
            || !ItemStack.isSameItemSameComponents(held, session.stackSnapshot())) {
            broadcastPoseState(player, ThrowStatePayload.cancel(player.getId()));
            return;
        }

        int chargeTicks = ChargeMath.effectiveCharge(elapsedTicks, clientChargeTicks);
        if (!ChargeMath.canRelease(chargeTicks)) {
            broadcastPoseState(player, ThrowStatePayload.cancel(player.getId()));
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
            LOGGER.warn("Rejected thrown-item spawn for {}", player.getGameProfile().getName());
            broadcastPoseState(player, ThrowStatePayload.cancel(player.getId()));
            return;
        }

        if (!player.getAbilities().instabuild) {
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }

        float chargeProgress = ChargeMath.progress(chargeTicks);
        playThrowSound(player, projectile, chargeProgress);
        player.getCooldowns().addCooldown(thrownStack.getItem(), ChargeMath.cooldown(chargeTicks));
        broadcastPoseState(player, ThrowStatePayload.release(player.getId(), chargeTicks));
    }

    private static boolean canStartThrow(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator()) {
            return false;
        }

        ItemStack held = player.getMainHandItem();
        return canThrow(held) && !player.getCooldowns().isOnCooldown(held.getItem());
    }

    public static boolean canThrow(ItemStack stack) {
        return ThrowItemRules.canThrow(stack);
    }

    private static boolean isSessionValid(ServerPlayer player, ChargeSession session, long elapsedTicks) {
        if (!canStartThrow(player) || !ChargeMath.isSessionFresh(elapsedTicks)) {
            return false;
        }
        if (!player.level().dimension().equals(session.dimension())) {
            return false;
        }
        ItemStack held = player.getMainHandItem();
        return held.getCount() == session.stackCount()
            && ItemStack.isSameItemSameComponents(held, session.stackSnapshot());
    }

    private static int currentServerTick(ServerPlayer player) {
        return player.level().getServer().getTickCount();
    }

    private static void broadcastPoseState(ServerPlayer player, ThrowStatePayload payload) {
        // GameTest mock players have no connection. Real online players always do.
        if (player.connection != null) {
            requirePlatformServices().sendToTrackingAndSelf(player, payload);
        }
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

    private record ChargeSession(
        int startedAtServerTick,
        ResourceKey<Level> dimension,
        ItemStack stackSnapshot,
        int stackCount
    ) {
    }
}
