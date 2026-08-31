package io.github.derkottersberg.swordthrow.fabric;

import io.github.derkottersberg.swordthrow.SwordThrow;
import io.github.derkottersberg.swordthrow.internal.PlatformServices;
import io.github.derkottersberg.swordthrow.network.ThrowActionPayload;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
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
        PayloadTypeRegistry.serverboundPlay().register(ThrowActionPayload.TYPE, ThrowActionPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ThrowActionPayload.TYPE, (payload, context) ->
            context.server().execute(() -> SwordThrow.handleThrowAction(context.player(), payload)));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
            SwordThrow.clearCharge(handler.getPlayer()));

        SwordThrow.initialize(new FabricPlatformServices());
    }

    private static final class FabricPlatformServices implements PlatformServices {
        @Override
        public String loaderName() {
            return "Fabric";
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
        public int getEnchantmentLevel(ItemStack stack, Holder<Enchantment> enchantment) {
            return EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack);
        }

        @Override
        public SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, Entity entity) {
            return state.getSoundType();
        }
    }
}
