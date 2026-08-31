package io.github.derkottersberg.swordthrow.internal;

import java.nio.file.Path;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Loader-owned client operations passed explicitly into shared client code. */
public interface ClientPlatformServices {
    String loaderName();

    Path configDirectory();

    void sendToServer(CustomPacketPayload payload);
}
