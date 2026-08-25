package com.gyugaze.remonsterintegration.profile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.gyugaze.remonsterintegration.ReMonsterIntegration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads data/*/mob_profiles/**/*.json into the shared MobProfileRegistry.
 */
public final class MobProfileReloadListener extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();
    private static final String DIRECTORY = "mob_profiles";

    public MobProfileReloadListener() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(
            Map<ResourceLocation, JsonElement> resources,
            ResourceManager resourceManager,
            ProfilerFiller profiler
    ) {
        Map<ResourceLocation, MobProfile> loaded = new LinkedHashMap<>();
        int missingEntities = 0;
        int invalidProfiles = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
            ResourceLocation sourceId = entry.getKey();

            try {
                JsonElement element = entry.getValue();
                if (!element.isJsonObject()) {
                    throw new IllegalArgumentException("Profile root must be a JSON object");
                }

                MobProfile profile = MobProfileParser.parse(sourceId, element.getAsJsonObject());
                ResourceLocation entityId = profile.entityId();

                if (!BuiltInRegistries.ENTITY_TYPE.containsKey(entityId)) {
                    missingEntities++;
                    ReMonsterIntegration.LOGGER.warn(
                            "Skipping Mob Profile {} because entity {} is not registered",
                            sourceId,
                            entityId
                    );
                    continue;
                }

                MobProfile previous = loaded.put(entityId, profile);
                if (previous != null) {
                    ReMonsterIntegration.LOGGER.warn(
                            "Mob Profile {} replaces an earlier profile for entity {}",
                            sourceId,
                            entityId
                    );
                }
            } catch (RuntimeException ex) {
                invalidProfiles++;
                ReMonsterIntegration.LOGGER.error("Failed to load Mob Profile {}", sourceId, ex);
            }
        }

        MobProfileRegistry.replaceAll(loaded);
        ReMonsterIntegration.LOGGER.info(
                "Loaded {} Mob Profiles ({} missing optional entities, {} invalid profiles)",
                loaded.size(),
                missingEntities,
                invalidProfiles
        );
    }
}
