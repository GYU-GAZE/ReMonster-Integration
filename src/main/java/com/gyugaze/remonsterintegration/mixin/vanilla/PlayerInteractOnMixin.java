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
 * Opens Re:Monster's compact subordinate menu only after the entire normal entity interaction chain
 * declined the click.
 *
 * Player#interactOn first lets the entity handle the held item/interaction and, when appropriate,
 * then lets ItemStack#interactLivingEntity run. Injecting at RETURN and requiring PASS means food,
 * healing items, dyes, chests, mod-specific tools, etc. retain priority automatically. An arbitrary
 * held item that neither the entity nor item understands can still open the subordinate menu.
 */
@Mixin(Player.class)
public abstract class PlayerInteractOnMixin {
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
