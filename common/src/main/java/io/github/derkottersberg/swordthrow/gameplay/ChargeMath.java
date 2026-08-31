package io.github.derkottersberg.swordthrow.gameplay;

/** Pure charge validation shared by runtime code and unit tests. */
public final class ChargeMath {
    public static final int MIN_CHARGE_TICKS = 15;
    public static final int MAX_CHARGE_TICKS = 30;
    public static final int MAX_SESSION_AGE_TICKS = 80;

    private ChargeMath() {
    }

    public static int effectiveCharge(long serverElapsedTicks, int claimedClientTicks) {
        int elapsed = clampToCharge(serverElapsedTicks);
        int claimed = clampToCharge(claimedClientTicks);
        return Math.min(elapsed, claimed);
    }

    public static boolean isSessionFresh(long serverElapsedTicks) {
        return serverElapsedTicks >= 0 && serverElapsedTicks <= MAX_SESSION_AGE_TICKS;
    }

    public static float progress(int chargeTicks) {
        return clampToCharge(chargeTicks) / (float) MAX_CHARGE_TICKS;
    }

    public static float speed(int chargeTicks) {
        return 1.10F + progress(chargeTicks) * 1.20F;
    }

    public static int cooldown(int chargeTicks) {
        return 10 + Math.round(progress(chargeTicks) * 8.0F);
    }

    private static int clampToCharge(long ticks) {
        return (int) Math.max(0L, Math.min(ticks, MAX_CHARGE_TICKS));
    }
}
