package com.gyugaze.remonsterintegration.mixin.tensura;

import io.github.manasmods.tensura.entity.template.TensuraMountEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gives Tensura mounts the same interaction split used by vanilla horses:
 * normal interaction keeps riding/item behavior, while secondary-use opens the native mount inventory.
 *
 * Tensura 2.0.1.2 normally lets an owned rideable mount consume the interaction before its
 * secondary-use branch, and a secondary-use PASS would fall through to command cycling. Re:Monster
 * already exposes movement commands in the companion chrome, so the mount inventory gets secondary-use
 * instead without replacing MountScreen or MountMenu.
 */
@Mixin(TensuraMountEntity.class)
public abstract class TensuraMountInventoryInteractionMixin {
    @Inject(method = "getRidingInteraction", at = @At("HEAD"), cancellable = true)
    private void remonster$openInventoryOnSecondaryUse(
            Player player,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (!player.isSecondaryUseActive()) {
            return;
        }

        TensuraMountEntity mount = (TensuraMountEntity) (Object) this;
        if (!mount.isTame() || !mount.isOwnedBy(player)) {
            return;
        }

        mount.openCustomInventoryScreen(player);
        cir.setReturnValue(InteractionResult.sidedSuccess(mount.level().isClientSide()));
    }
}
