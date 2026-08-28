package com.gyugaze.remonsterintegration.evolution;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.gyugaze.remonsterintegration.ReMonsterIntegration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class EvolutionReloadListener extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();
    private static final String DIRECTORY = "remonster_evolutions";

    public EvolutionReloadListener() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(
            Map<ResourceLocation, JsonElement> resources,
            ResourceManager resourceManager,
            ProfilerFiller profiler
    ) {
        Map<ResourceLocation, EvolutionDefinition> loaded = new LinkedHashMap<>();
        int invalid = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
            try {
                EvolutionDefinition definition = parse(entry.getKey(), entry.getValue());
                loaded.put(entry.getKey(), definition);
            } catch (RuntimeException ex) {
                invalid++;
                ReMonsterIntegration.LOGGER.error("Failed to load evolution definition {}", entry.getKey(), ex);
            }
        }

        EvolutionRegistry.replaceAll(loaded);
        ReMonsterIntegration.LOGGER.info(
                "Loaded {} Re:Monster evolution definitions ({} invalid)",
                loaded.size(),
                invalid
        );
    }

    private static EvolutionDefinition parse(ResourceLocation sourceId, JsonElement element) {
        if (!element.isJsonObject()) {
            throw new IllegalArgumentException("Evolution definition root must be a JSON object");
        }
        JsonObject object = element.getAsJsonObject();
        ResourceLocation from = requireId(object, "from", sourceId);
        ResourceLocation to = requireId(object, "to", sourceId);
        double minimumEp = object.has("minimum_ep") ? object.get("minimum_ep").getAsDouble() : 0.0D;
        EvolutionConditions conditions = object.has("conditions")
                ? parseConditions(sourceId, object.get("conditions"))
                : EvolutionConditions.NONE;
        return new EvolutionDefinition(sourceId, from, to, minimumEp, conditions);
    }

    private static EvolutionConditions parseConditions(ResourceLocation sourceId, JsonElement raw) {
        if (!raw.isJsonObject()) {
            throw new IllegalArgumentException("'conditions' must be an object in " + sourceId);
        }
        JsonObject object = raw.getAsJsonObject();
        EvolutionConditions.OwnershipRequirement ownership = EvolutionConditions.OwnershipRequirement.ANY;
        if (object.has("ownership")) {
            try {
                ownership = EvolutionConditions.OwnershipRequirement.valueOf(
                        object.get("ownership").getAsString().trim().toUpperCase(Locale.ROOT)
                );
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Invalid evolution ownership condition in " + sourceId, ex);
            }
        }

        return new EvolutionConditions(
                parseIds(object, "required_skills", sourceId),
                parseIds(object, "forbidden_skills", sourceId),
                parseIds(object, "any_biome_tags", sourceId),
                parseIds(object, "forbidden_biome_tags", sourceId),
                parseIds(object, "dimensions", sourceId),
                ownership,
                getOptionalInt(object, "min_block_light"),
                getOptionalInt(object, "max_block_light")
        );
    }

    private static List<ResourceLocation> parseIds(JsonObject object, String key, ResourceLocation sourceId) {
        if (!object.has(key)) {
            return List.of();
        }
        if (!object.get(key).isJsonArray()) {
            throw new IllegalArgumentException("'" + key + "' must be an array in " + sourceId);
        }
        List<ResourceLocation> result = new ArrayList<>();
        for (JsonElement value : object.getAsJsonArray(key)) {
            ResourceLocation id = ResourceLocation.tryParse(value.getAsString());
            if (id == null) {
                throw new IllegalArgumentException("Invalid resource id in '" + key + "' for " + sourceId);
            }
            result.add(id);
        }
        return List.copyOf(result);
    }

    private static Integer getOptionalInt(JsonObject object, String key) {
        return object.has(key) ? object.get(key).getAsInt() : null;
    }

    private static ResourceLocation requireId(JsonObject object, String key, ResourceLocation sourceId) {
        if (!object.has(key) || !object.get(key).isJsonPrimitive()) {
            throw new IllegalArgumentException("Missing string '" + key + "' in " + sourceId);
        }
        ResourceLocation parsed = ResourceLocation.tryParse(object.get(key).getAsString());
        if (parsed == null) {
            throw new IllegalArgumentException("Invalid resource id for '" + key + "' in " + sourceId);
        }
        return parsed;
    }
}
