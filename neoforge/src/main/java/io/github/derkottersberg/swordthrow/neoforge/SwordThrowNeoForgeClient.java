package io.github.derkottersberg.swordthrow.neoforge;

import io.github.derkottersberg.swordthrow.client.SwordThrowClient;
import io.github.derkottersberg.swordthrow.client.config.SwordThrowConfigScreen;
import io.github.derkottersberg.swordthrow.client.render.ThrownSwordRenderer;
import io.github.derkottersberg.swordthrow.entity.ModEntities;
import io.github.derkottersberg.swordthrow.internal.ClientPlatformServices;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;

final class SwordThrowNeoForgeClient {
    private SwordThrowNeoForgeClient() {
    }

    static void initialize(IEventBus modEventBus, ModContainer container) {
        SwordThrowClient.initialize(new NeoForgeClientPlatformServices());
        modEventBus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
            event.registerEntityRenderer(ModEntities.thrownSword(), ThrownSwordRenderer::new));
        NeoForge.EVENT_BUS.addListener(SwordThrowNeoForgeClient::onClientTick);
        container.registerExtensionPoint(
            IConfigScreenFactory.class,
            (modContainer, parent) -> new SwordThrowConfigScreen(parent)
        );
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
        public void sendToServer(CustomPacketPayload payload) {
            ClientPacketDistributor.sendToServer(payload);
        }
    }
}
