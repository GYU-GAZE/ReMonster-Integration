package com.gyugaze.remonsterintegration.mixin.bettersubs;

import com.gyugaze.remonsterintegration.mca.McaSubordinateAdapter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Better Subordinates attaches ExpandedSkillStorage only to OwnableEntity instances.
 *
 * MCA villagers deliberately remain VillagerEntityMCA and must not be forced to implement the
 * vanilla pet ownership contract just to receive subordinate progression. Tensura already resolves
 * their project-assigned permanent owner through SubordinateHelper, so the only missing piece is
 * allowing Better Subordinates to attach its native progression storage to the villager.
 *
 * This mixin changes only that storage-registration predicate. It does not apply Better Subordinates'
 * TensuraTamableEntity combat AI (including Haki automation) to MCA villagers and therefore leaves
 * MCA's Brain, schedule, family, profession and Townstead state untouched.
 */
@Pseudo
@Mixin(targets = "com.trbeyond.tensura_better_subs.entity.ExpandedSkillStorage", remap = false)
public abstract class ExpandedSkillStorageMixin {
    @Inject(
            method = "lambda$init$0",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 0
    )
    private static void remonster$allowMcaVillagerStorage(
            Entity entity,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (entity instanceof LivingEntity living && McaSubordinateAdapter.isMcaVillager(living)) {
            cir.setReturnValue(true);
        }
    }
}
