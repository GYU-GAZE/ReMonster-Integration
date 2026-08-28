package com.gyugaze.remonsterintegration.evolution;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Optional reusable conditions attached to one evolution edge. */
public record EvolutionConditions(
        List<ResourceLocation> requiredSkills,
        List<ResourceLocation> forbiddenSkills,
        List<ResourceLocation> anyBiomeTags,
        List<ResourceLocation> forbiddenBiomeTags,
        List<ResourceLocation> dimensions,
        List<ResourceLocation> forbiddenDimensions,
        OwnershipRequirement ownership,
        Integer minBlockLight,
        Integer maxBlockLight,
        Integer minY,
        Integer maxY
) {
    public static final EvolutionConditions NONE = new EvolutionConditions(
            List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
            OwnershipRequirement.ANY, null, null, null, null
    );

    public EvolutionConditions {
        requiredSkills = requiredSkills == null ? List.of() : List.copyOf(requiredSkills);
        forbiddenSkills = forbiddenSkills == null ? List.of() : List.copyOf(forbiddenSkills);
        anyBiomeTags = anyBiomeTags == null ? List.of() : List.copyOf(anyBiomeTags);
        forbiddenBiomeTags = forbiddenBiomeTags == null ? List.of() : List.copyOf(forbiddenBiomeTags);
        dimensions = dimensions == null ? List.of() : List.copyOf(dimensions);
        forbiddenDimensions = forbiddenDimensions == null ? List.of() : List.copyOf(forbiddenDimensions);
        ownership = ownership == null ? OwnershipRequirement.ANY : ownership;
        validateLight(minBlockLight, "minBlockLight");
        validateLight(maxBlockLight, "maxBlockLight");
        if (minBlockLight != null && maxBlockLight != null && minBlockLight > maxBlockLight) {
            throw new IllegalArgumentException("minBlockLight cannot exceed maxBlockLight");
        }
        if (minY != null && maxY != null && minY > maxY) {
            throw new IllegalArgumentException("minY cannot exceed maxY");
        }
    }

    private static void validateLight(Integer light, String name) {
        if (light != null && (light < 0 || light > 15)) {
            throw new IllegalArgumentException(name + " must be between 0 and 15");
        }
    }

    public enum OwnershipRequirement {
        ANY,
        WILD,
        OWNED
    }
}
