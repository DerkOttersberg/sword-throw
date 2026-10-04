package io.github.derkottersberg.swordthrow.client;

import io.github.derkottersberg.swordthrow.SwordThrow;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;

/** Loader adapters register this mapping with their native client API. */
public final class SwordThrowKeyMappings {
    public static final String CATEGORY = "key.categories.swordthrow.controls";
    public static final KeyMapping THROW = new KeyMapping(
        "key.swordthrow.throw",
        InputConstants.KEY_Q,
        CATEGORY
    );

    private SwordThrowKeyMappings() {
    }
}
