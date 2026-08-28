package com.gyugaze.remonsterintegration.evolution;

import io.github.manasmods.manascore.race.api.ManasRace;
import io.github.manasmods.manascore.race.api.ManasRaceInstance;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.race.api.Races;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.Objects;
import java.util.Optional;

/**
 * Persistent Re:Monster species identity.
 *
 * Species that declare a native_race binding are one identity with that ManasCore/Tensura race: the
 * native race is authoritative and the persisted Re:Monster id is its project metadata/body mapping.
 * Species without a native_race remain valid semantic identities for external creatures.
 */
public final class SpeciesStateService {
    private static final String SPECIES_KEY = "remonster_species";
    private static final String PHENOTYPE_SEED_KEY = "remonster_phenotype_seed";
    private static final long PHENOTYPE_SALT = 0x52454D4F4E535445L;

    private SpeciesStateService() {
    }

    public static Optional<ResourceLocation> getSpecies(LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");

        Optional<ResourceLocation> nativeRace = getNativeRaceId(entity);
        if (nativeRace.isPresent()) {
            Optional<ResourceLocation> nativeSpecies = SpeciesBodyProfileRegistry.speciesForNativeRace(nativeRace.get());
            if (nativeSpecies.isPresent()) {
                return nativeSpecies;
            }
        }

        Optional<ResourceLocation> stored = getStoredSpecies(entity);
        if (stored.isEmpty()) {
            return Optional.empty();
        }

        // A stale bound species must never disagree with an existing native race. This is what removes
        // the old Frankenstein state where semantic Hobgoblin could coexist with Enlightened Human.
        Optional<ResourceLocation> expectedRace = SpeciesBodyProfileRegistry.nativeRaceForSpecies(stored.get());
        if (expectedRace.isPresent() && nativeRace.isPresent() && !expectedRace.get().equals(nativeRace.get())) {
            return Optional.empty();
        }
        return stored;
    }

    public static Optional<ResourceLocation> getStoredSpecies(LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        String raw = entity.getPersistentData().getString(SPECIES_KEY);
        if (raw.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(ResourceLocation.tryParse(raw));
    }

    /**
     * Assigns a project species. When that species is bound to Tensura, the native race is changed too;
     * a successful RaceStorage set will then run the common body synchronization hook.
     */
    public static boolean setSpecies(LivingEntity entity, ResourceLocation species) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(species, "species");

        Optional<ResourceLocation> nativeRaceId = SpeciesBodyProfileRegistry.nativeRaceForSpecies(species);
        if (nativeRaceId.isPresent()) {
            ManasRace nativeRace = RaceAPI.getRaceRegistry().get(nativeRaceId.get());
            if (nativeRace == null) {
                return false;
            }

            Optional<ResourceLocation> currentRace = getNativeRaceId(entity);
            if (currentRace.isEmpty() || !currentRace.get().equals(nativeRaceId.get())) {
                Races races = RaceAPI.getRaceFrom(entity);
                if (!races.setRace(nativeRace, false)) {
                    return false;
                }
            }
        }

        setStoredSpeciesFromNative(entity, species);
        return true;
    }

    /** Used only by the native-race bridge after RaceStorage has already made the authoritative change. */
    static void setStoredSpeciesFromNative(LivingEntity entity, ResourceLocation species) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(species, "species");
        ensurePhenotypeSeed(entity);
        entity.getPersistentData().putString(SPECIES_KEY, species.toString());
    }

    public static Optional<ResourceLocation> getNativeRaceId(LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        try {
            return RaceAPI.getRaceFrom(entity).getRace().map(ManasRaceInstance::getRaceId);
        } catch (RuntimeException | LinkageError ignored) {
            return Optional.empty();
        }
    }

    public static void clearSpecies(LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        entity.getPersistentData().remove(SPECIES_KEY);
    }

    public static long getPhenotypeSeed(LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        return ensurePhenotypeSeed(entity);
    }

    public static void copyState(LivingEntity from, LivingEntity to) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");

        CompoundTag source = from.getPersistentData();
        CompoundTag target = to.getPersistentData();
        if (source.contains(SPECIES_KEY)) {
            target.putString(SPECIES_KEY, source.getString(SPECIES_KEY));
        }
        if (source.contains(PHENOTYPE_SEED_KEY)) {
            target.putLong(PHENOTYPE_SEED_KEY, source.getLong(PHENOTYPE_SEED_KEY));
        }
    }

    public static long seedForSpecies(LivingEntity entity, ResourceLocation species) {
        long base = getPhenotypeSeed(entity);
        long speciesHash = species.toString().hashCode();
        return mix64(base ^ (speciesHash * 0x9E3779B97F4A7C15L));
    }

    private static long ensurePhenotypeSeed(LivingEntity entity) {
        CompoundTag tag = entity.getPersistentData();
        if (tag.contains(PHENOTYPE_SEED_KEY)) {
            return tag.getLong(PHENOTYPE_SEED_KEY);
        }

        long seed = mix64(
                entity.getUUID().getMostSignificantBits()
                        ^ Long.rotateLeft(entity.getUUID().getLeastSignificantBits(), 23)
                        ^ PHENOTYPE_SALT
        );
        tag.putLong(PHENOTYPE_SEED_KEY, seed);
        return seed;
    }

    private static long mix64(long value) {
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }
}
