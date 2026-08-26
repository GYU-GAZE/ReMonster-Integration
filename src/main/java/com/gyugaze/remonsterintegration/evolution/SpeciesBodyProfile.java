package com.gyugaze.remonsterintegration.evolution;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

/** Data-driven physical representation of one semantic Re:Monster species. */
public record SpeciesBodyProfile(
        ResourceLocation species,
        Optional<TownsteadBody> townstead
) {
    public SpeciesBodyProfile {
        townstead = townstead == null ? Optional.empty() : townstead;
    }

    public record TownsteadBody(
            List<ResourceLocation> rootCandidates,
            List<String> rootSearchTerms,
            boolean preserveSharedLoci,
            boolean clampBodyMetrics
    ) {
        public TownsteadBody {
            rootCandidates = rootCandidates == null ? List.of() : List.copyOf(rootCandidates);
            rootSearchTerms = rootSearchTerms == null ? List.of() : List.copyOf(rootSearchTerms);
        }
    }
}
