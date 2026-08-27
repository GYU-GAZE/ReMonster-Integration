package com.gyugaze.remonsterintegration.mixin.mca;

import com.gyugaze.remonsterintegration.mca.McaCombatWeaponPolicy;
import com.gyugaze.remonsterintegration.mca.McaSubordinateAdapter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Ordinary MCA civilians cannot deal vanilla bare-hand melee damage. Guards remain untouched. */
@Mixin(targets = "net.conczin.mca.entity.VillagerEntityMCA", remap = false)
public abstract class VillagerEntityMcaCombatMixin {
    @Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true)
    private void remonster$requireCivilianMeleeWeapon(Entity target, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!McaSubordinateAdapter.isGuard(self)
                && !McaCombatWeaponPolicy.hasDamagingMeleeWeaponInMainHand(self)) {
            cir.setReturnValue(false);
        }
    }
}
