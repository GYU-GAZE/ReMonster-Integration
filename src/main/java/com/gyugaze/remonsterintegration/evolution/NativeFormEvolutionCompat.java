package com.gyugaze.remonsterintegration.evolution;

import com.gyugaze.remonsterintegration.tensura.TensuraDirewolfEvolutionCompat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

/** Dispatches graph-owned same-EntityType transitions to narrow source-mod adapters. */
public final class NativeFormEvolutionCompat {
    public static final ResourceLocation TENSURA_DIREWOLF =
            ResourceLocation.fromNamespaceAndPath("tensura", "direwolf");

    private NativeFormEvolutionCompat() {
    }

    /**
     * Lets the native adapter validate state/branch constraints before an edge is exposed as eligible.
     * The graph still owns whether an edge exists and its authored Re:Monster requirements; the source
     * mod remains the source of truth for native form-selection semantics that would otherwise be copied.
     */
    public static Eligibility evaluateEligibility(
            LivingEntity entity,
            SpeciesBodyProfile.NativeFormBody body
    ) {
        if (TENSURA_DIREWOLF.equals(body.adapter())) {
            return TensuraDirewolfEvolutionCompat.evaluateTransition(entity, body.form());
        }
        return Eligibility.failure("Unsupported native-form adapter: " + body.adapter());
    }

    public static ApplyResult apply(
            LivingEntity entity,
            SpeciesBodyProfile.NativeFormBody body,
            ResourceLocation targetSpecies
    ) {
        if (TENSURA_DIREWOLF.equals(body.adapter())) {
            return TensuraDirewolfEvolutionCompat.apply(entity, body.form(), targetSpecies);
        }
        return ApplyResult.failure("Unsupported native-form adapter: " + body.adapter());
    }

    /**
     * Resolves a source-mod form back into its Re:Monster semantic species when a matching profile is loaded.
     * This runs before the generic EntityType seed so naturally-existing native forms do not collapse to the
     * base species simply because they share one EntityType.
     */
    public static Optional<ResourceLocation> resolveCurrentSpecies(LivingEntity entity) {
        Optional<String> direwolfForm = TensuraDirewolfEvolutionCompat.currentForm(entity);
        if (direwolfForm.isPresent()) {
            return SpeciesBodyProfileRegistry.speciesForNativeForm(TENSURA_DIREWOLF, direwolfForm.get());
        }
        return Optional.empty();
    }

    public record Eligibility(boolean eligible, String detail) {
        public static Eligibility success(String detail) {
            return new Eligibility(true, detail);
        }

        public static Eligibility failure(String detail) {
            return new Eligibility(false, detail);
        }
    }

    public record ApplyResult(boolean success, String detail) {
        public static ApplyResult success(String detail) {
            return new ApplyResult(true, detail);
        }

        public static ApplyResult failure(String detail) {
            return new ApplyResult(false, detail);
        }
    }
}
