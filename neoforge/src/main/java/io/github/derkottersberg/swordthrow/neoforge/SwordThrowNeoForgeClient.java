package io.github.derkottersberg.swordthrow.neoforge;

import io.github.derkottersberg.swordthrow.client.SwordThrowClient;
import io.github.derkottersberg.swordthrow.client.SwordThrowKeyMappings;
import io.github.derkottersberg.swordthrow.client.config.SwordThrowConfigScreen;
import io.github.derkottersberg.swordthrow.client.render.ThrownSwordRenderer;
import io.github.derkottersberg.swordthrow.entity.ModEntities;
import io.github.derkottersberg.swordthrow.internal.ClientPlatformServices;
import io.github.derkottersberg.swordthrow.network.ThrowStatePayload;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import io.github.derkottersberg.swordthrow.network.ThrowActionPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.common.NeoForge;

final class SwordThrowNeoForgeClient {
    private SwordThrowNeoForgeClient() {
    }

    static void initialize(IEventBus modEventBus, ModContainer container) {
        modEventBus.addListener((RegisterKeyMappingsEvent event) -> event.register(SwordThrowKeyMappings.THROW));
        SwordThrowClient.initialize(new NeoForgeClientPlatformServices());
        modEventBus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
            event.registerEntityRenderer(ModEntities.thrownSword(), ThrownSwordRenderer::new));
        NeoForge.EVENT_BUS.addListener(SwordThrowNeoForgeClient::onClientTick);
        container.registerExtensionPoint(
            IConfigScreenFactory.class,
            (modContainer, parent) -> new SwordThrowConfigScreen(parent)
        );
    }

    static void handleThrowState(ThrowStatePayload payload) {
        SwordThrowClient.handleThrowState(payload);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        SwordThrowClient.tick(Minecraft.getInstance());
    }

    private static final class NeoForgeClientPlatformServices implements ClientPlatformServices {
        @Override
        public String loaderName() {
            return "NeoForge";
        }

        @Override
        public Path configDirectory() {
            return FMLPaths.CONFIGDIR.get();
        }

        @Override
        public KeyMapping throwKeyMapping() {
            return SwordThrowKeyMappings.THROW;
        }

        @Override
        public void sendToServer(ThrowActionPayload payload) {
            PacketDistributor.sendToServer(payload);
        }
    }
}
