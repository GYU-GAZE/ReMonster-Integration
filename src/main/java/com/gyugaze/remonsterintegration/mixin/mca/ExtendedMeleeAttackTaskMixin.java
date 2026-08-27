package com.gyugaze.remonsterintegration.mixin.mca;

import com.gyugaze.remonsterintegration.mca.McaMilitiaService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Stops MCA's panic self-defence melee task from fighting in parallel with Re:Monster militia AI. */
@Mixin(targets = "net.conczin.mca.entity.ai.brain.tasks.ExtendedMeleeAttackTask", remap = false)
public abstract class ExtendedMeleeAttackTaskMixin {
    @Inject(
            method = "checkExtraStartConditions(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Mob;)Z",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void remonster$preferMilitiaCombat(
            ServerLevel level,
            Mob mob,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (McaMilitiaService.shouldOwnSelfDefence(mob)) {
            cir.setReturnValue(false);
        }
    }
}
