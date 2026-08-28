package com.gyugaze.remonsterintegration.mixin.bettersubs;

import io.github.manasmods.tensura.data.TensuraEntityTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes Better Subordinates' native ExpandedSkillStorage available to any non-player living mob that
 * can later become a Re:Monster subordinate.
 *
 * Better Subordinates normally attaches this storage only to OwnableEntity instances. That misses MCA
 * villagers and many modded tameable/commandable mobs because Re:Monster ownership is intentionally
 * represented through Tensura's SubordinateHelper contract instead of forcing every integration target
 * to implement vanilla pet interfaces.
 *
 * Storage attachment itself is intentionally broad and behavior remains narrow: Better Subordinates'
 * own kill, damage and EP handlers already require SubordinateHelper to resolve an owner before they
 * mutate these counters. Wild mobs therefore carry only dormant storage until/if another system makes
 * them a real subordinate. Tensura clone entities keep Better Subordinates' native exclusion.
 *
 * This does not install TensuraTamableEntity AI or replace any entity Brain/schedule.
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
    private static void remonster$allowUniversalLivingStorage(
            Entity entity,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (entity instanceof LivingEntity
                && !(entity instanceof Player)
                && !entity.getType().is(TensuraEntityTags.CLONES)) {
            cir.setReturnValue(true);
        }
    }
}
