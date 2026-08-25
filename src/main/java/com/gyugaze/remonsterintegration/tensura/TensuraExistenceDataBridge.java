package com.gyugaze.remonsterintegration.tensura;

import io.github.manasmods.tensura.data.existence.EntityExistenceData;
import io.github.manasmods.tensura.registry.data.TensuraCustomData;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Read-only access to Tensura's data-driven entity_existence registry.
 *
 * This is deliberately kept separate from runtime SkillStorage: entity_existence describes the
 * species/data definition while runtime storage describes what this exact individual currently has.
 */
public final class TensuraExistenceDataBridge {
    private TensuraExistenceDataBridge() {
    }

    public static Optional<EntityExistenceData> get(LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        Registry<EntityExistenceData> registry = entity.level().registryAccess()
                .registry(TensuraCustomData.ENTITY_EXISTENCE)
                .orElse(null);
        if (registry == null) {
            return Optional.empty();
        }
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return Optional.ofNullable(registry.get(entityId));
    }

    public static List<ResourceLocation> getGuaranteedAbilities(LivingEntity entity) {
        return get(entity)
                .flatMap(EntityExistenceData::abilities)
                .orElse(List.of())
                .stream()
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .toList();
    }

    public static List<RandomAbility> getRandomAbilities(LivingEntity entity) {
        Map<ResourceLocation, Double> random = get(entity)
                .flatMap(EntityExistenceData::abilitiesRandom)
                .orElse(Map.of());
        List<RandomAbility> result = new ArrayList<>(random.size());
        random.forEach((skill, chance) -> result.add(new RandomAbility(skill, chance)));
        result.sort(Comparator.comparing(value -> value.skill().toString()));
        return List.copyOf(result);
    }

    public record RandomAbility(ResourceLocation skill, double chance) {
    }
}
