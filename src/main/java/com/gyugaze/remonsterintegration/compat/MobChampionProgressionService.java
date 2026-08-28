package com.gyugaze.remonsterintegration.compat;

import com.gyugaze.remonsterintegration.ReMonsterIntegration;
import com.gyugaze.remonsterintegration.tensura.TensuraSkillBridge;
import io.github.manasmods.tensura.registry.skill.CommonSkills;
import io.github.manasmods.tensura.registry.skill.ExtraSkills;
import io.github.manasmods.tensura.registry.skill.UniqueSkills;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Re:Monster gameplay layer for MobChampions ranks.
 *
 * MobChampions remains authoritative for rank generation and its own stat/equipment/effect package.
 * Re:Monster adds three rank-facing consequences:
 * - bonus EP reward when this entity is defeated (applied in the Tensura EnergyHelper mixin),
 * - a visible size modifier,
 * - one random Common/Extra skill for every champion and one additional Unique skill for Legendary.
 *
 * Skill rolls are persisted on the individual so chunk reloads and physical evolution never reroll them.
 */
public final class MobChampionProgressionService {
    private static final ResourceLocation SCALE_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(
            ReMonsterIntegration.MOD_ID,
            "mobchampions_rank_scale"
    );

    private static final String ABILITY_ROLLED_KEY = "remonster_mobchampions_ability_rolled";
    private static final String ABILITY_ID_KEY = "remonster_mobchampions_ability_id";
    private static final String UNIQUE_ROLLED_KEY = "remonster_mobchampions_unique_rolled";
    private static final String UNIQUE_ID_KEY = "remonster_mobchampions_unique_id";
    private static final String EVOLUTION_TRANSFER_KEY = "remonster_mobchampions_evolution_transfer";

    private static final Set<ResourceLocation> ORDINARY_EXCLUSIONS = Set.of(
            CommonSkills.TELEPATHY.getId(),
            CommonSkills.THOUGHT_COMMUNICATION.getId()
    );

    private static volatile List<ResourceLocation> ordinarySkillPool;
    private static volatile List<ResourceLocation> uniqueSkillPool;

    private MobChampionProgressionService() {
    }

    /** Called after MobChampions has initialized/synchronized the entity's rank. */
    public static void applySpawnBonuses(LivingEntity entity) {
        if (entity.level().isClientSide() || !entity.isAlive()) {
            return;
        }

        CompoundTag data = entity.getPersistentData();
        if (data.getBoolean(EVOLUTION_TRANSFER_KEY)) {
            return;
        }

        Optional<MobChampionsBridge.ChampionRank> rankOptional = MobChampionsBridge.getChampionRank(entity);
        if (rankOptional.isEmpty()) {
            removeScaleModifier(entity);
            return;
        }

        MobChampionsBridge.ChampionRank rank = rankOptional.get();
        applyScaleModifier(entity, scaleFor(rank));

        if (!data.getBoolean(ABILITY_ROLLED_KEY)) {
            ResourceLocation granted = grantRandomSkill(entity, ordinarySkills());
            data.putBoolean(ABILITY_ROLLED_KEY, true);
            if (granted != null) {
                data.putString(ABILITY_ID_KEY, granted.toString());
            }
        }

        if (rank == MobChampionsBridge.ChampionRank.LEGENDARY && !data.getBoolean(UNIQUE_ROLLED_KEY)) {
            ResourceLocation granted = grantRandomSkill(entity, uniqueSkills());
            data.putBoolean(UNIQUE_ROLLED_KEY, true);
            if (granted != null) {
                data.putString(UNIQUE_ID_KEY, granted.toString());
            }
        }
    }

    /**
     * Native Tensura EP reward multiplier for the defeated champion.
     * Common/non-champion entities remain exactly 1x.
     */
    public static double epRewardMultiplier(LivingEntity defeated) {
        return MobChampionsBridge.getChampionRank(defeated)
                .map(rank -> switch (rank) {
                    case UNCOMMON -> 1.25D;   // +25%
                    case RARE -> 1.50D;       // +50%
                    case EPIC -> 2.00D;       // +100%
                    case LEGENDARY -> 3.00D;  // +200%
                })
                .orElse(1.0D);
    }

    public static double scaleFor(MobChampionsBridge.ChampionRank rank) {
        return switch (rank) {
            case UNCOMMON -> 1.10D;
            case RARE -> 1.20D;
            case EPIC -> 1.35D;
            case LEGENDARY -> 1.50D;
        };
    }

    /** Prevents a replacement body from rolling a fresh champion skill during finalizeSpawn. */
    public static void markEvolutionTransfer(LivingEntity target) {
        target.getPersistentData().putBoolean(EVOLUTION_TRANSFER_KEY, true);
    }

    /** Clears the temporary transfer guard and restores the source individual's rank scale. */
    public static void finishEvolutionTransfer(LivingEntity target) {
        target.getPersistentData().remove(EVOLUTION_TRANSFER_KEY);
        applySpawnBonuses(target);
    }

    private static void applyScaleModifier(LivingEntity entity, double scale) {
        AttributeInstance attribute = entity.getAttribute(Attributes.SCALE);
        if (attribute == null) {
            return;
        }

        attribute.removeModifier(SCALE_MODIFIER_ID);
        if (Math.abs(scale - 1.0D) > 1.0E-6D) {
            attribute.addPermanentModifier(new AttributeModifier(
                    SCALE_MODIFIER_ID,
                    scale - 1.0D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            ));
        }
        entity.refreshDimensions();
    }

    private static void removeScaleModifier(LivingEntity entity) {
        AttributeInstance attribute = entity.getAttribute(Attributes.SCALE);
        if (attribute != null && attribute.hasModifier(SCALE_MODIFIER_ID)) {
            attribute.removeModifier(SCALE_MODIFIER_ID);
            entity.refreshDimensions();
        }
    }

    private static ResourceLocation grantRandomSkill(LivingEntity entity, List<ResourceLocation> pool) {
        if (pool.isEmpty()) {
            return null;
        }

        int start = entity.getRandom().nextInt(pool.size());
        for (int offset = 0; offset < pool.size(); offset++) {
            ResourceLocation skillId = pool.get((start + offset) % pool.size());
            if (TensuraSkillBridge.getSkillInstance(entity, skillId).isPresent()) {
                continue;
            }
            try {
                if (TensuraSkillBridge.ensureSkill(entity, skillId)) {
                    ReMonsterIntegration.LOGGER.debug(
                            "Granted MobChampions birth skill {} to {}",
                            skillId,
                            entity.getUUID()
                    );
                    return skillId;
                }
            } catch (RuntimeException | LinkageError exception) {
                ReMonsterIntegration.LOGGER.debug(
                        "MobChampions birth skill {} was rejected for {}",
                        skillId,
                        entity.getUUID(),
                        exception
                );
            }
        }
        return null;
    }

    private static List<ResourceLocation> ordinarySkills() {
        List<ResourceLocation> cached = ordinarySkillPool;
        if (cached != null) {
            return cached;
        }
        synchronized (MobChampionProgressionService.class) {
            if (ordinarySkillPool == null) {
                LinkedHashSet<ResourceLocation> ids = new LinkedHashSet<>();
                ids.addAll(readRegistrySupplierIds(CommonSkills.class));
                ids.addAll(readRegistrySupplierIds(ExtraSkills.class));
                ids.removeAll(ORDINARY_EXCLUSIONS);
                ordinarySkillPool = List.copyOf(ids);
            }
            return ordinarySkillPool;
        }
    }

    private static List<ResourceLocation> uniqueSkills() {
        List<ResourceLocation> cached = uniqueSkillPool;
        if (cached != null) {
            return cached;
        }
        synchronized (MobChampionProgressionService.class) {
            if (uniqueSkillPool == null) {
                uniqueSkillPool = List.copyOf(readRegistrySupplierIds(UniqueSkills.class));
            }
            return uniqueSkillPool;
        }
    }

    /**
     * Tensura declares its skill catalogs as public static RegistrySupplier fields. Reflection keeps
     * this pool version-tolerant and automatically includes every skill shipped by the target build.
     */
    private static List<ResourceLocation> readRegistrySupplierIds(Class<?> registryClass) {
        List<ResourceLocation> result = new ArrayList<>();
        for (Field field : registryClass.getFields()) {
            if (!Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            try {
                Object supplier = field.get(null);
                if (supplier == null) {
                    continue;
                }
                Method getId = supplier.getClass().getMethod("getId");
                Object id = getId.invoke(supplier);
                if (id instanceof ResourceLocation resourceLocation) {
                    result.add(resourceLocation);
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // Non RegistrySupplier public fields, if any, are intentionally ignored.
            }
        }
        return result;
    }
}
