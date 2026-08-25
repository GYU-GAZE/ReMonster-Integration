package com.gyugaze.remonsterintegration.mixin.vanilla;

import com.gyugaze.remonsterintegration.subordinate.SubordinateMovementService;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes explicit Re:Monster WANDER/STAY commands meaningful for vanilla-style pets. Pets that were
 * never commanded by Re:Monster keep vanilla behavior unchanged.
 */
@Mixin(FollowOwnerGoal.class)
public abstract class FollowOwnerGoalMixin {
    @Shadow @Final private TamableAnimal tamable;

    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
    private void remonster$respectMovementModeOnStart(CallbackInfoReturnable<Boolean> cir) {
        if (!SubordinateMovementService.allowsVanillaFollow(tamable)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "canContinueToUse", at = @At("HEAD"), cancellable = true)
    private void remonster$respectMovementModeWhileFollowing(CallbackInfoReturnable<Boolean> cir) {
        if (!SubordinateMovementService.allowsVanillaFollow(tamable)) {
            cir.setReturnValue(false);
        }
    }
}
