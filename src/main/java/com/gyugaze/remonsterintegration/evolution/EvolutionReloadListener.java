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

import java.util.LinkedHashMap;
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
        return new EvolutionDefinition(sourceId, from, to, minimumEp);
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
