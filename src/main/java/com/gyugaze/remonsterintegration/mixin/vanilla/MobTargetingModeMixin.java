package com.gyugaze.remonsterintegration.mixin.vanilla;

import com.gyugaze.remonsterintegration.subordinate.SubordinateTargetingService;
import com.gyugaze.remonsterintegration.subordinate.UniversalSkillAiService;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Runs the lightweight universal subordinate adapters from the common Mob tick. */
@Mixin(Mob.class)
public abstract class MobTargetingModeMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void remonster$tickTargetingMode(CallbackInfo ci) {
        Mob mob = (Mob) (Object) this;
        SubordinateTargetingService.tickExternal(mob);
        UniversalSkillAiService.tick(mob);
    }
}
