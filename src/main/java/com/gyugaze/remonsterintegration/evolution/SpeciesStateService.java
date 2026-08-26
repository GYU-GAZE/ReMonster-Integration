package com.gyugaze.remonsterintegration.evolution;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.Objects;
import java.util.Optional;

/**
 * Persistent semantic species state owned by Re:Monster.
 *
 * <p>This is intentionally independent from the entity Java type and from visual/body adapters.
 * An MCA villager may therefore be semantically an Ogre while Townstead renders that same
 * individual with a Tiefling-derived body.</p>
 */
public final class SpeciesStateService {
    private static final String SPECIES_KEY = "remonster_species";
    private static final String PHENOTYPE_SEED_KEY = "remonster_phenotype_seed";
    private static final long PHENOTYPE_SALT = 0x52454D4F4E535445L;

    private SpeciesStateService() {
    }

    public static Optional<ResourceLocation> getSpecies(LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        String raw = entity.getPersistentData().getString(SPECIES_KEY);
        if (raw.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(ResourceLocation.tryParse(raw));
    }

    public static void setSpecies(LivingEntity entity, ResourceLocation species) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(species, "species");
        ensurePhenotypeSeed(entity);
        entity.getPersistentData().putString(SPECIES_KEY, species.toString());
    }

    public static void clearSpecies(LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        entity.getPersistentData().remove(SPECIES_KEY);
    }

    /**
     * Stable seed used by body adapters when a transformation needs deterministic new traits.
     * It is stored explicitly so a future controlled entity replacement can copy it.
     */
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
