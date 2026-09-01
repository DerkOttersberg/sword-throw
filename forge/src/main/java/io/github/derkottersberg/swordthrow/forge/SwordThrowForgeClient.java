package io.github.derkottersberg.swordthrow.forge;

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
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;

final class SwordThrowForgeClient {
    private SwordThrowForgeClient() {
    }

    static void initialize(FMLJavaModLoadingContext context) {
        RegisterKeyMappingsEvent.BUS.addListener(event -> event.register(SwordThrowKeyMappings.THROW));
        SwordThrowClient.initialize(new ForgeClientPlatformServices());
        EntityRenderersEvent.RegisterRenderers.BUS.addListener(event ->
            event.registerEntityRenderer(ModEntities.thrownSword(), ThrownSwordRenderer::new));
        TickEvent.ClientTickEvent.Post.BUS.addListener(event ->
            SwordThrowClient.tick(Minecraft.getInstance()));
        context.getContainer().registerExtensionPoint(
            ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new ConfigScreenHandler.ConfigScreenFactory(SwordThrowConfigScreen::new)
        );
    }

    static void handleThrowState(ThrowStatePayload payload) {
        SwordThrowClient.handleThrowState(payload);
    }

    private static final class ForgeClientPlatformServices implements ClientPlatformServices {
        @Override
        public String loaderName() {
            return "Forge";
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
        public void sendToServer(CustomPacketPayload payload) {
            SwordThrowForge.sendToServer(payload);
        }
    }
}
