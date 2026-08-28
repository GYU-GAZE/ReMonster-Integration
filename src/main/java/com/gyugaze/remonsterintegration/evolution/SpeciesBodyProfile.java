package com.gyugaze.remonsterintegration.evolution;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Data-driven physical/runtime representation of one Re:Monster species identity. */
public record SpeciesBodyProfile(
        ResourceLocation species,
        Optional<ResourceLocation> nativeRace,
        Optional<TownsteadBody> townstead,
        Optional<EntityBody> entityBody,
        Optional<NativeFormBody> nativeForm
) {
    public SpeciesBodyProfile {
        nativeRace = nativeRace == null ? Optional.empty() : nativeRace;
        townstead = townstead == null ? Optional.empty() : townstead;
        entityBody = entityBody == null ? Optional.empty() : entityBody;
        nativeForm = nativeForm == null ? Optional.empty() : nativeForm;

        int physicalBindings = (entityBody.isPresent() ? 1 : 0) + (nativeForm.isPresent() ? 1 : 0);
        if (physicalBindings > 1) {
            throw new IllegalArgumentException("A species cannot declare both entity_body and native_form");
        }
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

    /**
     * A semantic species represented by a form/variant/state on the creature's existing EntityType.
     *
     * The adapter id selects a narrow upstream integration (for example tensura:direwolf) and form is
     * the adapter-owned canonical form name. Re:Monster owns graph identity/eligibility while the
     * source mod remains the physical form-state authority.
     */
    public record NativeFormBody(ResourceLocation adapter, String form) {
        public NativeFormBody {
            if (adapter == null) {
                throw new IllegalArgumentException("Native form requires an adapter id");
            }
            if (form == null || form.isBlank()) {
                throw new IllegalArgumentException("Native form requires a non-empty form name");
            }
            form = form.trim().toLowerCase(Locale.ROOT);
        }
    }
}
