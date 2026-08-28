package com.gyugaze.remonsterintegration.mixin.mobchampions;

import com.gyugaze.remonsterintegration.compat.MobChampionsUniversalPolicy;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/**
 * Keeps MobChampions authoritative for rank rolls, weights, stats, equipment, effects, loot and sync,
 * but removes its vanilla-heavy configured whitelist as a hard ceiling.
 */
@Mixin(targets = "mobchampions.event.MobChampionEventHandler", remap = false)
public abstract class MobChampionEventHandlerMixin {
    @Redirect(
            method = "addRandomChampion",
            at = @At(value = "INVOKE", target = "Ljava/util/List;contains(Ljava/lang/Object;)Z"),
            remap = false
    )
    private static boolean remonster$allowRandomChampion(
            List<?> configuredWhitelist,
            Object entityTypeId,
            LivingEntity entity
    ) {
        return MobChampionsUniversalPolicy.allowConfiguredOrUniversal(configuredWhitelist, entityTypeId, entity);
    }

    @Redirect(
            method = "addNormalMob",
            at = @At(value = "INVOKE", target = "Ljava/util/List;contains(Ljava/lang/Object;)Z"),
            remap = false
    )
    private static boolean remonster$trackUniversalCommonMob(
            List<?> configuredWhitelist,
            Object entityTypeId,
            LivingEntity entity
    ) {
        return MobChampionsUniversalPolicy.allowConfiguredOrUniversal(configuredWhitelist, entityTypeId, entity);
    }

    @Redirect(
            method = "updateChampionArrowDamage",
            at = @At(value = "INVOKE", target = "Ljava/util/List;contains(Ljava/lang/Object;)Z"),
            remap = false
    )
    private static boolean remonster$allowUniversalChampionProjectileDamage(
            List<?> configuredWhitelist,
            Object entityTypeId,
            LivingEntity entity,
            AbstractArrow arrow
    ) {
        return MobChampionsUniversalPolicy.allowConfiguredOrUniversal(configuredWhitelist, entityTypeId, entity);
    }
}
