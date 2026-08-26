package com.gyugaze.remonsterintegration.evolution;

import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class SpeciesBodyProfileRegistry {
    private static volatile Map<ResourceLocation, SpeciesBodyProfile> ENTRIES = Map.of();

    private SpeciesBodyProfileRegistry() {
    }

    public static Optional<SpeciesBodyProfile> get(ResourceLocation species) {
        return Optional.ofNullable(ENTRIES.get(species));
    }

    public static Map<ResourceLocation, SpeciesBodyProfile> all() {
        return ENTRIES;
    }

    static void replaceAll(Map<ResourceLocation, SpeciesBodyProfile> entries) {
        ENTRIES = Map.copyOf(new LinkedHashMap<>(entries));
    }
}
