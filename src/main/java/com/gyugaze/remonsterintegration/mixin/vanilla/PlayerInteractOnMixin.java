package com.gyugaze.remonsterintegration.mixin.vanilla;

import com.gyugaze.remonsterintegration.menu.SubordinateMenuEvents;
import com.gyugaze.remonsterintegration.menu.SubordinateMenuPolicy;
import com.gyugaze.remonsterintegration.tensura.TensuraSubordinateBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Opens Re:Monster's compact subordinate menu for mobs that do not own a native screen.
 *
 * Secondary-use is the explicit Re:Monster control gesture for fallback entities. It is intercepted
 * before vanilla/mod interactions so tame animals cannot consume it as sit/stand and Tensura's
 * Thought Communication interaction cannot swallow it first. Normal right-clicks still preserve the
 * complete entity/item interaction chain and only use the fallback when that chain returns PASS.
 */
@Mixin(Player.class)
public abstract class PlayerInteractOnMixin {
    @Inject(method = "interactOn", at = @At("HEAD"), cancellable = true)
    private void remonster$openFallbackOnSecondaryUse(
            Entity target,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        Player player = (Player) (Object) this;
        if (!player.isSecondaryUseActive()) {
            return;
        }
        if (!(target instanceof LivingEntity living) || !(living instanceof Mob)) {
            return;
        }
        if (!TensuraSubordinateBridge.isSubordinateOf(player, living)) {
            return;
        }
        if (!SubordinateMenuPolicy.shouldUseFallbackMenu(living)) {
            return;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            SubordinateMenuEvents.openFallback(serverPlayer, living);
        }
        cir.setReturnValue(InteractionResult.SUCCESS);
    }

    @Inject(method = "interactOn", at = @At("RETURN"), cancellable = true)
    private void remonster$openFallbackAfterUnhandledInteraction(
            Entity target,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (cir.getReturnValue() != InteractionResult.PASS) {
            return;
        }

        Player player = (Player) (Object) this;
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (player.isSecondaryUseActive()) {
            return;
        }
        if (!(target instanceof LivingEntity living) || !(living instanceof Mob)) {
            return;
        }
        if (!TensuraSubordinateBridge.isSubordinateOf(player, living)) {
            return;
        }
        if (!SubordinateMenuPolicy.shouldUseFallbackMenu(living)) {
            return;
        }

        SubordinateMenuEvents.openFallback(serverPlayer, living);
        cir.setReturnValue(InteractionResult.SUCCESS);
    }
}
