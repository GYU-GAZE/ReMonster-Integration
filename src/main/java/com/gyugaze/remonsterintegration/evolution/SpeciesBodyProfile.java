package com.gyugaze.remonsterintegration.evolution;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

/** Data-driven physical/runtime representation of one Re:Monster species identity. */
public record SpeciesBodyProfile(
        ResourceLocation species,
        Optional<ResourceLocation> nativeRace,
        Optional<TownsteadBody> townstead,
        Optional<EntityBody> entityBody
) {
    public SpeciesBodyProfile {
        nativeRace = nativeRace == null ? Optional.empty() : nativeRace;
        townstead = townstead == null ? Optional.empty() : townstead;
        entityBody = entityBody == null ? Optional.empty() : entityBody;
    }

    public record TownsteadBody(
            List<ResourceLocation> rootCandidates,
            List<String> rootSearchTerms,
            boolean preserveSharedLoci,
            boolean clampBodyMetrics,
            double scale
    ) {
        public TownsteadBody {
            rootCandidates = rootCandidates == null ? List.of() : List.copyOf(rootCandidates);
            rootSearchTerms = rootSearchTerms == null ? List.of() : List.copyOf(rootSearchTerms);
            if (!Double.isFinite(scale) || scale <= 0.0D) {
                throw new IllegalArgumentException("Townstead body scale must be a finite positive number");
            }
        }
    }

    /** Controlled replacement body for non-player/non-MCA living entities. */
    public record EntityBody(
            ResourceLocation entityType,
            boolean preserveEquipment,
            boolean reinforceBaseStats
    ) {
        public EntityBody {
            if (entityType == null) {
                throw new IllegalArgumentException("Entity evolution body requires an entity type");
            }
        }
    }
}
