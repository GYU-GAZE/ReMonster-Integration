package com.gyugaze.remonsterintegration.evolution;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

/** Data-driven physical representation of one semantic Re:Monster species. */
public record SpeciesBodyProfile(
        ResourceLocation species,
        double scale,
        Optional<TownsteadBody> townstead
) {
    public SpeciesBodyProfile {
        if (!Double.isFinite(scale) || scale <= 0.0D) {
            throw new IllegalArgumentException("Species body scale must be a finite positive number");
        }
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
