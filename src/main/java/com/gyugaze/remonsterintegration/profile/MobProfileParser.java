package com.gyugaze.remonsterintegration.profile;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.gyugaze.remonsterintegration.ecology.EcologyProfile;
import com.gyugaze.remonsterintegration.ecology.Habitat;
import com.gyugaze.remonsterintegration.ecology.SpawnRarity;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class MobProfileParser {
    private MobProfileParser() {
    }

    public static MobProfile parse(ResourceLocation sourceId, JsonObject json) {
        ResourceLocation entityId = parseResourceLocation(requiredString(json, "entity"), sourceId, "entity");
        MobDisposition disposition = enumValue(MobDisposition.class, string(json, "disposition", "neutral"), sourceId, "disposition");
        MobTier tier = enumValue(MobTier.class, string(json, "tier", "normal"), sourceId, "tier");
        double ep = number(json, "ep", 0.0);
        EpApplicationMode epMode = enumValue(EpApplicationMode.class, string(json, "ep_mode", "fallback"), sourceId, "ep_mode");
        double learningMultiplier = number(json, "learning_multiplier", 1.0);
        boolean guardHostile = bool(json, "guard_hostile", disposition == MobDisposition.HOSTILE);
        int dungeonWeight = integer(json, "dungeon_weight", 0);

        List<ResourceLocation> skills = new ArrayList<>();
        for (String skill : stringArray(json, "skills")) {
            skills.add(parseResourceLocation(skill, sourceId, "skills"));
        }

        Set<String> themes = new LinkedHashSet<>(stringArray(json, "themes"));
        EcologyProfile ecology = parseEcology(sourceId, json.getAsJsonObject("ecology"));

        return new MobProfile(
                entityId,
                disposition,
                tier,
                ep,
                epMode,
                skills,
                learningMultiplier,
                guardHostile,
                dungeonWeight,
                themes,
                ecology
        );
    }

    private static EcologyProfile parseEcology(ResourceLocation sourceId, JsonObject json) {
        if (json == null) {
            return new EcologyProfile(false, Set.of(), SpawnRarity.SPECIAL, 0, 1, 1);
        }

        boolean naturalSpawn = bool(json, "natural_spawn", true);
        SpawnRarity rarity = enumValue(SpawnRarity.class, string(json, "rarity", "common"), sourceId, "ecology.rarity");
        int weight = integer(json, "weight", 1);
        int groupMin = integer(json, "group_min", 1);
        int groupMax = integer(json, "group_max", groupMin);

        Set<Habitat> habitats = new LinkedHashSet<>();
        for (String habitat : stringArray(json, "habitats")) {
            habitats.add(enumValue(Habitat.class, habitat, sourceId, "ecology.habitats"));
        }

        return new EcologyProfile(naturalSpawn, habitats, rarity, weight, groupMin, groupMax);
    }

    private static ResourceLocation parseResourceLocation(String value, ResourceLocation sourceId, String field) {
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null) {
            throw new IllegalArgumentException("Invalid resource location '" + value + "' in " + sourceId + " field " + field);
        }
        return id;
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String value, ResourceLocation sourceId, String field) {
        try {
            return Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid " + field + " value '" + value + "' in " + sourceId, ex);
        }
    }

    private static String requiredString(JsonObject json, String key) {
        if (!json.has(key) || !json.get(key).isJsonPrimitive()) {
            throw new IllegalArgumentException("Missing required string field '" + key + "'");
        }
        return json.get(key).getAsString();
    }

    private static String string(JsonObject json, String key, String fallback) {
        return json.has(key) ? json.get(key).getAsString() : fallback;
    }

    private static boolean bool(JsonObject json, String key, boolean fallback) {
        return json.has(key) ? json.get(key).getAsBoolean() : fallback;
    }

    private static int integer(JsonObject json, String key, int fallback) {
        return json.has(key) ? json.get(key).getAsInt() : fallback;
    }

    private static double number(JsonObject json, String key, double fallback) {
        return json.has(key) ? json.get(key).getAsDouble() : fallback;
    }

    private static List<String> stringArray(JsonObject json, String key) {
        if (!json.has(key)) {
            return List.of();
        }

        JsonArray array = json.getAsJsonArray(key);
        List<String> values = new ArrayList<>(array.size());
        for (JsonElement element : array) {
            values.add(element.getAsString());
        }
        return values;
    }
}
