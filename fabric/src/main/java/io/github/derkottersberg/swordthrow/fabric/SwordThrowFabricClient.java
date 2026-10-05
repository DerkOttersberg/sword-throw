package io.github.derkottersberg.swordthrow.fabric;

import io.github.derkottersberg.swordthrow.client.SwordThrowClient;
import io.github.derkottersberg.swordthrow.client.SwordThrowKeyMappings;
import io.github.derkottersberg.swordthrow.client.render.ThrownSwordRenderer;
import io.github.derkottersberg.swordthrow.entity.ModEntities;
import io.github.derkottersberg.swordthrow.internal.ClientPlatformServices;
import io.github.derkottersberg.swordthrow.network.ThrowStatePayload;
import java.nio.file.Path;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.KeyMapping;
import io.github.derkottersberg.swordthrow.network.ThrowActionPayload;

public final class SwordThrowFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(SwordThrowKeyMappings.THROW);
        SwordThrowClient.initialize(new FabricClientPlatformServices());
        ClientPlayNetworking.registerGlobalReceiver(ThrowStatePayload.ID, (payload, context) ->
                SwordThrowClient.handleThrowState(payload));
        EntityRendererRegistry.register(ModEntities.thrownSword(), ThrownSwordRenderer::new);
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
        public KeyMapping throwKeyMapping() {
            return SwordThrowKeyMappings.THROW;
        }

        @Override
        public void sendToServer(ThrowActionPayload payload) {
            if (ClientPlayNetworking.canSend(ThrowActionPayload.ID)) {
                ClientPlayNetworking.send(payload);
            }
        }
    }
}
