package com.gyugaze.remonsterintegration.compat;

import com.gyugaze.remonsterintegration.ReMonsterIntegration;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.ModList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Optional read-only bridge to MobChampions.
 *
 * MobChampions is intentionally not a hard compile/runtime dependency of Re:Monster Integration.
 * The bridge consumes its public platform/data API reflectively so removing MobChampions never makes
 * the core integration addon unloadable.
 */
public final class MobChampionsBridge {
    public static final String MOD_ID = "mobchampions";

    private static volatile Access access;
    private static volatile boolean initializationAttempted;

    private MobChampionsBridge() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    /**
     * Returns only actual champion ranks. COMMON and uninitialized attachment data are treated as empty.
     */
    public static Optional<ChampionRank> getChampionRank(LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");

        if (!isLoaded()) {
            return Optional.empty();
        }

        Access resolved = resolveAccess();
        if (resolved == null) {
            return Optional.empty();
        }

        try {
            Object value = resolved.getMobChampionData().invoke(resolved.platform(), entity);
            if (!(value instanceof Optional<?> optional) || optional.isEmpty()) {
                return Optional.empty();
            }

            Object data = optional.get();
            int entityId = ((Number) data.getClass().getMethod("getEntityId").invoke(data)).intValue();
            if (entityId == -1) {
                return Optional.empty();
            }

            Object rank = data.getClass().getMethod("getRank").invoke(data);
            if (!(rank instanceof Enum<?> rankEnum)) {
                return Optional.empty();
            }

            String name = rankEnum.name().toUpperCase(Locale.ROOT);
            if ("COMMON".equals(name)) {
                return Optional.empty();
            }

            try {
                return Optional.of(ChampionRank.valueOf(name));
            } catch (IllegalArgumentException ignored) {
                ReMonsterIntegration.LOGGER.warn("Unknown MobChampions rank '{}'; treating as non-champion", name);
                return Optional.empty();
            }
        } catch (ReflectiveOperationException | RuntimeException ex) {
            ReMonsterIntegration.LOGGER.warn("Could not read MobChampions state for {}", entity.getUUID(), ex);
            return Optional.empty();
        }
    }

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
                access = new Access(platform, getMobChampionData);
            } catch (ReflectiveOperationException | RuntimeException ex) {
                ReMonsterIntegration.LOGGER.warn("MobChampions is loaded but its public champion-data API could not be resolved", ex);
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

    private record Access(Object platform, Method getMobChampionData) {
    }
}
