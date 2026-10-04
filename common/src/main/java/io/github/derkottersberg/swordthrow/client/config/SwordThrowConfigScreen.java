package io.github.derkottersberg.swordthrow.client.config;

import io.github.derkottersberg.swordthrow.internal.client.SettingsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class SwordThrowConfigScreen extends SettingsScreen {
    private boolean animations, trail;
    private int color;

    public SwordThrowConfigScreen(Screen parent) {
        super(parent, "Sword Throw Settings", "Visual effects on this client. Damage is controlled by the server.");
        setDraft(SwordThrowClientConfig.get());
    }

    private void setDraft(SwordThrowClientConfig.ConfigData data) {
        this.animations = data.thirdPersonAnimationsEnabled();
        this.trail = data.trailEffectEnabled();
        this.color = data.trailColor();
    }

    @Override
    protected void buildSettings() {
        toggleSetting("Throw visuals", "Third-person animation", "Animate the player's throwing pose when viewed in third person. Does not change throw strength or damage.",
            this.animations, v -> this.animations = v);
        toggleSetting("Throw visuals", "Projectile trail", "Draw a colored trail behind thrown items. Disable for a cleaner view or fewer visual effects.",
            this.trail, v -> this.trail = v);
        actionSetting("Throw visuals", "Trail color", "Click to cycle through seven colors. Visible when projectile trails are enabled.",
            SwordThrowClientConfig.colorLabel(this.color), button -> {
                this.color = SwordThrowClientConfig.nextTrailColor(this.color);
                button.setMessage(Component.literal(SwordThrowClientConfig.colorLabel(this.color)));
            });
    }

    @Override
    protected void resetDraft() { setDraft(new SwordThrowClientConfig.ConfigData()); }

    @Override
    protected void saveDraft() {
        SwordThrowClientConfig.set(new SwordThrowClientConfig.ConfigData(this.animations, this.trail, this.color));
    }

    @Override
    protected String summary() { return "Throw key: Options > Controls. Damage tuning: swordthrow-server.json."; }
}
