package com.gyugaze.remonsterintegration.evolution;

import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class SpeciesBodyProfileRegistry {
    private static volatile Map<ResourceLocation, SpeciesBodyProfile> ENTRIES = Map.of();
    private static volatile Map<ResourceLocation, ResourceLocation> ENTITY_TYPE_TO_SPECIES = Map.of();
    private static volatile Map<ResourceLocation, ResourceLocation> NATIVE_RACE_TO_SPECIES = Map.of();

    private SpeciesBodyProfileRegistry() {
    }

    public static Optional<SpeciesBodyProfile> get(ResourceLocation species) {
        return Optional.ofNullable(ENTRIES.get(species));
    }

    public static Optional<ResourceLocation> speciesForEntityType(ResourceLocation entityType) {
        return Optional.ofNullable(ENTITY_TYPE_TO_SPECIES.get(entityType));
    }

    public static Optional<ResourceLocation> speciesForNativeRace(ResourceLocation nativeRace) {
        return Optional.ofNullable(NATIVE_RACE_TO_SPECIES.get(nativeRace));
    }

    public static Optional<ResourceLocation> nativeRaceForSpecies(ResourceLocation species) {
        return get(species).flatMap(SpeciesBodyProfile::nativeRace);
    }

    public static Map<ResourceLocation, SpeciesBodyProfile> all() {
        return ENTRIES;
    }

    static void replaceAll(Map<ResourceLocation, SpeciesBodyProfile> entries) {
        Map<ResourceLocation, SpeciesBodyProfile> copied = Map.copyOf(new LinkedHashMap<>(entries));
        Map<ResourceLocation, ResourceLocation> entityReverse = new LinkedHashMap<>();
        Map<ResourceLocation, Boolean> ambiguousEntityTypes = new LinkedHashMap<>();
        Map<ResourceLocation, ResourceLocation> raceReverse = new LinkedHashMap<>();
        Map<ResourceLocation, Boolean> ambiguousNativeRaces = new LinkedHashMap<>();

        copied.values().forEach(profile -> {
            profile.entityBody().ifPresent(body -> {
                ResourceLocation existing = entityReverse.putIfAbsent(body.entityType(), profile.species());
                if (existing != null && !existing.equals(profile.species())) {
                    ambiguousEntityTypes.put(body.entityType(), true);
                }
            });
            profile.nativeRace().ifPresent(nativeRace -> {
                ResourceLocation existing = raceReverse.putIfAbsent(nativeRace, profile.species());
                if (existing != null && !existing.equals(profile.species())) {
                    ambiguousNativeRaces.put(nativeRace, true);
                }
            });
        });

        ambiguousEntityTypes.keySet().forEach(entityReverse::remove);
        ambiguousNativeRaces.keySet().forEach(raceReverse::remove);

        ENTRIES = copied;
        ENTITY_TYPE_TO_SPECIES = Map.copyOf(entityReverse);
        NATIVE_RACE_TO_SPECIES = Map.copyOf(raceReverse);
    }
}
