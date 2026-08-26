package com.gyugaze.remonsterintegration.evolution;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class EvolutionRegistry {
    private static volatile Map<ResourceLocation, EvolutionDefinition> BY_ID = Map.of();
    private static volatile Map<ResourceLocation, List<EvolutionDefinition>> BY_SOURCE = Map.of();

    private EvolutionRegistry() {
    }

    public static Optional<EvolutionDefinition> find(ResourceLocation from, ResourceLocation to) {
        return availableFrom(from).stream()
                .filter(definition -> definition.toSpecies().equals(to))
                .findFirst();
    }

    public static List<EvolutionDefinition> availableFrom(ResourceLocation from) {
        return BY_SOURCE.getOrDefault(from, List.of());
    }

    public static Map<ResourceLocation, EvolutionDefinition> all() {
        return BY_ID;
    }

    static void replaceAll(Map<ResourceLocation, EvolutionDefinition> definitions) {
        Map<ResourceLocation, EvolutionDefinition> byId = Map.copyOf(new LinkedHashMap<>(definitions));
        Map<ResourceLocation, List<EvolutionDefinition>> bySource = new LinkedHashMap<>();
        for (EvolutionDefinition definition : byId.values()) {
            bySource.computeIfAbsent(definition.fromSpecies(), ignored -> new ArrayList<>()).add(definition);
        }

        Map<ResourceLocation, List<EvolutionDefinition>> frozen = new LinkedHashMap<>();
        bySource.forEach((species, edges) -> {
            edges.sort(Comparator.comparing(edge -> edge.toSpecies().toString()));
            frozen.put(species, List.copyOf(edges));
        });

        BY_ID = byId;
        BY_SOURCE = Map.copyOf(frozen);
    }
}
