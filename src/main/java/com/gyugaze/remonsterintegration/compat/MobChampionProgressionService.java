package com.gyugaze.remonsterintegration.compat;

import com.gyugaze.remonsterintegration.ReMonsterIntegration;
import com.gyugaze.remonsterintegration.tensura.TensuraExistenceBridge;
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
 * - a one-time higher Tensura baseMaxEP floor, making the individual itself stronger,
 * - a visible size modifier,
 * - one random Common/Extra skill for every champion and one additional Unique skill for Legendary.
 *
 * The rank EP floor and skill rolls are persisted on the individual so chunk reloads and physical
 * evolution never stack or reroll them. Native Tensura EP gained later remains ordinary progression
 * above that birth floor.
 */
public final class MobChampionProgressionService {
    private static final ResourceLocation SCALE_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(
            ReMonsterIntegration.MOD_ID,
            "mobchampions_rank_scale"
    );

    private static final String RANK_EP_APPLIED_KEY = "remonster_mobchampions_rank_ep_applied";
    private static final String RANK_EP_FLOOR_KEY = "remonster_mobchampions_rank_ep_floor";
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

    /** Called after MobChampions and Re:Monster Mob Profiles have established the entity's spawn state. */
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
        applyRankEpFloor(entity, rank, data);
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
     * Applies the rank percentage to the individual's starting baseMaxEP exactly once.
     * Later native Tensura growth is not multiplied again; the persisted result only acts as a floor.
     */
    private static void applyRankEpFloor(
            LivingEntity entity,
            MobChampionsBridge.ChampionRank rank,
            CompoundTag data
    ) {
        double current = TensuraExistenceBridge.getBaseMaxEp(entity);
        if (!data.getBoolean(RANK_EP_APPLIED_KEY)) {
            double floor = current * baseEpMultiplierFor(rank);
            if (floor > current) {
                TensuraExistenceBridge.setBaseMaxEpPreservingEnergyRatio(entity, floor);
            }
            data.putBoolean(RANK_EP_APPLIED_KEY, true);
            data.putDouble(RANK_EP_FLOOR_KEY, Math.max(current, floor));
            return;
        }

        double floor = data.getDouble(RANK_EP_FLOOR_KEY);
        if (floor > current + 1.0E-9D) {
            TensuraExistenceBridge.setBaseMaxEpPreservingEnergyRatio(entity, floor);
        }
    }

    public static double baseEpMultiplierFor(MobChampionsBridge.ChampionRank rank) {
        return switch (rank) {
            case UNCOMMON -> 1.25D;
            case RARE -> 1.50D;
            case EPIC -> 2.00D;
            case LEGENDARY -> 3.00D;
        };
    }

    public static double scaleFor(MobChampionsBridge.ChampionRank rank) {
        return switch (rank) {
            case UNCOMMON -> 1.10D;
            case RARE -> 1.20D;
            case EPIC -> 1.35D;
            case LEGENDARY -> 1.50D;
        };
    }

    /** Prevents a replacement body from rolling a fresh champion skill/EP bonus during finalizeSpawn. */
    public static void markEvolutionTransfer(LivingEntity target) {
        target.getPersistentData().putBoolean(EVOLUTION_TRANSFER_KEY, true);
    }

    /** Clears the temporary transfer guard and restores the source individual's persisted rank bonuses. */
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
                Method getId = field.getType().getMethod("getId");
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
