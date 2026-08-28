package com.gyugaze.remonsterintegration.tensura;

import com.gyugaze.remonsterintegration.ReMonsterIntegration;
import com.gyugaze.remonsterintegration.evolution.NativeFormEvolutionCompat;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Narrow adapter for Tensura 2.0.1.2's same-EntityType Direwolf evolution machine.
 *
 * Re:Monster owns eligibility timing and the authored graph. Tensura remains authoritative for the
 * exact native body selector and in-place evolve() mechanics. Native autonomous calls to evolve()
 * are blocked by DirewolfEvolutionAuthorityMixin; this adapter authorizes exactly one native step
 * only after the graph has approved a destination that the native state machine agrees is valid.
 */
public final class TensuraDirewolfEvolutionCompat {
    private static final ResourceLocation DIREWOLF_ENTITY =
            ResourceLocation.fromNamespaceAndPath("tensura", "direwolf");

    private static final Set<String> BASE_NATIVE_SELECTOR_TARGETS = Set.of(
            "red_fang",
            "green_fang",
            "blue_fang",
            "brown_fang",
            "purple_fang",
            "black_fang",
            "star_wolf"
    );

    /** Deterministic follow-up transitions confirmed from Tensura 2.0.1.2 bytecode. */
    private static final Map<String, String> DETERMINISTIC_NATIVE_STEPS = Map.of(
            "red_fang", "mystic_fire_wolf",
            "green_fang", "mystic_wind_wolf",
            "blue_fang", "mystic_water_wolf",
            "brown_fang", "mystic_earth_wolf",
            "purple_fang", "mystic_space_wolf",
            "black_fang", "mystic_wolf",
            "tempest_wolf", "star_wolf",
            "star_wolf", "tempest_star_wolf"
    );

    private static final ThreadLocal<Integer> AUTHORIZED_NATIVE_DEPTH = ThreadLocal.withInitial(() -> 0);

    private TensuraDirewolfEvolutionCompat() {
    }

    public static NativeFormEvolutionCompat.Eligibility evaluateTransition(
            LivingEntity entity,
            String requestedForm
    ) {
        if (!isDirewolf(entity)) {
            return NativeFormEvolutionCompat.Eligibility.failure(
                    "Native Direwolf form target requires physical entity " + DIREWOLF_ENTITY
            );
        }

        String targetForm = normalize(requestedForm);
        Optional<String> currentOptional = currentForm(entity);
        if (currentOptional.isEmpty()) {
            return NativeFormEvolutionCompat.Eligibility.failure(
                    "Could not read native Tensura Direwolf variant from " + entity.getClass().getName()
            );
        }

        String currentForm = currentOptional.get();
        if (currentForm.equals(targetForm)) {
            return NativeFormEvolutionCompat.Eligibility.failure(
                    "Direwolf is already in native form " + targetForm
            );
        }

        Optional<Integer> currentState = currentEvolutionState(entity);
        Optional<Integer> maxState = maxEvolutionState(entity);
        if (currentState.isPresent() && maxState.isPresent() && currentState.get() >= maxState.get()) {
            return NativeFormEvolutionCompat.Eligibility.failure(
                    "Direwolf native evolution state is already maxed: " + currentState.get() + " / " + maxState.get()
            );
        }

        if ("direwolf".equals(currentForm) && BASE_NATIVE_SELECTOR_TARGETS.contains(targetForm)) {
            Optional<String> selected = nativeBaseEvolutionSelection(entity);
            if (selected.isEmpty()) {
                return NativeFormEvolutionCompat.Eligibility.failure(
                        "Could not evaluate Tensura Direwolf native environmental selector"
                );
            }
            if (!targetForm.equals(selected.get())) {
                return NativeFormEvolutionCompat.Eligibility.failure(
                        "Native Direwolf environment currently selects " + selected.get() + ", not " + targetForm
                );
            }
            return NativeFormEvolutionCompat.Eligibility.success(
                    "native-selector=" + selected.get()
            );
        }

        String deterministicTarget = DETERMINISTIC_NATIVE_STEPS.get(currentForm);
        if (targetForm.equals(deterministicTarget)) {
            return NativeFormEvolutionCompat.Eligibility.success(
                    "native-state-step=" + currentForm + "->" + targetForm
            );
        }

        return NativeFormEvolutionCompat.Eligibility.failure(
                "Unsupported Direwolf native form transition: " + currentForm + " -> " + targetForm
        );
    }

    public static NativeFormEvolutionCompat.ApplyResult apply(
            LivingEntity entity,
            String requestedForm,
            ResourceLocation targetSpecies
    ) {
        NativeFormEvolutionCompat.Eligibility eligibility = evaluateTransition(entity, requestedForm);
        if (!eligibility.eligible()) {
            return NativeFormEvolutionCompat.ApplyResult.failure(eligibility.detail());
        }

        String targetForm = normalize(requestedForm);
        String currentForm = currentForm(entity).orElse("");
        int beforeState = currentEvolutionState(entity).orElse(-1);
        try {
            invokeNativeEvolve(entity);

            // Never force a graph-selected variant after evolve(). setVariant() itself has native
            // per-variant side effects. The graph's native-form eligibility delegates base branching
            // to getEvolutionByBiomes(), so the native result must already equal the authored target.
            String actualForm = currentForm(entity).orElse("");
            if (!targetForm.equals(actualForm)) {
                ReMonsterIntegration.LOGGER.error(
                        "Graph-owned Direwolf transition disagreed with native state machine for {}: {} -> {} (expected {})",
                        entity.getUUID(),
                        currentForm,
                        actualForm,
                        targetForm
                );
                return NativeFormEvolutionCompat.ApplyResult.failure(
                        "Native Direwolf evolve produced " + actualForm + " instead of graph target " + targetForm
                );
            }

            int afterState = currentEvolutionState(entity).orElse(-1);
            return NativeFormEvolutionCompat.ApplyResult.success(
                    "native-form=" + targetSpecies
                            + " | direwolf=" + currentForm + "->" + targetForm
                            + " | evolutionState=" + beforeState + "->" + afterState
            );
        } catch (ReflectiveOperationException | LinkageError exception) {
            ReMonsterIntegration.LOGGER.error(
                    "Failed graph-owned Tensura Direwolf transition {} -> {} for {}",
                    currentForm,
                    targetForm,
                    entity.getUUID(),
                    exception
            );
            return NativeFormEvolutionCompat.ApplyResult.failure(
                    "Tensura Direwolf native form API could not be invoked: " + exception.getClass().getSimpleName()
            );
        }
    }

    public static Optional<String> currentForm(LivingEntity entity) {
        if (!isDirewolf(entity)) {
            return Optional.empty();
        }
        try {
            Method getVariant = entity.getClass().getMethod("getVariant");
            Object variant = getVariant.invoke(entity);
            if (variant instanceof Enum<?> enumValue) {
                return Optional.of(normalize(enumValue.name()));
            }
            return Optional.empty();
        } catch (ReflectiveOperationException | LinkageError exception) {
            ReMonsterIntegration.LOGGER.debug(
                    "Could not resolve native Direwolf variant for {}",
                    entity.getUUID(),
                    exception
            );
            return Optional.empty();
        }
    }

    public static boolean isNativeEvolutionInvocationAllowed() {
        return AUTHORIZED_NATIVE_DEPTH.get() > 0;
    }

    private static Optional<String> nativeBaseEvolutionSelection(LivingEntity entity) {
        try {
            Method method = entity.getClass().getMethod("getEvolutionByBiomes");
            Object variant = method.invoke(entity);
            if (variant instanceof Enum<?> enumValue) {
                return Optional.of(normalize(enumValue.name()));
            }
            return Optional.empty();
        } catch (ReflectiveOperationException | LinkageError exception) {
            ReMonsterIntegration.LOGGER.debug(
                    "Could not invoke native Direwolf environmental evolution selector for {}",
                    entity.getUUID(),
                    exception
            );
            return Optional.empty();
        }
    }

    private static void invokeNativeEvolve(LivingEntity entity) throws ReflectiveOperationException {
        Method evolve = entity.getClass().getMethod("evolve");
        AUTHORIZED_NATIVE_DEPTH.set(AUTHORIZED_NATIVE_DEPTH.get() + 1);
        try {
            evolve.invoke(entity);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof ReflectiveOperationException reflective) {
                throw reflective;
            }
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw exception;
        } finally {
            int remaining = AUTHORIZED_NATIVE_DEPTH.get() - 1;
            if (remaining <= 0) {
                AUTHORIZED_NATIVE_DEPTH.remove();
            } else {
                AUTHORIZED_NATIVE_DEPTH.set(remaining);
            }
        }
    }

    private static Optional<Integer> currentEvolutionState(LivingEntity entity) {
        return invokeIntGetter(entity, "getCurrentEvolutionState");
    }

    private static Optional<Integer> maxEvolutionState(LivingEntity entity) {
        return invokeIntGetter(entity, "getMaxEvolutionState");
    }

    private static Optional<Integer> invokeIntGetter(LivingEntity entity, String methodName) {
        try {
            Method method = entity.getClass().getMethod(methodName);
            Object value = method.invoke(entity);
            return value instanceof Number number ? Optional.of(number.intValue()) : Optional.empty();
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return Optional.empty();
        }
    }

    private static boolean isDirewolf(LivingEntity entity) {
        return DIREWOLF_ENTITY.equals(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
    }

    private static String normalize(String form) {
        return form == null ? "" : form.trim().toLowerCase(Locale.ROOT);
    }
}
