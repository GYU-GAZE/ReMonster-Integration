package com.gyugaze.remonsterintegration.evolution;

import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class SpeciesBodyProfileRegistry {
    private static volatile Map<ResourceLocation, SpeciesBodyProfile> ENTRIES = Map.of();
    private static volatile Map<ResourceLocation, ResourceLocation> ENTITY_TYPE_TO_SPECIES = Map.of();

    private SpeciesBodyProfileRegistry() {
    }

    public static Optional<SpeciesBodyProfile> get(ResourceLocation species) {
        return Optional.ofNullable(ENTRIES.get(species));
    }

    public static Optional<ResourceLocation> speciesForEntityType(ResourceLocation entityType) {
        return Optional.ofNullable(ENTITY_TYPE_TO_SPECIES.get(entityType));
    }

    public static Map<ResourceLocation, SpeciesBodyProfile> all() {
        return ENTRIES;
    }

    static void replaceAll(Map<ResourceLocation, SpeciesBodyProfile> entries) {
        Map<ResourceLocation, SpeciesBodyProfile> copied = Map.copyOf(new LinkedHashMap<>(entries));
        Map<ResourceLocation, ResourceLocation> reverse = new LinkedHashMap<>();
        Map<ResourceLocation, Boolean> ambiguous = new LinkedHashMap<>();

        copied.values().forEach(profile -> profile.entityBody().ifPresent(body -> {
            ResourceLocation existing = reverse.putIfAbsent(body.entityType(), profile.species());
            if (existing != null && !existing.equals(profile.species())) {
                ambiguous.put(body.entityType(), true);
            }
        }));
        ambiguous.keySet().forEach(reverse::remove);

        ENTRIES = copied;
        ENTITY_TYPE_TO_SPECIES = Map.copyOf(reverse);
    }
}
