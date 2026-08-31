package io.github.derkottersberg.swordthrow.forge;

import io.github.derkottersberg.swordthrow.SwordThrow;
import io.github.derkottersberg.swordthrow.internal.PlatformServices;
import io.github.derkottersberg.swordthrow.network.ThrowActionPayload;
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
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

@Mod(SwordThrow.MOD_ID)
public final class SwordThrowForge {
    private static final String PROTOCOL_VERSION = "2";
    private static final Channel<CustomPacketPayload> NETWORK = createNetwork();

    public SwordThrowForge(FMLJavaModLoadingContext context) {
        SwordThrow.initialize(new ForgePlatformServices(context));
        PlayerEvent.PlayerLoggedOutEvent.BUS.addListener(event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                SwordThrow.clearCharge(player);
            }
        });
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
    }
}
