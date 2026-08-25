package com.gyugaze.remonsterintegration.profile;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class MobProfileRegistry {
    private static volatile Map<ResourceLocation, MobProfile> profiles = Map.of();

    private MobProfileRegistry() {
    }

    public static Optional<MobProfile> get(ResourceLocation entityId) {
        return Optional.ofNullable(profiles.get(entityId));
    }

    public static Optional<MobProfile> get(LivingEntity entity) {
        ResourceLocation entityId = EntityType.getKey(entity.getType());
        return get(entityId);
    }

    public static Map<ResourceLocation, MobProfile> all() {
        return profiles;
    }

    public static void replaceAll(Map<ResourceLocation, MobProfile> loadedProfiles) {
        profiles = Collections.unmodifiableMap(new LinkedHashMap<>(loadedProfiles));
    }

    public static void clear() {
        profiles = Map.of();
    }
}
