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
import java.util.Optional;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.Channel;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraftforge.network.NetworkDirection;
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
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

@Mod(SwordThrow.MOD_ID)
public final class SwordThrowForge {
    private static final String PROTOCOL_VERSION = "3";
    private static final Channel<CustomPacketPayload> NETWORK = createNetwork();

    public SwordThrowForge(FMLJavaModLoadingContext context) {
        SwordThrow.initialize(new ForgePlatformServices(context));
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> clear(event.getEntity()));
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.Clone event) -> {
            clear(event.getOriginal());
            clear(event.getEntity());
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent event) -> clear(event.getEntity()));
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent event) -> clear(event.getEntity()));
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.StartTracking event) -> {
            if (event.getEntity() instanceof ServerPlayer observer) SwordThrow.syncActiveChargeTo(observer, event.getTarget());
        });
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) SwordThrow.tickServer(event.getServer());
        });
        MinecraftForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> SwordThrow.clearAllCharges());
        if (FMLEnvironment.dist.isClient()) SwordThrowForgeClient.initialize(context);
    }

    private static void clear(net.minecraft.world.entity.player.Player player) {
        if (player instanceof ServerPlayer serverPlayer) SwordThrow.clearCharge(serverPlayer);
    }

    private static Channel<CustomPacketPayload> createNetwork() {
        return ChannelBuilder.named(SwordThrow.id("network")).networkProtocolVersion(3)
                .payloadChannel().play()
                .serverbound(flow -> flow.addMain(ThrowActionPayload.ID, ThrowActionPayload.STREAM_CODEC.cast(),
                        (payload, context) -> {
                            ServerPlayer player = context.getSender();
                            if (player != null) SwordThrow.handleThrowAction(player, payload);
                        }))
                .clientbound().addMain(ThrowStatePayload.ID, ThrowStatePayload.STREAM_CODEC.cast(),
                        (payload, context) -> {
                            if (context.isClientSide()) SwordThrowForgeClient.handleThrowState(payload);
                        }).build();
    }

    static void sendToServer(ThrowActionPayload payload) {
        NETWORK.send(payload, PacketDistributor.SERVER.noArg());
    }

    private static final class ForgePlatformServices implements PlatformServices {
        private final DeferredRegister<EntityType<?>> entityTypes =
            DeferredRegister.create(Registries.ENTITY_TYPE, SwordThrow.MOD_ID);

        ForgePlatformServices(FMLJavaModLoadingContext context) {
            entityTypes.register(context.getModEventBus());
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
        public int getEnchantmentLevel(ItemStack stack, net.minecraft.core.Holder<Enchantment> enchantment) {
            return EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack);
        }

        @Override
        public SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, Entity entity) {
            return state.getSoundType(level, pos, entity);
        }

        @Override
        public void sendToPlayer(ServerPlayer target, ThrowStatePayload payload) {
            if (target.connection != null) {
                NETWORK.send(payload, PacketDistributor.PLAYER.with(target));
            }
        }

        @Override
        public void sendToTrackingAndSelf(ServerPlayer source, ThrowStatePayload payload) {
            NETWORK.send(payload, PacketDistributor.TRACKING_ENTITY_AND_SELF.with(source));
        }
    }

}
