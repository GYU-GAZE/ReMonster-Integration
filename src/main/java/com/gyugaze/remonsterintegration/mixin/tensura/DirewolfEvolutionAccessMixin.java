package com.gyugaze.remonsterintegration.mixin.tensura;

import io.github.manasmods.tensura.entity.variant.DirewolfVariant;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Exposes the exact private Tensura 2.0.1.2 Direwolf native branch selector to the graph adapter. */
@Mixin(targets = "io.github.manasmods.tensura.entity.monster.DirewolfEntity", remap = false)
public interface DirewolfEvolutionAccessMixin {
    @Invoker(value = "getEvolutionByBiomes", remap = false)
    DirewolfVariant remonster$invokeGetEvolutionByBiomes();
}
