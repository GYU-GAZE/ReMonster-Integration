package com.gyugaze.remonsterintegration.learning;

import com.gyugaze.remonsterintegration.ReMonsterIntegration;
import com.gyugaze.remonsterintegration.compat.MobChampionsBridge;
import com.gyugaze.remonsterintegration.growth.ExternalEnergyFoundationService;
import com.gyugaze.remonsterintegration.profile.MobProfile;
import com.gyugaze.remonsterintegration.profile.MobProfileRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.List;
import java.util.Optional;

/** NeoForge event bridge for universal kill-driven native acquisition/mastery learning. */
public final class KillLearningEvents {
    private static final double DEFAULT_BASE_LEARNING_POINTS = 1.0;

    private KillLearningEvents() {
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity defeated = event.getEntity();
        if (defeated.level().isClientSide()) {
            return;
        }

        Optional<LivingEntity> creditedLearner = KillCreditResolver.resolveLearner(event.getSource());
        if (creditedLearner.isEmpty()) {
            return;
        }

        LivingEntity learner = creditedLearner.get();

        // Re:Monster is the single universal kill-learning authority. Players and non-players use the
        // same real ManasCore/Tensura acquisition/mastery representation, while the defeated mob's
        // actual skill storage plus Mob Profile declarations provide the source skills. The target pack
        // removes Tensura: Skill Obtain rather than maintaining a second EntityType -> direct-drop table.

        // Tensura's own death handler may have expanded this external killer's aura/magicule capacity.
        // Preserve its previous fill fraction before the acquisition attempt pays native energy costs.
        ExternalEnergyFoundationService.syncCapacityGrowth(learner);

        Optional<MobProfile> profile = MobProfileRegistry.get(defeated);
        double championMultiplier = MobChampionsBridge.getChampionRank(defeated)
                .map(KillLearningEvents::getChampionLearningMultiplier)
                .orElse(1.0);

        List<ResourceLocation> progressed = KillLearningService.grantFromDefeatedEntity(
                learner,
                defeated,
                profile,
                DEFAULT_BASE_LEARNING_POINTS * championMultiplier
        );

        if (!progressed.isEmpty()) {
            ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(defeated.getType());
            ReMonsterIntegration.LOGGER.debug(
                    "Entity {} gained acquisition-learning progress from {} for {} (profile={}, MobChampions multiplier x{})",
                    learner.getUUID(),
                    entityId,
                    progressed,
                    profile.isPresent(),
                    championMultiplier
            );
        }
    }

    private static double getChampionLearningMultiplier(MobChampionsBridge.ChampionRank rank) {
        return switch (rank) {
            case UNCOMMON -> 1.25;
            case RARE -> 1.5;
            case EPIC -> 2.0;
            case LEGENDARY -> 3.0;
        };
    }
}
