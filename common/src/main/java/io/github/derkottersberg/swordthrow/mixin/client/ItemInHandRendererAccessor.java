package io.github.derkottersberg.swordthrow.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ItemInHandRenderer.class)
public interface ItemInHandRendererAccessor {
    @Invoker("renderPlayerArm")
    void swordthrow$renderPlayerArm(
        PoseStack poseStack,
        MultiBufferSource collector,
        int packedLight,
        float equipProgress,
        float swingProgress,
        HumanoidArm arm
    );
}
