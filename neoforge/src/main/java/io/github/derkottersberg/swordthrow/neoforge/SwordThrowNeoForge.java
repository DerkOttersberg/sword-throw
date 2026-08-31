package io.github.derkottersberg.swordthrow.neoforge;

import io.github.derkottersberg.swordthrow.SwordThrow;
import io.github.derkottersberg.swordthrow.internal.PlatformServices;
import io.github.derkottersberg.swordthrow.network.ThrowActionPayload;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.HandlerThread;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(SwordThrow.MOD_ID)
public final class SwordThrowNeoForge {
    public SwordThrowNeoForge(IEventBus modEventBus, ModContainer container) {
        modEventBus.addListener(this::registerPayloads);
        SwordThrow.initialize(new NeoForgePlatformServices(modEventBus));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                SwordThrow.clearCharge(player);
            }
        });
        if (FMLEnvironment.getDist().isClient()) {
            SwordThrowNeoForgeClient.initialize(modEventBus, container);
        }
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("2")
            .executesOn(HandlerThread.MAIN)
            .playToServer(
                ThrowActionPayload.TYPE,
                ThrowActionPayload.STREAM_CODEC,
                (payload, context) -> SwordThrow.handleThrowAction((ServerPlayer) context.player(), payload)
            );
    }

    private static final class NeoForgePlatformServices implements PlatformServices {
        private final DeferredRegister<EntityType<?>> entityTypes =
            DeferredRegister.create(Registries.ENTITY_TYPE, SwordThrow.MOD_ID);

        NeoForgePlatformServices(IEventBus modEventBus) {
            entityTypes.register(modEventBus);
        }

        @Override
        public String loaderName() {
            return "NeoForge";
        }

        @Override
        public <T extends Entity> RegistryHandle<EntityType<T>> registerEntityType(
            String path,
            Supplier<EntityType<T>> factory
        ) {
            DeferredHolder<EntityType<?>, EntityType<T>> holder = entityTypes.register(path, factory);
            return holder::get;
        }

        @Override
        public int getEnchantmentLevel(ItemStack stack, Holder<Enchantment> enchantment) {
            return stack.getEnchantmentLevel(enchantment);
        }

        @Override
        public SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, Entity entity) {
            return state.getSoundType(level, pos, entity);
        }
    }
}
