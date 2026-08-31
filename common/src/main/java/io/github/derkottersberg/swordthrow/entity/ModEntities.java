package io.github.derkottersberg.swordthrow.entity;

import io.github.derkottersberg.swordthrow.SwordThrow;
import io.github.derkottersberg.swordthrow.internal.PlatformServices;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
    private static PlatformServices.RegistryHandle<EntityType<ThrownSwordEntity>> thrownSword;

    private ModEntities() {
    }

    public static void initialize(PlatformServices services) {
        if (thrownSword != null) {
            throw new IllegalStateException("Sword Throw entity types were initialized twice");
        }

        thrownSword = services.registerEntityType("thrown_sword", () ->
            EntityType.Builder.<ThrownSwordEntity>of(ThrownSwordEntity::new, MobCategory.MISC)
                .sized(0.5F, 0.5F)
                .clientTrackingRange(6)
                .updateInterval(2)
                .build(ResourceKey.create(Registries.ENTITY_TYPE, SwordThrow.id("thrown_sword")))
        );
    }

    public static PlatformServices.RegistryHandle<EntityType<ThrownSwordEntity>> thrownSwordHandle() {
        if (thrownSword == null) {
            throw new IllegalStateException("Sword Throw entity type requested before initialization");
        }
        return thrownSword;
    }

    public static EntityType<ThrownSwordEntity> thrownSword() {
        return thrownSwordHandle().get();
    }
}
