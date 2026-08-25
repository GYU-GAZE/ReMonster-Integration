package com.gyugaze.remonsterintegration.mixin.mca;

import com.gyugaze.remonsterintegration.mca.McaGuardTargetPolicy;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.OptionalInt;

/**
 * MCA keeps all normal guard acquisition behind GuardEnemiesSensor#getPriority.
 * Injecting here preserves MCA's Brain/tasks while allowing project-owned per-entity exceptions.
 */
@Mixin(targets = "net.conczin.mca.entity.ai.brain.sensor.GuardEnemiesSensor", remap = false)
public abstract class GuardEnemiesSensorMixin {
    @Inject(method = "getPriority", at = @At("HEAD"), cancellable = true, remap = false)
    private static void remonster$overrideGuardPriority(
            LivingEntity target,
            LivingEntity guard,
            CallbackInfoReturnable<Integer> cir
    ) {
        OptionalInt override = McaGuardTargetPolicy.getPriorityOverride(target, guard);
        if (override.isPresent()) {
            cir.setReturnValue(override.getAsInt());
        }
    }
}
