package com.gyugaze.remonsterintegration.mixin.tensura;

import com.gyugaze.remonsterintegration.tensura.TensuraDirewolfEvolutionCompat;
import io.github.manasmods.tensura.entity.monster.DirewolfEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Gives the Re:Monster evolution graph sole authority over Direwolf evolution timing/branching while
 * retaining Tensura's real in-place evolve() implementation for graph-approved transitions.
 */
@Mixin(value = DirewolfEntity.class, remap = false)
public abstract class DirewolfEvolutionAuthorityMixin {
    @Inject(method = "evolve", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void remonster$gateNativeDirewolfEvolution(CallbackInfo ci) {
        if (!TensuraDirewolfEvolutionCompat.isNativeEvolutionInvocationAllowed()) {
            ci.cancel();
        }
    }
}
