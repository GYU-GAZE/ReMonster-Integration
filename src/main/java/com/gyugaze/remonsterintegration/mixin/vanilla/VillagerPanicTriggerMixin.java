package com.gyugaze.remonsterintegration.mixin.vanilla;

import com.gyugaze.remonsterintegration.mca.McaMilitiaService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.VillagerPanicTrigger;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Armed healthy MCA civilians use militia combat instead of vanilla panic navigation. */
@Mixin(VillagerPanicTrigger.class)
public abstract class VillagerPanicTriggerMixin {
    @Inject(method = "start", at = @At("HEAD"), cancellable = true)
    private void remonster$skipArmedMcaPanicStart(
            ServerLevel level,
            Villager villager,
            long gameTime,
            CallbackInfo ci
    ) {
        if (McaMilitiaService.shouldOwnSelfDefence(villager)) {
            ci.cancel();
        }
    }

    @Inject(method = "canStillUse", at = @At("HEAD"), cancellable = true)
    private void remonster$stopArmedMcaPanic(
            ServerLevel level,
            Villager villager,
            long gameTime,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (McaMilitiaService.shouldOwnSelfDefence(villager)) {
            cir.setReturnValue(false);
        }
    }
}
