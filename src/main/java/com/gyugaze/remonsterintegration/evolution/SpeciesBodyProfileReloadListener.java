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
import java.util.Map;
import java.util.Optional;

public final class SpeciesBodyProfileReloadListener extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();
    private static final String DIRECTORY = "remonster_species";

    public SpeciesBodyProfileReloadListener() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(
            Map<ResourceLocation, JsonElement> resources,
            ResourceManager resourceManager,
            ProfilerFiller profiler
    ) {
        Map<ResourceLocation, SpeciesBodyProfile> loaded = new LinkedHashMap<>();
        int invalid = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
            try {
                SpeciesBodyProfile profile = parse(entry.getKey(), entry.getValue());
                SpeciesBodyProfile previous = loaded.put(profile.species(), profile);
                if (previous != null) {
                    ReMonsterIntegration.LOGGER.warn(
                            "Species body profile {} replaces an earlier profile for {}",
                            entry.getKey(),
                            profile.species()
                    );
                }
            } catch (RuntimeException ex) {
                invalid++;
                ReMonsterIntegration.LOGGER.error("Failed to load species body profile {}", entry.getKey(), ex);
            }
        }

        SpeciesBodyProfileRegistry.replaceAll(loaded);
        ReMonsterIntegration.LOGGER.info(
                "Loaded {} Re:Monster species body profiles ({} invalid)",
                loaded.size(),
                invalid
        );
    }

    private static SpeciesBodyProfile parse(ResourceLocation sourceId, JsonElement element) {
        if (!element.isJsonObject()) {
            throw new IllegalArgumentException("Species body profile root must be a JSON object");
        }
        JsonObject object = element.getAsJsonObject();
        ResourceLocation species = requireId(object, "species", sourceId);

        Optional<SpeciesBodyProfile.TownsteadBody> townstead = Optional.empty();
        if (object.has("townstead")) {
            JsonElement rawTownstead = object.get("townstead");
            if (!rawTownstead.isJsonObject()) {
                throw new IllegalArgumentException("'townstead' must be an object");
            }
            JsonObject body = rawTownstead.getAsJsonObject();
            List<ResourceLocation> candidates = parseIds(body, "root_candidates", sourceId);
            List<String> searchTerms = parseStrings(body, "root_search_terms");
            if (candidates.isEmpty() && searchTerms.isEmpty()) {
                throw new IllegalArgumentException(
                        "Townstead body must provide root_candidates and/or root_search_terms"
                );
            }
            townstead = Optional.of(new SpeciesBodyProfile.TownsteadBody(
                    candidates,
                    searchTerms,
                    getBoolean(body, "preserve_shared_loci", true),
                    getBoolean(body, "clamp_body_metrics", true)
            ));
        }

        return new SpeciesBodyProfile(species, townstead);
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

    private static List<ResourceLocation> parseIds(JsonObject object, String key, ResourceLocation sourceId) {
        if (!object.has(key)) {
            return List.of();
        }
        if (!object.get(key).isJsonArray()) {
            throw new IllegalArgumentException("'" + key + "' must be an array in " + sourceId);
        }
        List<ResourceLocation> result = new ArrayList<>();
        for (JsonElement element : object.getAsJsonArray(key)) {
            ResourceLocation id = ResourceLocation.tryParse(element.getAsString());
            if (id == null) {
                throw new IllegalArgumentException("Invalid resource id in '" + key + "' for " + sourceId);
            }
            result.add(id);
        }
        return List.copyOf(result);
    }

    private static List<String> parseStrings(JsonObject object, String key) {
        if (!object.has(key)) {
            return List.of();
        }
        if (!object.get(key).isJsonArray()) {
            throw new IllegalArgumentException("'" + key + "' must be an array");
        }
        List<String> result = new ArrayList<>();
        for (JsonElement element : object.getAsJsonArray(key)) {
            String value = element.getAsString().trim();
            if (!value.isEmpty()) {
                result.add(value);
            }
        }
        return List.copyOf(result);
    }

    private static boolean getBoolean(JsonObject object, String key, boolean fallback) {
        return object.has(key) ? object.get(key).getAsBoolean() : fallback;
    }
}
