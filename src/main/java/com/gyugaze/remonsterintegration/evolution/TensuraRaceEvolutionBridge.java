package com.gyugaze.remonsterintegration.evolution;

import com.gyugaze.remonsterintegration.ReMonsterIntegration;
import io.github.manasmods.manascore.race.api.ManasRace;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.ModList;

import java.util.Optional;

/**
 * Makes an authored Re:Monster species binding and its ManasCore/Tensura race one runtime identity.
 * Native RaceStorage remains authoritative for the race lifecycle; Re:Monster supplies body metadata,
 * extra evolution edges and common presentation around that exact native identity.
 */
public final class TensuraRaceEvolutionBridge {
    private TensuraRaceEvolutionBridge() {
    }

    public static void afterNativeRaceSet(LivingEntity entity, ManasRace newRace, boolean evolution) {
        if (entity.level().isClientSide() || newRace == null) {
            return;
        }

        ResourceLocation nativeRaceId = newRace.getRegistryName();
        Optional<ResourceLocation> projectTarget = SpeciesBodyProfileRegistry.speciesForNativeRace(nativeRaceId);

        if (projectTarget.isPresent()) {
            ResourceLocation targetSpecies = projectTarget.get();
            Component actorName = entity.getDisplayName().copy();
            applyProjectBody(entity, targetSpecies);
            SpeciesStateService.setStoredSpeciesFromNative(entity, targetSpecies);
            SpeciesScaleService.applyCurrent(entity);

            if (evolution) {
                EvolutionFeedbackService.announce(entity, actorName, targetSpecies);
            }
            ReMonsterIntegration.LOGGER.info(
                    "Native race synchronized for {}: race={} <-> species={} evolution={}",
                    entity.getUUID(),
                    nativeRaceId,
                    targetSpecies,
                    evolution
            );
            return;
        }

        // If a bound project species was left behind by an admin/debug race replacement, remove that
        // stale mirror. Project-only species remain untouched because they deliberately have no race.
        SpeciesStateService.getStoredSpecies(entity).ifPresent(stored -> {
            if (SpeciesBodyProfileRegistry.nativeRaceForSpecies(stored).isPresent()) {
                SpeciesStateService.clearSpecies(entity);
            }
        });

        if (evolution) {
            EvolutionFeedbackService.announce(entity, entity.getDisplayName().copy(), nativeRaceId);
        }
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
