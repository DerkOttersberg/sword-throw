package io.github.derkottersberg.swordthrow.client;

import io.github.derkottersberg.swordthrow.SwordThrow;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/** Loader adapters register this mapping with their native client API. */
public final class SwordThrowKeyMappings {
    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(SwordThrow.id("controls"));
    public static final KeyMapping THROW = new KeyMapping(
        "key.swordthrow.throw",
        GLFW.GLFW_KEY_Q,
        CATEGORY
    );

    private SwordThrowKeyMappings() {
    }
}
