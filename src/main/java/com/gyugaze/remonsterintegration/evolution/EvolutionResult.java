package com.gyugaze.remonsterintegration.evolution;

import net.minecraft.resources.ResourceLocation;

public record EvolutionResult(
        boolean success,
        ResourceLocation fromSpecies,
        ResourceLocation toSpecies,
        String bodyResult,
        String failure
) {
    public static EvolutionResult failure(ResourceLocation from, ResourceLocation to, String reason) {
        return new EvolutionResult(false, from, to, "unchanged", reason);
    }

    public static EvolutionResult success(ResourceLocation from, ResourceLocation to, String bodyResult) {
        return new EvolutionResult(true, from, to, bodyResult, "");
    }
}
