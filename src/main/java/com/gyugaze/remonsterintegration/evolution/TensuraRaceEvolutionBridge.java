package com.gyugaze.remonsterintegration.evolution;

import com.gyugaze.remonsterintegration.ReMonsterIntegration;
import io.github.manasmods.manascore.race.api.ManasRace;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.ModList;

import java.util.Optional;

/**
 * Keeps player/native ManasCore race evolution and Re:Monster semantic/Townstead state in one flow.
 *
 * Native Tensura remains authoritative for the actual race. Re:Monster observes a successful native
 * evolution, maps it to a project species when one exists, applies the project physical body recipe,
 * and emits the same evolution presentation used by MCA/external creature evolution.
 */
public final class TensuraRaceEvolutionBridge {
    private TensuraRaceEvolutionBridge() {
    }

    public static void afterNativeEvolution(LivingEntity entity, ManasRace newRace) {
        if (entity.level().isClientSide() || newRace == null) {
            return;
        }

        ResourceLocation nativeRaceId = newRace.getRegistryName();
        Component actorName = entity.getDisplayName().copy();
        Optional<ResourceLocation> projectTarget = resolveProjectTarget(entity, nativeRaceId);

        if (projectTarget.isPresent()) {
            ResourceLocation targetSpecies = projectTarget.get();
            applyProjectBody(entity, targetSpecies);
            SpeciesStateService.setSpecies(entity, targetSpecies);
            SpeciesScaleService.applyCurrent(entity);
            EvolutionFeedbackService.announce(entity, actorName, targetSpecies);
            ReMonsterIntegration.LOGGER.info(
                    "Native race evolution synchronized for {}: race={} -> semantic={}",
                    entity.getUUID(),
                    nativeRaceId,
                    targetSpecies
            );
            return;
        }

        // Even ordinary Tensura-only evolutions should use the common Re:Monster presentation.
        EvolutionFeedbackService.announce(entity, actorName, nativeRaceId);
    }

    /**
     * Prefer a concrete outgoing project edge from the entity's current semantic species. This is what
     * allows a semantic Hobgoblin to select native tensura:orc / tensura:ogre from the Tensura menu.
     * If there is no outgoing edge, fall back to a project species with the same race path when one is
     * explicitly authored (for example native Hobgoblin becoming the project Hobgoblin body).
     */
    public static Optional<ResourceLocation> resolveProjectTarget(
            LivingEntity entity,
            ResourceLocation nativeRaceId
    ) {
        Optional<ResourceLocation> current = SpeciesStateService.getSpecies(entity);
        if (current.isPresent()) {
            Optional<ResourceLocation> edgeTarget = EvolutionRegistry.availableFrom(current.get()).stream()
                    .map(EvolutionDefinition::toSpecies)
                    .filter(target -> target.getPath().equals(nativeRaceId.getPath()))
                    .findFirst();
            if (edgeTarget.isPresent()) {
                return edgeTarget;
            }
        }

        ResourceLocation samePath = ResourceLocation.fromNamespaceAndPath(
                ReMonsterIntegration.MOD_ID,
                nativeRaceId.getPath()
        );
        return SpeciesBodyProfileRegistry.get(samePath).isPresent()
                ? Optional.of(samePath)
                : Optional.empty();
    }

    private static void applyProjectBody(LivingEntity entity, ResourceLocation targetSpecies) {
        Optional<SpeciesBodyProfile.TownsteadBody> body = SpeciesBodyProfileRegistry.get(targetSpecies)
                .flatMap(SpeciesBodyProfile::townstead);
        if (body.isEmpty() || !ModList.get().isLoaded("townstead")) {
            return;
        }

        TownsteadEvolutionCompat.ApplyResult result = TownsteadEvolutionCompat.apply(
                entity,
                targetSpecies,
                body.get()
        );
        if (!result.success()) {
            ReMonsterIntegration.LOGGER.warn(
                    "Native race {} changed successfully for {}, but Townstead body sync failed: {}",
                    targetSpecies,
                    entity.getUUID(),
                    result.failure()
            );
        }
    }
}
