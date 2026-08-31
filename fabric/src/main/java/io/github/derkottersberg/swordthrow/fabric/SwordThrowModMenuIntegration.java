package io.github.derkottersberg.swordthrow.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import io.github.derkottersberg.swordthrow.client.config.SwordThrowConfigScreen;

public final class SwordThrowModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return SwordThrowConfigScreen::new;
    }
}
