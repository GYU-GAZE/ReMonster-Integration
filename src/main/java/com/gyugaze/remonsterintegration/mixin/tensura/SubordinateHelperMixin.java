package com.gyugaze.remonsterintegration.mixin.tensura;

import com.gyugaze.remonsterintegration.subordinate.SubordinateMovementMode;
import com.gyugaze.remonsterintegration.subordinate.SubordinateMovementService;
import com.gyugaze.remonsterintegration.subordinate.SubordinateTargetingMode;
import com.gyugaze.remonsterintegration.subordinate.SubordinateTargetingService;
import com.gyugaze.remonsterintegration.tensura.TensuraSubordinateBridge;
import io.github.manasmods.tensura.entity.template.subclass.ISubordinate;
import io.github.manasmods.tensura.util.SubordinateHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets Thought Communication's existing SubordinateHelper calls drive ownership-only subordinates.
 * Native Tensura ISubordinate entities always fall through to upstream behavior unchanged.
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

    @Inject(method = "setNeutral", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void remonster$neutral(Mob target, CallbackInfo ci) {
        if (applyExternalTargeting(target, SubordinateTargetingMode.NEUTRAL)) {
            ci.cancel();
        }
    }

    @Inject(method = "setPassive", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void remonster$passive(Mob target, CallbackInfo ci) {
        if (applyExternalTargeting(target, SubordinateTargetingMode.PASSIVE)) {
            ci.cancel();
        }
    }

    @Inject(method = "setAggressive", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void remonster$aggressive(LivingEntity target, CallbackInfo ci) {
        if (target instanceof Mob mob && applyExternalTargeting(mob, SubordinateTargetingMode.AGGRESSIVE)) {
            ci.cancel();
        }
    }

    @Inject(method = "setProtect", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void remonster$protect(Mob target, CallbackInfo ci) {
        if (applyExternalTargeting(target, SubordinateTargetingMode.PROTECT)) {
            ci.cancel();
        }
    }

    private static boolean applyExternalTargeting(Mob target, SubordinateTargetingMode mode) {
        if (target instanceof ISubordinate || TensuraSubordinateBridge.getResolvedOwnerUuid(target).isEmpty()) {
            return false;
        }
        return SubordinateTargetingService.setMode(target, mode);
    }
}
