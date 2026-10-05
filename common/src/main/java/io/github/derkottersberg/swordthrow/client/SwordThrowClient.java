package io.github.derkottersberg.swordthrow.client;

import io.github.derkottersberg.swordthrow.SwordThrow;
import io.github.derkottersberg.swordthrow.client.config.SwordThrowClientConfig;
import io.github.derkottersberg.swordthrow.gameplay.ChargeMath;
import io.github.derkottersberg.swordthrow.internal.ClientPlatformServices;
import io.github.derkottersberg.swordthrow.network.ThrowActionPayload;
import io.github.derkottersberg.swordthrow.network.ThrowStatePayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class SwordThrowClient {
    private static final int CHARGE_BAR_WIDTH = 24;
    private static final int CHARGE_BAR_HEIGHT = 3;
    private static final ThrowPoseState LOCAL_POSE = new ThrowPoseState();
    private static final RemoteThrowPoseTracker REMOTE_POSES = new RemoteThrowPoseTracker();
    private static final ClientContextTracker<Level, Player> CLIENT_CONTEXT = new ClientContextTracker<>();

    private static ClientPlatformServices services;
    private static long clientTicks;
    private static boolean charging;
    private static int chargeTicks;
    private static boolean allowNextSingleItemDrop;
    private static boolean serverRejectedUntilKeyRelease;
    private static ItemStack chargingStack = ItemStack.EMPTY;

    private SwordThrowClient() {
    }

    public static void initialize(ClientPlatformServices platformServices) {
        if (services != null) {
            throw new IllegalStateException("Sword Throw client initialized twice");
        }
        services = platformServices;
        SwordThrowClientConfig.initialize(platformServices.configDirectory());
        SwordThrow.LOGGER.info("Sword Throw client initialized on {}", platformServices.loaderName());
    }

    public static void tick(Minecraft client) {
        clientTicks++;
        migrateDropKeyBinding(client);
        Player previousPlayer = CLIENT_CONTEXT.player();
        switch (CLIENT_CONTEXT.update(client.level, client.player)) {
            case LEVEL_CHANGED -> resetAllClientState();
            case PLAYER_CHANGED -> {
                resetLocalInputState();
                if (previousPlayer != null) {
                    REMOTE_POSES.remove(previousPlayer.getId());
                }
            }
            case NONE -> {
            }
        }

        LOCAL_POSE.tick();
        tickRemotePoses(client);

        if (client.player == null || client.level == null || !client.player.isAlive() || client.player.isSpectator()) {
            resetLocalInputState();
            return;
        }

        KeyMapping throwKey = requireServices().throwKeyMapping();
        ItemStack heldStack = client.player.getMainHandItem();
        boolean keyDown = throwKey.isDown() && client.screen == null;
        boolean canThrowHeldItem = SwordThrow.canThrow(heldStack)
            && !client.player.getCooldowns().isOnCooldown(heldStack.getItem());

        if (!keyDown) {
            serverRejectedUntilKeyRelease = false;
        }

        if (charging && !sameChargingStack(heldStack)) {
            cancelCharge(true);
        }

        // Count elapsed ticks after START, including the release tick. START itself
        // has elapsed zero ticks; counting it would overstate the server clock.
        if (charging) {
            chargeTicks = Math.min(chargeTicks + 1, ChargeMath.MAX_CHARGE_TICKS);
        }

        if (keyDown && canThrowHeldItem && !serverRejectedUntilKeyRelease) {
            if (!charging) {
                charging = true;
                chargeTicks = 0;
                chargingStack = heldStack.copy();
                LOCAL_POSE.beginCharge();
                requireServices().sendToServer(ThrowActionPayload.start());
            }

            LOCAL_POSE.setChargeProgress(ChargeMath.progress(chargeTicks));
            return;
        }

        if (!charging) {
            return;
        }

        if (client.screen != null || !canThrowHeldItem) {
            cancelCharge(true);
            return;
        }

        if (ChargeMath.canRelease(chargeTicks)) {
            LOCAL_POSE.releaseForward(ChargeMath.progress(chargeTicks));
            requireServices().sendToServer(ThrowActionPayload.release(chargeTicks));
            clearChargeFields();
        } else {
            requireServices().sendToServer(ThrowActionPayload.cancel());
            LOCAL_POSE.cancel();
            clearChargeFields();
            if (throwKey.same(client.options.keyDrop)) {
                allowNormalSingleItemDrop(client);
            }
        }
    }

    public static void handleThrowState(ThrowStatePayload payload) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && client.player.getId() == payload.playerEntityId()) {
            handleLocalThrowState(payload);
            return;
        }

        REMOTE_POSES.apply(payload, clientTicks);
    }

    public static void renderChargeBar(GuiGraphics graphics) {
        if (!charging) {
            return;
        }

        int left = graphics.guiWidth() / 2 - CHARGE_BAR_WIDTH / 2;
        int top = graphics.guiHeight() / 2 + 12;
        int right = left + CHARGE_BAR_WIDTH;
        int bottom = top + CHARGE_BAR_HEIGHT;

        // Power feedback must follow input time, not the deliberately smoothed arm pose.
        float progress = ChargeMath.progress(chargeTicks);
        int fillWidth = Math.round((CHARGE_BAR_WIDTH - 2) * progress);
        int fillColor = chargeTicks >= ChargeMath.MAX_CHARGE_TICKS ? 0xFFDDD37A
            : ChargeMath.canRelease(chargeTicks) ? 0xFF7DBEC9 : 0xFFC96A6A;

        graphics.fill(left - 1, top - 1, right + 1, bottom + 1, 0xAA111111);
        graphics.fill(left, top, right, bottom, 0xCC2A2A2A);
        graphics.fill(left + 1, top + 1, left + 1 + fillWidth, bottom - 1, fillColor);
    }

    public static boolean shouldInterceptDropKey(Minecraft client) {
        if (client == null || services == null || client.player == null || client.screen != null) {
            return false;
        }
        KeyMapping throwKey = services.throwKeyMapping();
        ItemStack held = client.player.getMainHandItem();
        return client.player.isAlive()
            && !client.player.isSpectator()
            && throwKey.same(client.options.keyDrop)
            && throwKey.isDown()
            && SwordThrow.canThrow(held)
            && !client.player.getCooldowns().isOnCooldown(held.getItem());
    }

    public static boolean consumeSingleItemDropBypass() {
        if (!allowNextSingleItemDrop) {
            return false;
        }
        allowNextSingleItemDrop = false;
        return true;
    }

    public static ThrowPoseState poseStateFor(int entityId) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && client.player.getId() == entityId) {
            return charging || !LOCAL_POSE.isIdle() ? LOCAL_POSE : null;
        }
        return REMOTE_POSES.poseFor(entityId);
    }

    public static ThrowPoseState localPoseState() {
        return LOCAL_POSE;
    }

    static int remotePoseCount() {
        return REMOTE_POSES.size();
    }

    private static void handleLocalThrowState(ThrowStatePayload payload) {
        switch (payload.phase()) {
            case CHARGING -> {
                if (!charging && LOCAL_POSE.isIdle()) {
                    LOCAL_POSE.beginCharge();
                }
                LOCAL_POSE.setChargeProgress(ChargeMath.progress(payload.chargeTicks()));
            }
            case RELEASE -> {
                LOCAL_POSE.releaseForward(ChargeMath.progress(payload.chargeTicks()));
                clearChargeFields();
                serverRejectedUntilKeyRelease = false;
            }
            case CANCEL -> {
                LOCAL_POSE.cancel();
                serverRejectedUntilKeyRelease = charging
                    && services != null
                    && services.throwKeyMapping().isDown();
                clearChargeFields();
            }
        }
    }

    private static void tickRemotePoses(Minecraft client) {
        REMOTE_POSES.tick(clientTicks, entityId ->
            client.level != null && client.level.getEntity(entityId) instanceof Player);
    }

    private static boolean sameChargingStack(ItemStack heldStack) {
        return heldStack.getCount() == chargingStack.getCount()
            && ItemStack.isSameItemSameComponents(heldStack, chargingStack);
    }

    private static void allowNormalSingleItemDrop(Minecraft client) {
        if (client.player == null || client.gameMode == null || client.screen != null) {
            return;
        }
        allowNextSingleItemDrop = true;
        client.player.drop(false);
    }

    private static void cancelCharge(boolean tellServer) {
        if (tellServer && services != null) {
            services.sendToServer(ThrowActionPayload.cancel());
        }
        LOCAL_POSE.cancel();
        clearChargeFields();
    }

    private static void clearChargeFields() {
        charging = false;
        chargeTicks = 0;
        chargingStack = ItemStack.EMPTY;
    }

    private static void resetLocalInputState() {
        clearChargeFields();
        allowNextSingleItemDrop = false;
        serverRejectedUntilKeyRelease = false;
        LOCAL_POSE.cancel();
    }

    private static void resetAllClientState() {
        resetLocalInputState();
        REMOTE_POSES.clear();
    }

    private static void migrateDropKeyBinding(Minecraft client) {
        if (!SwordThrowClientConfig.consumeDropKeyMigration()) {
            return;
        }
        KeyMapping throwKey = requireServices().throwKeyMapping();
        throwKey.setKey(InputConstants.getKey(client.options.keyDrop.saveString()));
        KeyMapping.resetMapping();
        client.options.save();
        SwordThrow.LOGGER.info("Migrated the Sword Throw key to the existing Drop Item binding");
    }

    private static ClientPlatformServices requireServices() {
        if (services == null) {
            throw new IllegalStateException("Sword Throw client used before initialization");
        }
        return services;
    }

}
