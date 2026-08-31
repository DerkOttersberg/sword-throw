package io.github.derkottersberg.swordthrow.fabric;

import io.github.derkottersberg.swordthrow.client.SwordThrowClient;
import io.github.derkottersberg.swordthrow.client.render.ThrownSwordRenderer;
import io.github.derkottersberg.swordthrow.entity.ModEntities;
import io.github.derkottersberg.swordthrow.internal.ClientPlatformServices;
import java.nio.file.Path;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class SwordThrowFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SwordThrowClient.initialize(new FabricClientPlatformServices());
        EntityRenderers.register(ModEntities.thrownSword(), ThrownSwordRenderer::new);
        ClientTickEvents.END_CLIENT_TICK.register(SwordThrowClient::tick);
    }

    private static final class FabricClientPlatformServices implements ClientPlatformServices {
        @Override
        public String loaderName() {
            return "Fabric";
        }

        @Override
        public Path configDirectory() {
            return FabricLoader.getInstance().getConfigDir();
        }

        @Override
        public void sendToServer(CustomPacketPayload payload) {
            ClientPlayNetworking.send(payload);
        }
    }
}
