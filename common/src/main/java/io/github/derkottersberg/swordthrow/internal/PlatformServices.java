package io.github.derkottersberg.swordthrow.internal;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

/** Loader-owned registrations used by the shared bootstrap. */
public interface PlatformServices {
    String loaderName();

    <T extends Entity> RegistryHandle<EntityType<T>> registerEntityType(
        String path,
        Supplier<EntityType<T>> factory
    );

    int getEnchantmentLevel(ItemStack stack, Holder<Enchantment> enchantment);

    SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, Entity entity);

    @FunctionalInterface
    interface RegistryHandle<T> extends Supplier<T> {
    }
}
