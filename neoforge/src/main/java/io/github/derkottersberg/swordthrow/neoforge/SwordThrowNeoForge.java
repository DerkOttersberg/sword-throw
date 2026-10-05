package io.github.derkottersberg.swordthrow.neoforge;

import io.github.derkottersberg.swordthrow.SwordThrow;
import io.github.derkottersberg.swordthrow.internal.PlatformServices;
import io.github.derkottersberg.swordthrow.network.ThrowActionPayload;
import io.github.derkottersberg.swordthrow.network.ThrowStatePayload;
import java.nio.file.Path;
import java.lang.reflect.InvocationTargetException;
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
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.HandlerThread;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(SwordThrow.MOD_ID)
public final class SwordThrowNeoForge {
    public SwordThrowNeoForge(IEventBus modEventBus, ModContainer container) {
        registerDevelopmentGameTests(modEventBus);
        modEventBus.addListener(this::registerPayloads);
        SwordThrow.initialize(new NeoForgePlatformServices(modEventBus));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                SwordThrow.clearCharge(player);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.Clone event) -> {
            if (event.getOriginal() instanceof ServerPlayer original) {
                SwordThrow.clearCharge(original);
            }
            if (event.getEntity() instanceof ServerPlayer replacement) {
                SwordThrow.clearCharge(replacement);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                SwordThrow.clearCharge(player);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                SwordThrow.clearCharge(player);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.StartTracking event) -> {
            if (event.getEntity() instanceof ServerPlayer observer) {
                SwordThrow.syncActiveChargeTo(observer, event.getTarget());
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> SwordThrow.tickServer(event.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> SwordThrow.clearAllCharges());
        if (FMLEnvironment.dist.isClient()) {
            SwordThrowNeoForgeClient.initialize(modEventBus, container);
        }
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("3")
            .executesOn(HandlerThread.MAIN)
            .playToServer(
                ThrowActionPayload.ID,
                ThrowActionPayload.STREAM_CODEC,
                (payload, context) -> SwordThrow.handleThrowAction((ServerPlayer) context.player(), payload)
            )
            .playToClient(
                ThrowStatePayload.ID,
                ThrowStatePayload.STREAM_CODEC,
                (payload, context) -> SwordThrowNeoForgeClient.handleThrowState(payload)
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
        public Path configDirectory() {
            return FMLPaths.CONFIGDIR.get();
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

        @Override
        public void sendToPlayer(
            ServerPlayer target,
            ThrowStatePayload payload
        ) {
            if (target.connection != null && NetworkRegistry.hasChannel(target.connection, payload.type().id())) {
                PacketDistributor.sendToPlayer(target, payload);
            }
        }

        @Override
        public void sendToTrackingAndSelf(
            ServerPlayer source,
            ThrowStatePayload payload
        ) {
            java.util.LinkedHashSet<ServerPlayer> recipients = new java.util.LinkedHashSet<>(
                ((net.minecraft.server.level.ServerLevel)source.level())
                    .getChunkSource()
                    .chunkMap
                    .getPlayersWatching(source)
            );
            recipients.add(source);
            recipients.forEach(recipient -> sendToPlayer(recipient, payload));
        }
    }

    /** Register the source-set-only GameTests without shipping them in release jars. */
    private static void registerDevelopmentGameTests(IEventBus modEventBus) {
        try {
            Class<?> bootstrap = Class.forName(
                "io.github.derkottersberg.swordthrow.neoforge.gametest.SwordThrowNeoForgeGameTests"
            );
            bootstrap.getMethod("register", IEventBus.class).invoke(null, modEventBus);
        } catch (ClassNotFoundException ignored) {
            // Expected in production jars and normal development launches.
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
            throw new IllegalStateException("Could not register Sword Throw NeoForge GameTests", exception);
        }
    }
}
