package com.gyugaze.remonsterintegration.mixin.mca;

import com.gyugaze.remonsterintegration.mca.McaMilitiaService;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps healthy, armed MCA civilians from entering panic while the militia layer is engaged. */
@Mixin(targets = "net.conczin.mca.entity.ai.brain.VillagerBrain", remap = false)
public abstract class VillagerBrainMilitiaMixin {
    @Shadow @Final private Mob entity;

    @Inject(method = "isPanicking", at = @At("HEAD"), cancellable = true, remap = false)
    private void remonster$suppressHealthyMilitiaPanic(CallbackInfoReturnable<Boolean> cir) {
        if (McaMilitiaService.shouldSuppressPanic(entity)) {
            cir.setReturnValue(false);
        }
    }
}
