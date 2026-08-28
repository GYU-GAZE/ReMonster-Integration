package com.gyugaze.remonsterintegration.tensura;

import com.gyugaze.remonsterintegration.ReMonsterIntegration;
import com.gyugaze.remonsterintegration.evolution.NativeFormEvolutionCompat;
import io.github.manasmods.tensura.entity.monster.DirewolfEntity;
import io.github.manasmods.tensura.entity.variant.DirewolfVariant;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Narrow adapter for Tensura 2.0.1.2's same-EntityType Direwolf evolution machine.
 *
 * Re:Monster owns eligibility and target selection. Tensura still owns the real Direwolf entity,
 * evolution state, stat buff and variant implementation. Native autonomous calls to evolve() are
 * blocked by DirewolfEvolutionAuthorityMixin; this service temporarily authorizes exactly one native
 * step after the Re:Monster graph has approved a target.
 */
public final class TensuraDirewolfEvolutionCompat {
    private static final ResourceLocation DIREWOLF_ENTITY =
            ResourceLocation.fromNamespaceAndPath("tensura", "direwolf");

    private static final Map<String, DirewolfVariant> BASE_FANG_TARGETS = Map.of(
            "red_fang", DirewolfVariant.RED_FANG,
            "green_fang", DirewolfVariant.GREEN_FANG,
            "blue_fang", DirewolfVariant.BLUE_FANG,
            "brown_fang", DirewolfVariant.BROWN_FANG,
            "purple_fang", DirewolfVariant.PURPLE_FANG,
            "black_fang", DirewolfVariant.BLACK_FANG
    );

    /** Deterministic native follow-up steps confirmed in the target Tensura Direwolf state machine. */
    private static final Map<DirewolfVariant, DirewolfVariant> DETERMINISTIC_NATIVE_STEPS = Map.of(
            DirewolfVariant.RED_FANG, DirewolfVariant.MYSTIC_FIRE_WOLF,
            DirewolfVariant.GREEN_FANG, DirewolfVariant.MYSTIC_WIND_WOLF,
            DirewolfVariant.BLUE_FANG, DirewolfVariant.MYSTIC_WATER_WOLF,
            DirewolfVariant.BROWN_FANG, DirewolfVariant.MYSTIC_EARTH_WOLF,
            DirewolfVariant.PURPLE_FANG, DirewolfVariant.MYSTIC_SPACE_WOLF,
            DirewolfVariant.BLACK_FANG, DirewolfVariant.MYSTIC_WOLF
    );

    private static final ThreadLocal<Integer> AUTHORIZED_NATIVE_DEPTH = ThreadLocal.withInitial(() -> 0);

    private TensuraDirewolfEvolutionCompat() {
    }

    public static NativeFormEvolutionCompat.ApplyResult apply(
            LivingEntity entity,
            String requestedForm,
            ResourceLocation targetSpecies
    ) {
        if (!(entity instanceof DirewolfEntity direwolf)) {
            return NativeFormEvolutionCompat.ApplyResult.failure(
                    "Native Direwolf form target requires physical entity " + DIREWOLF_ENTITY
            );
        }

        Optional<DirewolfVariant> targetOptional = parseVariant(requestedForm);
        if (targetOptional.isEmpty()) {
            return NativeFormEvolutionCompat.ApplyResult.failure(
                    "Unknown Tensura Direwolf form: " + requestedForm
            );
        }

        DirewolfVariant currentVariant = direwolf.getVariant();
        DirewolfVariant targetVariant = targetOptional.get();
        String currentForm = normalize(currentVariant);
        String targetForm = normalize(targetVariant);

        if (currentVariant == targetVariant) {
            return NativeFormEvolutionCompat.ApplyResult.failure(
                    "Direwolf is already in native form " + targetForm
            );
        }

        boolean graphSelectedBaseFang = currentVariant == DirewolfVariant.DIREWOLF
                && targetVariant == BASE_FANG_TARGETS.get(targetForm);
        boolean deterministicNativeStep = targetVariant == DETERMINISTIC_NATIVE_STEPS.get(currentVariant);
        if (!graphSelectedBaseFang && !deterministicNativeStep) {
            return NativeFormEvolutionCompat.ApplyResult.failure(
                    "Unsupported Direwolf native form transition: " + currentForm + " -> " + targetForm
            );
        }

        int beforeState = direwolf.getCurrentEvolutionState();
        try {
            invokeNativeEvolve(direwolf);

            // The base Direwolf's upstream evolve() selects a Fang from the environment itself. The
            // graph already made that choice, so retain Tensura's state/stat mutation and replace only
            // the resulting variant with the graph-selected equivalent Fang.
            if (graphSelectedBaseFang) {
                direwolf.setVariant(targetVariant);
            }

            DirewolfVariant actualVariant = direwolf.getVariant();
            if (actualVariant != targetVariant) {
                ReMonsterIntegration.LOGGER.error(
                        "Graph-owned Direwolf transition produced unexpected native form for {}: {} -> {} (expected {})",
                        entity.getUUID(),
                        currentForm,
                        normalize(actualVariant),
                        targetForm
                );
                return NativeFormEvolutionCompat.ApplyResult.failure(
                        "Native Direwolf evolve produced " + normalize(actualVariant) + " instead of " + targetForm
                );
            }

            int afterState = direwolf.getCurrentEvolutionState();
            return NativeFormEvolutionCompat.ApplyResult.success(
                    "native-form=" + targetSpecies
                            + " | direwolf=" + currentForm + "->" + targetForm
                            + " | evolutionState=" + beforeState + "->" + afterState
            );
        } catch (RuntimeException | LinkageError exception) {
            ReMonsterIntegration.LOGGER.error(
                    "Failed graph-owned Tensura Direwolf transition {} -> {} for {}",
                    currentForm,
                    targetForm,
                    entity.getUUID(),
                    exception
            );
            return NativeFormEvolutionCompat.ApplyResult.failure(
                    "Tensura Direwolf native form API failed: " + exception.getClass().getSimpleName()
            );
        }
    }

    public static Optional<String> currentForm(LivingEntity entity) {
        if (!(entity instanceof DirewolfEntity direwolf)) {
            return Optional.empty();
        }
        return Optional.of(normalize(direwolf.getVariant()));
    }

    public static boolean isNativeEvolutionInvocationAllowed() {
        return AUTHORIZED_NATIVE_DEPTH.get() > 0;
    }

    private static void invokeNativeEvolve(DirewolfEntity direwolf) {
        AUTHORIZED_NATIVE_DEPTH.set(AUTHORIZED_NATIVE_DEPTH.get() + 1);
        try {
            direwolf.evolve();
        } finally {
            int remaining = AUTHORIZED_NATIVE_DEPTH.get() - 1;
            if (remaining <= 0) {
                AUTHORIZED_NATIVE_DEPTH.remove();
            } else {
                AUTHORIZED_NATIVE_DEPTH.set(remaining);
            }
        }
    }

    private static Optional<DirewolfVariant> parseVariant(String form) {
        if (form == null || form.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(DirewolfVariant.valueOf(form.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    private static String normalize(DirewolfVariant variant) {
        return variant.name().toLowerCase(Locale.ROOT);
    }
}
