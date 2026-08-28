package com.gyugaze.remonsterintegration.mixin.mca;

import com.gyugaze.remonsterintegration.mca.McaMilitiaService;
import net.conczin.mca.util.network.datasync.CDataParameter;
import net.conczin.mca.util.network.datasync.CTrackedEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hides only the panic pose while a healthy armed civilian is actively fighting. */
@Mixin(targets = "net.conczin.mca.entity.ai.brain.VillagerBrain", remap = false)
public abstract class VillagerBrainMilitiaMixin {
    @Shadow @Final private Mob entity;
    @Shadow @Final private static CDataParameter<Boolean> PANICKING;

    /**
     * MCA intentionally uses its PANIC activity as part of civilian self-defence. Re:Monster leaves
     * that activity, navigation and melee behavior completely untouched and only clears the tracked
     * presentation flag while an armed healthy civilian is actively engaged. Low-health civilians
     * therefore regain MCA's normal visible panic automatically.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Inject(method = "think", at = @At("TAIL"), remap = false)
    private void remonster$hideHealthyArmedPanicPose(CallbackInfo ci) {
        if (McaMilitiaService.shouldSuppressPanic(entity) && entity instanceof CTrackedEntity tracked) {
            tracked.setTrackedValue(PANICKING, false);
        }
    }
}
