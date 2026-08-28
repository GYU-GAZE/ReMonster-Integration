package com.gyugaze.remonsterintegration.mixin.mca;

import com.gyugaze.remonsterintegration.mca.McaDialogueCompatibilityService;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * MCA Capitals and Townstead both redeclare VillagerEntityMCA#getTranslatable. In the target pack
 * MCA Capitals currently wins the overwrite conflict and Townstead's species voice is skipped.
 *
 * This lower-priority mixin injects after the upstream overwrites have been composed. It only
 * short-circuits when the compatibility service found either a real capital line or a real Townstead
 * species line; otherwise the already-authoritative MCA Capitals/MCA implementation continues.
 */
@Mixin(value = VillagerEntityMCA.class, priority = 900)
public abstract class VillagerEntityMcaDialogueCompatMixin {
    @Inject(
            method = "getTranslatable",
            at = @At("HEAD"),
            cancellable = true,
            require = 0,
            remap = false
    )
    private void remonster$composeTownsteadAndCapitalDialogue(
            Player target,
            String phraseId,
            Object[] params,
            CallbackInfoReturnable<MutableComponent> cir
    ) {
        MutableComponent resolved = McaDialogueCompatibilityService.resolve(
                (VillagerEntityMCA) (Object) this,
                target,
                phraseId,
                params
        );
        if (resolved != null) {
            cir.setReturnValue(resolved);
        }
    }
}
