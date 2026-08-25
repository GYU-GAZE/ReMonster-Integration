package com.gyugaze.remonsterintegration.mixin.tensura;

import com.gyugaze.remonsterintegration.subordinate.SubordinateMovementMode;
import com.gyugaze.remonsterintegration.subordinate.SubordinateMovementService;
import io.github.manasmods.tensura.util.SubordinateHelper;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets Thought Communication's existing calls to SubordinateHelper drive non-native subordinate AI.
 * Native Tensura ISubordinate entities fall through to upstream behavior unchanged.
 */
@Mixin(value = SubordinateHelper.class, remap = false)
public abstract class SubordinateHelperMixin {
    @Inject(method = "setFollow", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void remonster$follow(Mob target, CallbackInfo ci) {
        if (SubordinateMovementService.apply(target, SubordinateMovementMode.FOLLOW)) {
            ci.cancel();
        }
    }

    @Inject(method = "setWander", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void remonster$wander(Mob target, CallbackInfo ci) {
        if (SubordinateMovementService.apply(target, SubordinateMovementMode.WANDER)) {
            ci.cancel();
        }
    }

    @Inject(method = "setStay", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void remonster$stay(Mob target, CallbackInfo ci) {
        if (SubordinateMovementService.apply(target, SubordinateMovementMode.STAY)) {
            ci.cancel();
        }
    }
}
