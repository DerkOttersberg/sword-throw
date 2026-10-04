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
import io.github.derkottersberg.swordthrow.network.ThrowActionPayload;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
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
        context.getModEventBus().addListener((RegisterKeyMappingsEvent event) -> event.register(SwordThrowKeyMappings.THROW));
        SwordThrowClient.initialize(new ForgeClientPlatformServices());
        context.getModEventBus().addListener((EntityRenderersEvent.RegisterRenderers event) ->
            event.registerEntityRenderer(ModEntities.thrownSword(), ThrownSwordRenderer::new));
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) SwordThrowClient.tick(Minecraft.getInstance());
        });
        ModLoadingContext.get().registerExtensionPoint(
            ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new ConfigScreenHandler.ConfigScreenFactory((client, parent) -> new SwordThrowConfigScreen(parent))
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
        public void sendToServer(ThrowActionPayload payload) {
            SwordThrowForge.sendToServer(payload);
        }
    }
}
