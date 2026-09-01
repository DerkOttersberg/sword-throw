package io.github.derkottersberg.swordthrow.gameplay;

import io.github.derkottersberg.swordthrow.SwordThrow;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/** Stable data-pack hooks for controlling Sword Throw item behavior. */
public final class SwordThrowItemTags {
    public static final TagKey<Item> THROWABLE = create("throwable");
    public static final TagKey<Item> CANNOT_THROW = create("cannot_throw");
    public static final TagKey<Item> SPEARS = create("spears");
    public static final TagKey<Item> EMBEDDABLE = create("embeddable");

    private SwordThrowItemTags() {
    }

    private static TagKey<Item> create(String path) {
        return TagKey.create(Registries.ITEM, SwordThrow.id(path));
    }
}
