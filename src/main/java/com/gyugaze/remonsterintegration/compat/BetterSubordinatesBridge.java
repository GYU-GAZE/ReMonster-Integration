package com.gyugaze.remonsterintegration.compat;

import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.Method;
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

    private static volatile boolean lookupAttempted;
    private static volatile Method getSkillExpansionFrom;

    private BetterSubordinatesBridge() {
    }

    public static StorageStatus getProgressionStorageStatus(LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");

        Method method = resolveGetSkillExpansionFrom();
        if (method == null) {
            return StorageStatus.MOD_NOT_AVAILABLE;
        }

        try {
            Object storage = method.invoke(null, entity);
            return storage == null ? StorageStatus.MISSING : StorageStatus.PRESENT;
        } catch (ReflectiveOperationException | LinkageError ex) {
            return StorageStatus.ERROR;
        }
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
