package io.github.derkottersberg.swordthrow.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(FirstPersonHandsAndItemsRenderer.class)
public interface ItemInHandRendererAccessor {
    @Invoker("renderPlayerArm")
    void swordthrow$renderPlayerArm(
        PoseStack poseStack,
        SubmitNodeCollector collector,
        int packedLight,
        float equipProgress,
        float swingProgress,
        HumanoidArm arm,
        PlayerRenderState player
    );
}
