package com.gyugaze.remonsterintegration.compat;

import com.gyugaze.remonsterintegration.ReMonsterIntegration;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.ModList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/** Optional reflective bridge to MobChampions. */
public final class MobChampionsBridge {
    public static final String MOD_ID = "mobchampions";

    private static volatile Access access;
    private static volatile boolean initializationAttempted;

    private MobChampionsBridge() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    /** Returns only actual champion ranks; COMMON/uninitialized data are treated as empty. */
    public static Optional<ChampionRank> getChampionRank(LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        Optional<ChampionState> state = getChampionState(entity);
        if (state.isEmpty() || !state.get().initialized() || "COMMON".equals(state.get().rankName())) {
            return Optional.empty();
        }
        try {
            return Optional.of(ChampionRank.valueOf(state.get().rankName()));
        } catch (IllegalArgumentException ignored) {
            ReMonsterIntegration.LOGGER.warn(
                    "Unknown MobChampions rank '{}'; treating as non-champion",
                    state.get().rankName()
            );
            return Optional.empty();
        }
    }

    /**
     * Seeds a replacement entity's MobChampions attachment before finalizeSpawn. MobChampions only
     * rolls a random rank while entityId == -1, so this preserves the source rank without skipping the
     * new species' native finalizeSpawn lifecycle.
     */
    public static void prepareEvolutionSpawn(LivingEntity source, LivingEntity target) {
        if (!isLoaded()) {
            return;
        }
        Access resolved = resolveAccess();
        if (resolved == null) {
            return;
        }

        try {
            Object targetData = getDataObject(resolved, target).orElse(null);
            if (targetData == null) {
                return;
            }
            Object sourceData = getDataObject(resolved, source).orElse(null);
            Object sourceRank = sourceData == null
                    ? resolved.commonRank()
                    : sourceData.getClass().getMethod("getRank").invoke(sourceData);

            targetData.getClass().getMethod("setEntityId", int.class).invoke(targetData, target.getId());
            targetData.getClass().getMethod("setRank", resolved.rankClass()).invoke(targetData, sourceRank);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            ReMonsterIntegration.LOGGER.warn(
                    "Could not seed MobChampions evolution state {} -> {}",
                    source.getUUID(),
                    target.getUUID(),
                    ex
            );
        }
    }

    /** Re-applies champion gameplay modifiers of the preserved rank after the target finalized. */
    public static void finishEvolutionSpawn(LivingEntity source, LivingEntity target) {
        if (!isLoaded()) {
            return;
        }
        Access resolved = resolveAccess();
        if (resolved == null) {
            return;
        }

        try {
            Object targetData = getDataObject(resolved, target).orElse(null);
            if (targetData == null) {
                return;
            }
            Object rank = targetData.getClass().getMethod("getRank").invoke(targetData);
            if (rank instanceof Enum<?> rankEnum && !"COMMON".equals(rankEnum.name())) {
                resolved.buildChampion().invoke(null, target, rank);
            }
            resolved.syncMobChampionData().invoke(resolved.platform(), null, target);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            ReMonsterIntegration.LOGGER.warn(
                    "Could not finish MobChampions evolution transfer {} -> {}",
                    source.getUUID(),
                    target.getUUID(),
                    ex
            );
        }
    }

    private static Optional<ChampionState> getChampionState(LivingEntity entity) {
        if (!isLoaded()) {
            return Optional.empty();
        }
        Access resolved = resolveAccess();
        if (resolved == null) {
            return Optional.empty();
        }

        try {
            Object data = getDataObject(resolved, entity).orElse(null);
            if (data == null) {
                return Optional.empty();
            }
            int entityId = ((Number) data.getClass().getMethod("getEntityId").invoke(data)).intValue();
            Object rank = data.getClass().getMethod("getRank").invoke(data);
            if (!(rank instanceof Enum<?> rankEnum)) {
                return Optional.empty();
            }
            return Optional.of(new ChampionState(entityId != -1, rankEnum.name().toUpperCase(Locale.ROOT)));
        } catch (ReflectiveOperationException | RuntimeException ex) {
            ReMonsterIntegration.LOGGER.warn("Could not read MobChampions state for {}", entity.getUUID(), ex);
            return Optional.empty();
        }
    }

    private static Optional<?> getDataObject(Access resolved, LivingEntity entity) throws ReflectiveOperationException {
        Object value = resolved.getMobChampionData().invoke(resolved.platform(), entity);
        return value instanceof Optional<?> optional ? optional : Optional.empty();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Access resolveAccess() {
        if (initializationAttempted) {
            return access;
        }

        synchronized (MobChampionsBridge.class) {
            if (initializationAttempted) {
                return access;
            }

            initializationAttempted = true;
            try {
                Class<?> servicesClass = Class.forName("mobchampions.platform.Services");
                Field platformField = servicesClass.getField("PLATFORM");
                Object platform = platformField.get(null);
                Method getMobChampionData = platform.getClass().getMethod("getMobChampionData", LivingEntity.class);
                Method syncMobChampionData = platform.getClass().getMethod(
                        "syncMobChampionData",
                        ServerPlayer.class,
                        LivingEntity.class
                );
                Class<? extends Enum> rankClass = (Class<? extends Enum>) Class.forName(
                        "mobchampions.network.MobChampion$Rank"
                ).asSubclass(Enum.class);
                Object commonRank = Enum.valueOf(rankClass, "COMMON");
                Class<?> builderClass = Class.forName("mobchampions.util.MobChampionBuilder");
                Method buildChampion = builderClass.getMethod("build", LivingEntity.class, rankClass);
                access = new Access(
                        platform,
                        getMobChampionData,
                        syncMobChampionData,
                        rankClass,
                        commonRank,
                        buildChampion
                );
            } catch (ReflectiveOperationException | RuntimeException ex) {
                ReMonsterIntegration.LOGGER.warn(
                        "MobChampions is loaded but its public champion-data API could not be resolved",
                        ex
                );
                access = null;
            }

            return access;
        }
    }

    public enum ChampionRank {
        UNCOMMON,
        RARE,
        EPIC,
        LEGENDARY
    }

    private record ChampionState(boolean initialized, String rankName) {
    }

    private record Access(
            Object platform,
            Method getMobChampionData,
            Method syncMobChampionData,
            Class<? extends Enum> rankClass,
            Object commonRank,
            Method buildChampion
    ) {
    }
}
