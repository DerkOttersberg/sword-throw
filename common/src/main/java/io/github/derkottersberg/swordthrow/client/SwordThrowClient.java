package io.github.derkottersberg.swordthrow.client;

import io.github.derkottersberg.swordthrow.SwordThrow;
import io.github.derkottersberg.swordthrow.client.config.SwordThrowClientConfig;
import io.github.derkottersberg.swordthrow.gameplay.ChargeMath;
import io.github.derkottersberg.swordthrow.internal.ClientPlatformServices;
import io.github.derkottersberg.swordthrow.network.ThrowActionPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

public final class SwordThrowClient {
    private static final int CHARGE_BAR_WIDTH = 24;
    private static final int CHARGE_BAR_HEIGHT = 3;

    private static ClientPlatformServices services;
    private static boolean charging;
    private static int chargeTicks;
    private static boolean allowNextSingleItemDrop;
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
        ThrowPoseState.tick();

        if (client.player == null || client.level == null || !client.player.isAlive() || client.player.isSpectator()) {
            resetClientState();
            return;
        }

        ItemStack heldStack = client.player.getMainHandItem();
        boolean keyDown = client.options.keyDrop.isDown() && client.gui.screen() == null;
        boolean canThrowHeldItem = SwordThrow.canThrow(heldStack)
            && !client.player.getCooldowns().isOnCooldown(heldStack);

        if (charging && !sameChargingStack(heldStack)) {
            cancelCharge(true);
        }

        if (keyDown && canThrowHeldItem) {
            if (!charging) {
                charging = true;
                chargeTicks = 0;
                chargingStack = heldStack.copy();
                ThrowPoseState.beginCharge();
                requireServices().sendToServer(ThrowActionPayload.start());
            }

            chargeTicks = Math.min(chargeTicks + 1, ChargeMath.MAX_CHARGE_TICKS);
            ThrowPoseState.setChargeProgress(chargeTicks / (float) ChargeMath.MAX_CHARGE_TICKS);
            return;
        }

        if (!charging) {
            return;
        }

        if (client.gui.screen() != null || !canThrowHeldItem) {
            cancelCharge(true);
            return;
        }

        if (chargeTicks >= ChargeMath.MIN_CHARGE_TICKS) {
            ThrowPoseState.releaseForward();
            requireServices().sendToServer(ThrowActionPayload.release(chargeTicks));
            clearChargeFields();
        } else {
            requireServices().sendToServer(ThrowActionPayload.cancel());
            ThrowPoseState.cancel();
            clearChargeFields();
            allowNormalSingleItemDrop(client);
        }
    }

    public static void renderChargeBar(GuiGraphicsExtractor graphics) {
        if (!ThrowPoseState.isChargeIndicatorVisible()) {
            return;
        }

        int left = graphics.guiWidth() / 2 - CHARGE_BAR_WIDTH / 2;
        int top = graphics.guiHeight() / 2 + 12;
        int right = left + CHARGE_BAR_WIDTH;
        int bottom = top + CHARGE_BAR_HEIGHT;

        float progress = ThrowPoseState.getChargeIndicatorProgress(1.0F);
        int fillWidth = Math.max(1, Math.round((CHARGE_BAR_WIDTH - 2) * progress));
        int fillColor = progress >= 0.5F ? 0xFFDDD37A : 0xFFC96A6A;

        graphics.fill(left - 1, top - 1, right + 1, bottom + 1, 0xAA111111);
        graphics.fill(left, top, right, bottom, 0xCC2A2A2A);
        graphics.fill(left + 1, top + 1, left + 1 + fillWidth, bottom - 1, fillColor);
    }

    public static boolean shouldInterceptDropKey(Minecraft client) {
        return client != null
            && client.gui.screen() == null
            && client.player != null
            && client.player.isAlive()
            && !client.player.isSpectator()
            && SwordThrow.canThrow(client.player.getMainHandItem());
    }

    public static boolean consumeSingleItemDropBypass() {
        if (!allowNextSingleItemDrop) {
            return false;
        }
        allowNextSingleItemDrop = false;
        return true;
    }

    public static boolean isChargingLocalPlayer(int entityId) {
        Minecraft client = Minecraft.getInstance();
        return client.player != null
            && client.player.getId() == entityId
            && (charging || ThrowPoseState.isOffHandVisible());
    }

    private static boolean sameChargingStack(ItemStack heldStack) {
        return heldStack.getCount() == chargingStack.getCount()
            && ItemStack.isSameItemSameComponents(heldStack, chargingStack);
    }

    private static void allowNormalSingleItemDrop(Minecraft client) {
        if (client.player == null || client.gui.screen() != null) {
            return;
        }
        allowNextSingleItemDrop = true;
        client.player.drop(false);
    }

    private static void cancelCharge(boolean tellServer) {
        if (tellServer && services != null) {
            services.sendToServer(ThrowActionPayload.cancel());
        }
        ThrowPoseState.cancel();
        clearChargeFields();
    }

    private static void clearChargeFields() {
        charging = false;
        chargeTicks = 0;
        chargingStack = ItemStack.EMPTY;
    }

    private static void resetClientState() {
        clearChargeFields();
        allowNextSingleItemDrop = false;
        ThrowPoseState.cancel();
    }

    private static ClientPlatformServices requireServices() {
        if (services == null) {
            throw new IllegalStateException("Sword Throw client used before initialization");
        }
        return services;
    }
}
