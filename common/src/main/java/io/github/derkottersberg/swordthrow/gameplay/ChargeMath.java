package io.github.derkottersberg.swordthrow.gameplay;

/** Pure charge validation shared by runtime code and unit tests. */
public final class ChargeMath {
    // Separate a deliberate hold from a vanilla Drop Item tap, not from full power.
    public static final int MIN_CHARGE_TICKS = 2;
    public static final int MAX_CHARGE_TICKS = 30;
    public static final int MAX_SESSION_AGE_TICKS = 80;

    private ChargeMath() {
    }

    public static int effectiveCharge(long serverElapsedTicks, int claimedClientTicks) {
        int elapsed = clampCharge(serverElapsedTicks);
        int claimed = clampCharge(claimedClientTicks);
        return Math.min(elapsed, claimed);
    }

    public static boolean isSessionFresh(long serverElapsedTicks) {
        return serverElapsedTicks >= 0 && serverElapsedTicks <= MAX_SESSION_AGE_TICKS;
    }

    public static float progress(int chargeTicks) {
        return clampCharge(chargeTicks) / (float) MAX_CHARGE_TICKS;
    }

    public static boolean canRelease(int chargeTicks) {
        return clampCharge(chargeTicks) >= MIN_CHARGE_TICKS;
    }

    public static float speed(int chargeTicks) {
        return 1.10F + progress(chargeTicks) * 1.20F;
    }

    public static int cooldown(int chargeTicks) {
        return 10 + Math.round(progress(chargeTicks) * 8.0F);
    }

    public static int clampCharge(long ticks) {
        return (int) Math.max(0L, Math.min(ticks, MAX_CHARGE_TICKS));
    }
}
