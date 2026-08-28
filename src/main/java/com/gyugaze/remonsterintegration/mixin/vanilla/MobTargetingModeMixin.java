package com.gyugaze.remonsterintegration.mixin.vanilla;

import com.gyugaze.remonsterintegration.evolution.NaturalEvolutionService;
import com.gyugaze.remonsterintegration.growth.ExternalEnergyFoundationService;
import com.gyugaze.remonsterintegration.growth.UniversalCharacterGrowthService;
import com.gyugaze.remonsterintegration.learning.NpcSkillTeachingService;
import com.gyugaze.remonsterintegration.mca.McaMilitiaService;
import com.gyugaze.remonsterintegration.subordinate.SubordinateTargetingService;
import com.gyugaze.remonsterintegration.subordinate.UniversalSkillAiService;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Runs lightweight universal compatibility adapters from the common Mob tick. */
@Mixin(Mob.class)
public abstract class MobTargetingModeMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void remonster$tickUniversalAdapters(CallbackInfo ci) {
        Mob mob = (Mob) (Object) this;
        SubordinateTargetingService.tickExternal(mob);
        McaMilitiaService.tick(mob);
        UniversalCharacterGrowthService.tick(mob);
        ExternalEnergyFoundationService.tick(mob);
        NpcSkillTeachingService.tick(mob);
        UniversalSkillAiService.tick(mob);
        NaturalEvolutionService.tick(mob);
    }
}
