package com.gyugaze.remonsterintegration.mixin.tensura;

import io.github.manasmods.tensura.entity.ai.behaviour.path.SubordinateFollowOwner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Tensura's native subordinate follow behaviour uses speedMod 1.5 while common combat pursuit such
 * as Barghest uses 2.0. Keeping follow at the same navigation multiplier prevents fast monsters from
 * feeling artificially crippled immediately after Naming while retaining Tensura's own SmartBrain
 * follow/teleport implementation.
 */
@Mixin(value = SubordinateFollowOwner.class, remap = false)
public abstract class SubordinateFollowOwnerMixin {
    @ModifyConstant(
            method = "<init>",
            constant = @Constant(floatValue = 1.5F),
            remap = false,
            require = 0
    )
    private float remonster$useResponsiveFollowSpeed(float original) {
        return 2.0F;
    }
}
