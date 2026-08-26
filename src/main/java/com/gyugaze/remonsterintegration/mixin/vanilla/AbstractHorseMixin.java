package com.gyugaze.remonsterintegration.mixin.vanilla;

import com.gyugaze.remonsterintegration.subordinate.SubordinateMovementService;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Gives Tensura-named vanilla horses a lightweight FOLLOW/STAY adapter without replacing horse AI,
 * inventory, riding or saddle behavior. WANDER is intentionally just normal vanilla horse AI.
 */
@Mixin(AbstractHorse.class)
public abstract class AbstractHorseMixin {
    @Inject(method = "tick", at = @At("TAIL"), require = 0)
    private void remonster$tickNamedHorseMovement(CallbackInfo ci) {
        SubordinateMovementService.tickHorse((AbstractHorse) (Object) this);
    }
}
