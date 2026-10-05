package io.github.derkottersberg.swordthrow.fabric;

import io.github.derkottersberg.swordthrow.SwordThrow;
import io.github.derkottersberg.swordthrow.internal.PlatformServices;
import io.github.derkottersberg.swordthrow.network.ThrowActionPayload;
import io.github.derkottersberg.swordthrow.network.ThrowStatePayload;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

public final class SwordThrowFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playC2S().register(ThrowActionPayload.ID, ThrowActionPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(ThrowStatePayload.ID, ThrowStatePayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ThrowActionPayload.ID, (payload, context) ->
                SwordThrow.handleThrowAction(context.player(), payload));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
            SwordThrow.clearCharge(handler.player));
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            if (entity instanceof ServerPlayer player) {
                SwordThrow.clearCharge(player);
            }
        });
        EntityTrackingEvents.START_TRACKING.register((tracked, observer) ->
            SwordThrow.syncActiveChargeTo(observer, tracked));
        ServerTickEvents.END_SERVER_TICK.register(SwordThrow::tickServer);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> SwordThrow.clearAllCharges());

        SwordThrow.initialize(new FabricPlatformServices());
    }

    private static final class FabricPlatformServices implements PlatformServices {
        @Override
        public String loaderName() {
            return "Fabric";
        }

        @Override
        public Path configDirectory() {
            return FabricLoader.getInstance().getConfigDir();
        }

        @Override
        public <T extends Entity> RegistryHandle<EntityType<T>> registerEntityType(
            String path,
            Supplier<EntityType<T>> factory
        ) {
            EntityType<T> type = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                SwordThrow.id(path),
                factory.get()
            );
            return () -> type;
        }

        @Override
        public int getEnchantmentLevel(ItemStack stack, net.minecraft.core.Holder<Enchantment> enchantment) {
            return EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack);
        }

        @Override
        public SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, Entity entity) {
            return state.getSoundType();
        }

        @Override
        public void sendToPlayer(ServerPlayer target, ThrowStatePayload payload) {
            if (target.connection != null && ServerPlayNetworking.canSend(target, ThrowStatePayload.ID)) {
                ServerPlayNetworking.send(target, payload);
            }
        }

        @Override
        public void sendToTrackingAndSelf(ServerPlayer source, ThrowStatePayload payload) {
            LinkedHashSet<ServerPlayer> recipients = new LinkedHashSet<>(PlayerLookup.tracking(source));
            recipients.add(source);
            for (ServerPlayer recipient : recipients) {
                sendToPlayer(recipient, payload);
            }
        }
    }
}
