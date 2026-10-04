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
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.simple.SimpleChannel;
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
    private static final SimpleChannel NETWORK = createNetwork();

    public SwordThrowForge() {
        FMLJavaModLoadingContext context = FMLJavaModLoadingContext.get();
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

    private static SimpleChannel createNetwork() {
        SimpleChannel channel = NetworkRegistry.newSimpleChannel(SwordThrow.id("network"),
            () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);
        channel.registerMessage(0, ThrowActionPayload.class,
            (payload, buffer) -> ThrowActionPayload.STREAM_CODEC.encode(buffer, payload),
            ThrowActionPayload.STREAM_CODEC::decode, (payload, supplier) -> {
                var context = supplier.get();
                context.enqueueWork(() -> {
                    ServerPlayer player = context.getSender();
                    if (player != null) SwordThrow.handleThrowAction(player, payload);
                });
                context.setPacketHandled(true);
            }, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        channel.registerMessage(1, ThrowStatePayload.class,
            (payload, buffer) -> ThrowStatePayload.STREAM_CODEC.encode(buffer, payload),
            ThrowStatePayload.STREAM_CODEC::decode, (payload, supplier) -> {
                var context = supplier.get();
                context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> SwordThrowForgeClient.handleThrowState(payload)));
                context.setPacketHandled(true);
            }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        return channel;
    }

    static void sendToServer(ThrowActionPayload payload) {
        NETWORK.sendToServer(payload);
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
        public int getEnchantmentLevel(ItemStack stack, Enchantment enchantment) {
            return EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack);
        }

        @Override
        public SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, Entity entity) {
            return state.getSoundType(level, pos, entity);
        }

        @Override
        public void sendToPlayer(ServerPlayer target, ThrowStatePayload payload) {
            if (target.connection != null) {
                NETWORK.send(PacketDistributor.PLAYER.with(() -> target), payload);
            }
        }

        @Override
        public void sendToTrackingAndSelf(ServerPlayer source, ThrowStatePayload payload) {
            NETWORK.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> source), payload);
        }
    }

}
