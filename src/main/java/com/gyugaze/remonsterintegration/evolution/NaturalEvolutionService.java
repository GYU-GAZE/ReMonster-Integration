package com.gyugaze.remonsterintegration.evolution;

import com.gyugaze.remonsterintegration.tensura.TensuraSubordinateBridge;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;

import java.util.List;
import java.util.Optional;

/**
 * Graduates the data-driven evolution graph from pack-author commands into ordinary mob gameplay.
 *
 * Entity-body profiles provide a reversible entity-type -> semantic-species seed. Wild mobs then
 * automatically take exactly one unambiguous eligible evolution edge after a short debounce. Combat
 * is deliberately not a blocker: a creature can evolve in the middle of a fight when its authored
 * conditions become valid. Owned mobs never auto-pick a branch; their existing server-authoritative
 * evolution UI remains the choice surface.
 */
public final class NaturalEvolutionService {
    private static final String READY_EDGE_KEY = "remonster_natural_evolution_ready_edge";
    private static final String READY_SINCE_KEY = "remonster_natural_evolution_ready_since";
    private static final long READY_DEBOUNCE_TICKS = 100L;

    private NaturalEvolutionService() {
    }

    public static void tick(Mob mob) {
        if (mob.level().isClientSide() || !mob.isAlive()) {
            return;
        }

        seedSpeciesFromBody(mob);

        if ((mob.tickCount + mob.getId()) % 20 != 0) {
            return;
        }

        long gameTime = mob.level().getGameTime();

        // Never silently transform another character's companion. Owned branch choices already have UI.
        if (isOwned(mob)) {
            clearReadyState(mob);
            return;
        }

        Optional<ResourceLocation> species = SpeciesStateService.getSpecies(mob);
        if (species.isEmpty()) {
            clearReadyState(mob);
            return;
        }

        List<EvolutionDefinition> eligible = EvolutionRegistry.availableFrom(species.get()).stream()
                .filter(definition -> EvolutionRequirementService.isSatisfied(mob, definition))
                .toList();

        // Multiple valid wild branches must be disambiguated by authored conditions, never a coin flip.
        if (eligible.size() != 1) {
            clearReadyState(mob);
            return;
        }

        EvolutionDefinition definition = eligible.getFirst();
        String edgeId = definition.id().toString();
        String previousEdge = mob.getPersistentData().getString(READY_EDGE_KEY);
        if (!edgeId.equals(previousEdge)) {
            mob.getPersistentData().putString(READY_EDGE_KEY, edgeId);
            mob.getPersistentData().putLong(READY_SINCE_KEY, gameTime);
            return;
        }

        long readySince = mob.getPersistentData().getLong(READY_SINCE_KEY);
        if (readySince <= 0L || gameTime - readySince < READY_DEBOUNCE_TICKS) {
            return;
        }

        EvolutionResult result = EvolutionService.evolve(mob, definition.toSpecies());
        if (!result.success()) {
            clearReadyState(mob);
        }
    }

    public static boolean seedSpeciesFromBody(Mob mob) {
        if (SpeciesStateService.getSpecies(mob).isPresent()) {
            return false;
        }
        ResourceLocation entityType = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        Optional<ResourceLocation> species = SpeciesBodyProfileRegistry.speciesForEntityType(entityType);
        if (species.isEmpty()) {
            return false;
        }
        SpeciesStateService.setSpecies(mob, species.get());
        return true;
    }

    private static boolean isOwned(Mob mob) {
        if (TensuraSubordinateBridge.getResolvedOwnerUuid(mob).isPresent()) {
            return true;
        }
        return mob instanceof OwnableEntity ownable && ownable.getOwnerUUID() != null;
    }

    private static void clearReadyState(Mob mob) {
        mob.getPersistentData().remove(READY_EDGE_KEY);
        mob.getPersistentData().remove(READY_SINCE_KEY);
    }
}
