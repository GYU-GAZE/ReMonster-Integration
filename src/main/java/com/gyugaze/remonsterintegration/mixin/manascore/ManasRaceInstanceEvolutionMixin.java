package com.gyugaze.remonsterintegration.mixin.manascore;

import com.gyugaze.remonsterintegration.evolution.EvolutionDefinition;
import com.gyugaze.remonsterintegration.evolution.EvolutionRegistry;
import com.gyugaze.remonsterintegration.evolution.EvolutionRequirementService;
import com.gyugaze.remonsterintegration.evolution.SpeciesBodyProfileRegistry;
import com.gyugaze.remonsterintegration.evolution.SpeciesStateService;
import com.gyugaze.remonsterintegration.tensura.TensuraExistenceBridge;
import io.github.manasmods.manascore.race.api.ManasRace;
import io.github.manasmods.manascore.race.api.ManasRaceInstance;
import io.github.manasmods.manascore.race.api.RaceAPI;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Makes authored Re:Monster branches first-class options in ManasCore's normal evolution UI. */
@Mixin(value = ManasRaceInstance.class, remap = false)
public abstract class ManasRaceInstanceEvolutionMixin {
    @Inject(method = "getNextEvolutions", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private void remonster$appendProjectEvolutions(
            LivingEntity entity,
            CallbackInfoReturnable<List<ManasRace>> cir
    ) {
        Optional<ResourceLocation> currentSpecies = SpeciesStateService.getSpecies(entity);
        if (currentSpecies.isEmpty()) {
            return;
        }

        List<EvolutionDefinition> projectEdges = EvolutionRegistry.availableFrom(currentSpecies.get());
        if (projectEdges.isEmpty()) {
            return;
        }

        List<ManasRace> result = new ArrayList<>(cir.getReturnValue());
        boolean changed = false;
        for (EvolutionDefinition edge : projectEdges) {
            Optional<ResourceLocation> nativeTargetId = SpeciesBodyProfileRegistry.nativeRaceForSpecies(edge.toSpecies());
            if (nativeTargetId.isEmpty()) {
                continue;
            }
            ManasRace nativeTarget = RaceAPI.getRaceRegistry().get(nativeTargetId.get());
            if (nativeTarget != null && !result.contains(nativeTarget)) {
                result.add(nativeTarget);
                changed = true;
            }
        }

        if (changed) {
            cir.setReturnValue(List.copyOf(result));
        }
    }

    @Inject(method = "getEvolutionProgress", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private void remonster$projectEvolutionProgress(
            LivingEntity entity,
            ManasRace targetRace,
            CallbackInfoReturnable<Float> cir
    ) {
        Optional<EvolutionDefinition> definition = findProjectEdge(entity, targetRace);
        if (definition.isEmpty()) {
            return;
        }

        EvolutionDefinition edge = definition.get();
        if (EvolutionRequirementService.isSatisfied(entity, edge)) {
            cir.setReturnValue(1.0F);
            return;
        }

        double minimumEp = edge.minimumEp();
        if (minimumEp <= 0.0D) {
            cir.setReturnValue(0.99F);
            return;
        }

        double ep = TensuraExistenceBridge.getBaseMaxEp(entity);
        float epProgress = (float) Mth.clamp(ep / minimumEp, 0.0D, 0.99D);
        cir.setReturnValue(epProgress);
    }

    private static Optional<EvolutionDefinition> findProjectEdge(LivingEntity entity, ManasRace targetRace) {
        if (targetRace == null) {
            return Optional.empty();
        }
        ResourceLocation nativeTargetId = targetRace.getRegistryName();
        return SpeciesStateService.getSpecies(entity).stream()
                .flatMap(source -> EvolutionRegistry.availableFrom(source).stream())
                .filter(edge -> SpeciesBodyProfileRegistry.nativeRaceForSpecies(edge.toSpecies())
                        .filter(nativeTargetId::equals)
                        .isPresent())
                .findFirst();
    }
}
