package com.gyugaze.remonsterintegration.mixin.mca;

import com.gyugaze.remonsterintegration.mca.McaMilitiaService;
import net.conczin.mca.util.network.datasync.CDataParameter;
import net.conczin.mca.util.network.datasync.CTrackedEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps healthy armed MCA civilians out of the panic animation while militia combat owns the fight. */
@Mixin(targets = "net.conczin.mca.entity.ai.brain.VillagerBrain", remap = false)
public abstract class VillagerBrainMilitiaMixin {
    @Shadow @Final private Mob entity;
    @Shadow @Final private static CDataParameter<Boolean> PANICKING;

    @Inject(method = "isPanicking", at = @At("HEAD"), cancellable = true, remap = false)
    private void remonster$suppressHealthyMilitiaPanic(CallbackInfoReturnable<Boolean> cir) {
        if (McaMilitiaService.shouldSuppressPanic(entity)) {
            cir.setReturnValue(false);
        }
    }

    /** VillagerBrain#think writes the tracked PANICKING bit directly from Activity.PANIC. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Inject(method = "think", at = @At("TAIL"), remap = false)
    private void remonster$clearTrackedMilitiaPanic(CallbackInfo ci) {
        if (McaMilitiaService.shouldSuppressPanic(entity) && entity instanceof CTrackedEntity tracked) {
            tracked.setTrackedValue(PANICKING, false);
        }
    }
}
