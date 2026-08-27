package com.gyugaze.remonsterintegration.evolution;

import com.gyugaze.remonsterintegration.ReMonsterIntegration;
import com.gyugaze.remonsterintegration.mca.McaSubordinateAdapter;
import com.gyugaze.remonsterintegration.tensura.TensuraExistenceBridge;
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
            return EvolutionResult.failure(null, targetSpecies, "Target has no Re:Monster semantic species state");
        }

        ResourceLocation current = currentOptional.get();
        Optional<EvolutionDefinition> definitionOptional = EvolutionRegistry.find(current, targetSpecies);
        if (definitionOptional.isEmpty()) {
            return EvolutionResult.failure(current, targetSpecies, "No loaded evolution edge connects these species");
        }

        EvolutionDefinition definition = definitionOptional.get();
        double progressionEp = TensuraExistenceBridge.getBaseMaxEp(entity);
        if (progressionEp < definition.minimumEp()) {
            return EvolutionResult.failure(
                    current,
                    targetSpecies,
                    "EP requirement not met: " + progressionEp + " / " + definition.minimumEp()
            );
        }

        String bodyResult = "semantic-only";
        LivingEntity evolvedEntity = entity;
        Optional<SpeciesBodyProfile> profile = SpeciesBodyProfileRegistry.get(targetSpecies);
        Optional<SpeciesBodyProfile.TownsteadBody> townsteadBody = profile.flatMap(SpeciesBodyProfile::townstead);
        Optional<SpeciesBodyProfile.EntityBody> entityBody = profile.flatMap(SpeciesBodyProfile::entityBody);
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

        SpeciesStateService.setSpecies(evolvedEntity, targetSpecies);
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
