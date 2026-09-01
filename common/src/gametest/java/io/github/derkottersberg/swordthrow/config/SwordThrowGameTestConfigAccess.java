package io.github.derkottersberg.swordthrow.config;

/** Test-source-only bridge for deterministic live damage scenarios. */
public final class SwordThrowGameTestConfigAccess {
    private SwordThrowGameTestConfigAccess() {
    }

    public static void replace(SwordThrowServerConfig.ConfigData replacement) {
        SwordThrowServerConfig.replaceForTests(replacement);
    }

    public static void reset() {
        SwordThrowServerConfig.replaceForTests(new SwordThrowServerConfig.ConfigData());
    }
}
