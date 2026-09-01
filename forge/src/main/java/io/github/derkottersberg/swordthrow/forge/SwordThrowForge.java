package io.github.derkottersberg.swordthrow.forge;

import io.github.derkottersberg.swordthrow.SwordThrow;
import io.github.derkottersberg.swordthrow.internal.PlatformServices;
import io.github.derkottersberg.swordthrow.network.ThrowActionPayload;
import io.github.derkottersberg.swordthrow.network.ThrowStatePayload;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

@Mod(SwordThrow.MOD_ID)
public final class SwordThrowForge {
    private static final String PROTOCOL_VERSION = "3";
    private static final Channel<CustomPacketPayload> NETWORK = createNetwork();
    private static final Channel<CustomPacketPayload> STATE_NETWORK = createStateNetwork();

    public SwordThrowForge(FMLJavaModLoadingContext context) {
        registerDevelopmentGameTests(context.getModBusGroup());
        SwordThrow.initialize(new ForgePlatformServices(context));
        PlayerEvent.PlayerLoggedOutEvent.BUS.addListener(event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                SwordThrow.clearCharge(player);
            }
        });
        PlayerEvent.Clone.BUS.addListener(event -> {
            if (event.getOriginal() instanceof ServerPlayer original) {
                SwordThrow.clearCharge(original);
            }
            if (event.getEntity() instanceof ServerPlayer replacement) {
                SwordThrow.clearCharge(replacement);
            }
        });
        PlayerEvent.PlayerChangedDimensionEvent.BUS.addListener(event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                SwordThrow.clearCharge(player);
            }
        });
        PlayerEvent.PlayerRespawnEvent.BUS.addListener(event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                SwordThrow.clearCharge(player);
            }
        });
        PlayerEvent.StartTracking.BUS.addListener(event -> {
            if (event.getEntity() instanceof ServerPlayer observer) {
                SwordThrow.syncActiveChargeTo(observer, event.getTarget());
            }
        });
        TickEvent.ServerTickEvent.Post.BUS.addListener(event -> SwordThrow.tickServer(event.server()));
        ServerStoppedEvent.BUS.addListener(event -> SwordThrow.clearAllCharges());
        if (FMLEnvironment.dist.isClient()) {
            SwordThrowForgeClient.initialize(context);
        }
    }

    private static Channel<CustomPacketPayload> createNetwork() {
        return ChannelBuilder.named(SwordThrow.id("network"))
            .networkProtocolVersion(Integer.parseInt(PROTOCOL_VERSION))
            .payloadChannel()
            .play()
            .serverbound()
            .addMain(
                ThrowActionPayload.TYPE,
                ThrowActionPayload.STREAM_CODEC,
                (payload, context) -> {
                    ServerPlayer sender = context.getSender();
                    if (sender != null) {
                        SwordThrow.handleThrowAction(sender, payload);
                    }
                }
            )
            .build();
    }

    private static Channel<CustomPacketPayload> createStateNetwork() {
        return ChannelBuilder.named(SwordThrow.id("state_network"))
            .networkProtocolVersion(Integer.parseInt(PROTOCOL_VERSION))
            .payloadChannel()
            .play()
            .clientbound()
            .addMain(
                ThrowStatePayload.TYPE,
                ThrowStatePayload.STREAM_CODEC,
                (payload, context) -> SwordThrowForgeClient.handleThrowState(payload)
            )
            .build();
    }

    static void sendToServer(CustomPacketPayload payload) {
        NETWORK.send(payload, PacketDistributor.SERVER.noArg());
    }

    private static final class ForgePlatformServices implements PlatformServices {
        private final DeferredRegister<EntityType<?>> entityTypes =
            DeferredRegister.create(Registries.ENTITY_TYPE, SwordThrow.MOD_ID);

        ForgePlatformServices(FMLJavaModLoadingContext context) {
            entityTypes.register(context.getModBusGroup());
        }

        @Override
        public String loaderName() {
            return "Forge";
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
            RegistryObject<EntityType<T>> holder = entityTypes.register(path, factory);
            return holder::get;
        }

        @Override
        public int getEnchantmentLevel(ItemStack stack, Holder<Enchantment> enchantment) {
            return EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack);
        }

        @Override
        public SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, Entity entity) {
            return state.getSoundType(level, pos, entity);
        }

        @Override
        public void sendToPlayer(ServerPlayer target, CustomPacketPayload payload) {
            if (target.connection != null) {
                STATE_NETWORK.send(payload, PacketDistributor.PLAYER.with(target));
            }
        }

        @Override
        public void sendToTrackingAndSelf(ServerPlayer source, CustomPacketPayload payload) {
            STATE_NETWORK.send(payload, PacketDistributor.TRACKING_ENTITY_AND_SELF.with(source));
        }
    }

    /** Register the source-set-only GameTests without shipping them in release jars. */
    private static void registerDevelopmentGameTests(BusGroup modBus) {
        try {
            Class<?> bootstrap = Class.forName(
                "io.github.derkottersberg.swordthrow.forge.gametest.SwordThrowForgeGameTests"
            );
            bootstrap.getMethod("register", BusGroup.class).invoke(null, modBus);
        } catch (ClassNotFoundException ignored) {
            // Expected in production jars and normal development launches.
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
            throw new IllegalStateException("Could not register Sword Throw Forge GameTests", exception);
        }
    }
}
