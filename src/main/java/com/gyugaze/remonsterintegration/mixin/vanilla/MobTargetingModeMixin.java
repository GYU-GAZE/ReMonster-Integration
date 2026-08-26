package com.gyugaze.remonsterintegration.mixin.vanilla;

import com.gyugaze.remonsterintegration.subordinate.SubordinateTargetingService;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Runs the lightweight targeting compatibility layer only for non-native mobs with an explicit mode. */
@Mixin(Mob.class)
public abstract class MobTargetingModeMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void remonster$tickTargetingMode(CallbackInfo ci) {
        SubordinateTargetingService.tickExternal((Mob) (Object) this);
    }
}
