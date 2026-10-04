package io.github.derkottersberg.swordthrow.internal;

import java.nio.file.Path;
import net.minecraft.client.KeyMapping;
import io.github.derkottersberg.swordthrow.network.ThrowStatePayload;
import io.github.derkottersberg.swordthrow.network.ThrowActionPayload;

/** Loader-owned client operations passed explicitly into shared client code. */
public interface ClientPlatformServices {
    String loaderName();

    Path configDirectory();

    KeyMapping throwKeyMapping();

    void sendToServer(ThrowActionPayload payload);
}
