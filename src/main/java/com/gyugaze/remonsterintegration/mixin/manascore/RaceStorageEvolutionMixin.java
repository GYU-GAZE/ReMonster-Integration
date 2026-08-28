package com.gyugaze.remonsterintegration.mixin.manascore;

import com.gyugaze.remonsterintegration.evolution.TensuraRaceEvolutionBridge;
import io.github.manasmods.manascore.race.api.ManasRaceInstance;
import io.github.manasmods.manascore.race.impl.RaceStorage;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Mirrors every successful native ManasCore/Tensura race change into Re:Monster species/body state. */
@Mixin(value = RaceStorage.class, remap = false)
public abstract class RaceStorageEvolutionMixin {
    @Shadow
    protected abstract LivingEntity getOwner();

    @Inject(method = "setRace", at = @At("RETURN"), remap = false, require = 0)
    private void remonster$afterNativeRaceSet(
            ManasRaceInstance race,
            boolean evolution,
            boolean teleportToSpawn,
            @Nullable MutableComponent component,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!cir.getReturnValueZ() || race == null) {
            return;
        }
        TensuraRaceEvolutionBridge.afterNativeRaceSet(getOwner(), race.getRace(), evolution);
    }
}
