package io.github.derkottersberg.swordthrow.gameplay;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TridentItem;

/** Centralized, data-pack-aware item classification used on both logical sides. */
public final class ThrowItemRules {
    private ThrowItemRules() {
    }

    public static boolean canThrow(ItemStack stack) {
        return stack != null && decideThrowable(
            !stack.isEmpty(),
            stack.is(SwordThrowItemTags.CANNOT_THROW),
            stack.is(SwordThrowItemTags.THROWABLE),
            isVanillaTrident(stack)
        );
    }

    public static boolean isSpear(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.is(SwordThrowItemTags.SPEARS);
    }

    public static boolean canEmbed(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.is(SwordThrowItemTags.EMBEDDABLE);
    }

    private static boolean isVanillaTrident(ItemStack stack) {
        return stack.getItem() instanceof TridentItem || stack.is(Items.TRIDENT);
    }

    /** Pure precedence rule kept visible to package tests and data-pack integrations. */
    static boolean decideThrowable(
        boolean hasItem,
        boolean explicitlyBlocked,
        boolean explicitlyAllowed,
        boolean vanillaTrident
    ) {
        return hasItem && !explicitlyBlocked && (explicitlyAllowed || !vanillaTrident);
    }
}
