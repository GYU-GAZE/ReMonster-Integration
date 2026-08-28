package com.gyugaze.remonsterintegration.evolution;

import com.gyugaze.remonsterintegration.ReMonsterIntegration;
import com.gyugaze.remonsterintegration.mca.McaSubordinateAdapter;
import io.github.manasmods.manascore.race.api.ManasRace;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.race.api.Races;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.ModList;

import java.util.Objects;
import java.util.Optional;

public final class EvolutionService {
    private EvolutionService() {
    }

    public static EvolutionResult evolve(LivingEntity entity, ResourceLocation targetSpecies) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(targetSpecies, "targetSpecies");

        if (entity.level().isClientSide()) {
            return EvolutionResult.failure(null, targetSpecies, "Evolution must run on the logical server");
        }

        Optional<ResourceLocation> currentOptional = SpeciesStateService.getSpecies(entity);
        if (currentOptional.isEmpty()) {
            return EvolutionResult.failure(null, targetSpecies, "Target has no Re:Monster species identity");
        }

        ResourceLocation current = currentOptional.get();
        Optional<EvolutionDefinition> definitionOptional = EvolutionRegistry.find(current, targetSpecies);
        if (definitionOptional.isEmpty()) {
            return EvolutionResult.failure(current, targetSpecies, "No loaded evolution edge connects these species");
        }

        EvolutionDefinition definition = definitionOptional.get();
        EvolutionRequirementService.Evaluation evaluation = EvolutionRequirementService.evaluate(entity, definition);
        if (!evaluation.eligible()) {
            return EvolutionResult.failure(current, targetSpecies, evaluation.failureReason());
        }

        // Bound species are native Tensura races, not a second semantic race layered on top. Delegate the
        // authoritative transition to RaceStorage so onRaceEvolution, intrinsic skills, attributes and all
        // native hooks run exactly once. RaceStorageEvolutionMixin mirrors the project body and feedback.
        Optional<ResourceLocation> nativeRaceId = SpeciesBodyProfileRegistry.nativeRaceForSpecies(targetSpecies);
        if (nativeRaceId.isPresent()) {
            ManasRace nativeRace = RaceAPI.getRaceRegistry().get(nativeRaceId.get());
            if (nativeRace == null) {
                return EvolutionResult.failure(
                        current,
                        targetSpecies,
                        "Bound native race is not registered: " + nativeRaceId.get()
                );
            }

            Races races = RaceAPI.getRaceFrom(entity);
            if (!races.evolveRace(nativeRace)) {
                return EvolutionResult.failure(
                        current,
                        targetSpecies,
                        "Native RaceStorage rejected evolution to " + nativeRaceId.get()
                );
            }

            ReMonsterIntegration.LOGGER.info(
                    "Evolution delegated to native race for {}: {} -> {} ({})",
                    entity.getUUID(),
                    current,
                    targetSpecies,
                    nativeRaceId.get()
            );
            return EvolutionResult.success(
                    current,
                    targetSpecies,
                    "native-race=" + nativeRaceId.get() + " | project body synchronized"
            );
        }

        Component actorName = entity.getDisplayName().copy();
        String bodyResult = "semantic-only";
        LivingEntity evolvedEntity = entity;
        Optional<SpeciesBodyProfile> profile = SpeciesBodyProfileRegistry.get(targetSpecies);
        Optional<SpeciesBodyProfile.TownsteadBody> townsteadBody = profile.flatMap(SpeciesBodyProfile::townstead);
        Optional<SpeciesBodyProfile.EntityBody> entityBody = profile.flatMap(SpeciesBodyProfile::entityBody);
        Optional<SpeciesBodyProfile.NativeFormBody> nativeForm = profile.flatMap(SpeciesBodyProfile::nativeForm);
        boolean townsteadTarget = entity instanceof ServerPlayer || McaSubordinateAdapter.isMcaVillager(entity);

        if (townsteadTarget && townsteadBody.isPresent()) {
            if (!ModList.get().isLoaded("townstead")) {
                return EvolutionResult.failure(
                        current,
                        targetSpecies,
                        "Target species requires a Townstead body but Townstead is not loaded"
                );
            }

            TownsteadEvolutionCompat.ApplyResult physical =
                    TownsteadEvolutionCompat.apply(entity, targetSpecies, townsteadBody.orElseThrow());
            if (!physical.success()) {
                return EvolutionResult.failure(current, targetSpecies, physical.failure());
            }
            bodyResult = physical.description();
        } else if (!townsteadTarget && nativeForm.isPresent()) {
            NativeFormEvolutionCompat.ApplyResult physical = NativeFormEvolutionCompat.apply(
                    entity,
                    nativeForm.orElseThrow(),
                    targetSpecies
            );
            if (!physical.success()) {
                return EvolutionResult.failure(current, targetSpecies, physical.detail());
            }
            bodyResult = physical.detail();
        } else if (!townsteadTarget && entityBody.isPresent()) {
            GenericMobEvolutionCompat.ApplyResult physical = GenericMobEvolutionCompat.apply(
                    entity,
                    entityBody.orElseThrow(),
                    targetSpecies
            );
            if (!physical.success()) {
                return EvolutionResult.failure(current, targetSpecies, physical.failure());
            }
            evolvedEntity = physical.entity();
            bodyResult = physical.detail();
        }

        if (townsteadTarget) {
            double targetScale = townsteadBody
                    .map(SpeciesBodyProfile.TownsteadBody::scale)
                    .orElse(1.0D);
            SpeciesScaleService.apply(evolvedEntity, targetScale);
            bodyResult += " | townsteadScale=" + targetScale;
        }

        if (!SpeciesStateService.setSpecies(evolvedEntity, targetSpecies)) {
            return EvolutionResult.failure(current, targetSpecies, "Failed to synchronize target species identity");
        }
        EvolutionFeedbackService.announce(evolvedEntity, actorName, targetSpecies);
        ReMonsterIntegration.LOGGER.info(
                "Evolution applied to {}: {} -> {} ({})",
                evolvedEntity.getUUID(),
                current,
                targetSpecies,
                bodyResult
        );
        return EvolutionResult.success(current, targetSpecies, bodyResult);
    }
}
