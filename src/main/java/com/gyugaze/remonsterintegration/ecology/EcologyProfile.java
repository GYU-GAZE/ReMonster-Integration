package com.gyugaze.remonsterintegration.ecology;

import java.util.Set;

public record EcologyProfile(
        boolean naturalSpawn,
        Set<Habitat> habitats,
        SpawnRarity rarity,
        int weight,
        int groupMin,
        int groupMax
) {
    public EcologyProfile {
        habitats = Set.copyOf(habitats);

        if (weight < 0) {
            throw new IllegalArgumentException("Spawn weight cannot be negative");
        }
        if (groupMin < 1) {
            throw new IllegalArgumentException("groupMin must be at least 1");
        }
        if (groupMax < groupMin) {
            throw new IllegalArgumentException("groupMax cannot be smaller than groupMin");
        }
    }
}
