package com.gyugaze.remonsterintegration.compat;

import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Reflection-only runtime boundary for optional Better Subordinates diagnostics.
 *
 * Re:Monster compiles against the exact target Better Subordinates build for mixin development,
 * but the integration addon must still load when Better Subordinates is absent. Keeping this
 * diagnostic boundary reflective prevents an optional third-party class from becoming a hard
 * classloading dependency of the command system.
 */
public final class BetterSubordinatesBridge {
    private static final String BETTER_SUBS_CLASS = "com.trbeyond.tensura_better_subs.BetterSubs";
    private static final List<String> PROGRESSION_MEMBERS = List.of(
            "spiderEyesEaten",
            "spiderKills",
            "slimeKills",
            "centipedeKills",
            "totalKills",
            "damageTaken",
            "existenceUpdated"
    );

    private static volatile boolean lookupAttempted;
    private static volatile Method getSkillExpansionFrom;

    private BetterSubordinatesBridge() {
    }

    public static StorageStatus getProgressionStorageStatus(LivingEntity entity) {
        return getProgressionSnapshot(entity).status();
    }

    /**
     * Reads Better Subordinates' native ExpandedSkillStorage without taking ownership of it.
     *
     * The known counter names are intentionally treated as diagnostics only. Better Subordinates
     * remains authoritative for incrementing, persisting and interpreting every value.
     */
    public static ProgressionSnapshot getProgressionSnapshot(LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");

        Method method = resolveGetSkillExpansionFrom();
        if (method == null) {
            return ProgressionSnapshot.unavailable(StorageStatus.MOD_NOT_AVAILABLE);
        }

        try {
            Object storage = method.invoke(null, entity);
            if (storage == null) {
                return ProgressionSnapshot.unavailable(StorageStatus.MISSING);
            }

            Map<String, String> values = new LinkedHashMap<>();
            for (String member : PROGRESSION_MEMBERS) {
                values.put(member, readMember(storage, member));
            }
            return new ProgressionSnapshot(
                    StorageStatus.PRESENT,
                    storage.getClass().getName(),
                    Map.copyOf(values)
            );
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ex) {
            return ProgressionSnapshot.unavailable(StorageStatus.ERROR);
        }
    }

    private static String readMember(Object storage, String member) {
        String suffix = Character.toUpperCase(member.charAt(0)) + member.substring(1);
        for (String methodName : List.of("get" + suffix, "is" + suffix, member)) {
            Method accessor = findNoArgMethod(storage.getClass(), methodName);
            if (accessor == null) {
                continue;
            }
            try {
                accessor.setAccessible(true);
                return String.valueOf(accessor.invoke(storage));
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // Fall through to direct field access. Diagnostics must never affect progression.
            }
        }

        Field field = findField(storage.getClass(), member);
        if (field != null) {
            try {
                field.setAccessible(true);
                return String.valueOf(field.get(storage));
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // Report unknown below.
            }
        }
        return "unknown";
    }

    private static Method findNoArgMethod(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredMethod(name);
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static Field findField(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static Method resolveGetSkillExpansionFrom() {
        if (lookupAttempted) {
            return getSkillExpansionFrom;
        }

        synchronized (BetterSubordinatesBridge.class) {
            if (lookupAttempted) {
                return getSkillExpansionFrom;
            }

            try {
                Class<?> betterSubs = Class.forName(BETTER_SUBS_CLASS, false, BetterSubordinatesBridge.class.getClassLoader());
                getSkillExpansionFrom = betterSubs.getMethod("getSkillExpansionFrom", LivingEntity.class);
            } catch (ReflectiveOperationException | LinkageError ignored) {
                getSkillExpansionFrom = null;
            } finally {
                lookupAttempted = true;
            }

            return getSkillExpansionFrom;
        }
    }

    public record ProgressionSnapshot(
            StorageStatus status,
            String storageClass,
            Map<String, String> values
    ) {
        public ProgressionSnapshot {
            storageClass = storageClass == null ? "" : storageClass;
            values = values == null ? Map.of() : Map.copyOf(values);
        }

        public static ProgressionSnapshot unavailable(StorageStatus status) {
            return new ProgressionSnapshot(status, "", Map.of());
        }
    }

    public enum StorageStatus {
        PRESENT("present"),
        MISSING("missing"),
        MOD_NOT_AVAILABLE("mod not available"),
        ERROR("reflection error");

        private final String displayName;

        StorageStatus(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }
    }
}
