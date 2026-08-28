package com.gyugaze.remonsterintegration.evolution;

import net.minecraft.resources.ResourceLocation;

/** One directed edge in the data-driven Re:Monster species evolution graph. */
public record EvolutionDefinition(
        ResourceLocation id,
        ResourceLocation fromSpecies,
        ResourceLocation toSpecies,
        double minimumEp,
        EvolutionConditions conditions
) {
    public EvolutionDefinition {
        if (minimumEp < 0.0D) {
            throw new IllegalArgumentException("minimumEp cannot be negative");
        }
        conditions = conditions == null ? EvolutionConditions.NONE : conditions;
    }
}
