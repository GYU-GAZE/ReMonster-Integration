package com.gyugaze.remonsterintegration.mixin.tensura;

import com.gyugaze.remonsterintegration.compat.MobChampionProgressionService;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adds MobChampions rank value to Tensura's native kill-EP calculation without awarding EP twice. */
@Mixin(value = EnergyHelper.class, remap = false)
public abstract class EnergyHelperChampionMixin {
    @Inject(
            method = "getEPGain(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;Z)D",
            at = @At("RETURN"),
            cancellable = true,
            remap = false,
            require = 0
    )
    private static void remonster$multiplyChampionEpReward(
            LivingEntity defeated,
            LivingEntity killer,
            boolean checkSpawn,
            CallbackInfoReturnable<Double> cir
    ) {
        double reward = cir.getReturnValue();
        if (reward <= 0.0D) {
            return;
        }
        double multiplier = MobChampionProgressionService.epRewardMultiplier(defeated);
        if (multiplier != 1.0D) {
            cir.setReturnValue(reward * multiplier);
        }
    }
}
