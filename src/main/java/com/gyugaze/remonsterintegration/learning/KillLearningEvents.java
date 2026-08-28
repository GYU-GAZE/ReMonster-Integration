package com.gyugaze.remonsterintegration.learning;

import com.gyugaze.remonsterintegration.ReMonsterIntegration;
import com.gyugaze.remonsterintegration.compat.MobChampionsBridge;
import com.gyugaze.remonsterintegration.growth.ExternalEnergyFoundationService;
import com.gyugaze.remonsterintegration.profile.MobProfile;
import com.gyugaze.remonsterintegration.profile.MobProfileRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.List;
import java.util.Optional;

/** NeoForge event bridge for non-player kill-driven native acquisition/mastery learning. */
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

        // The target pack deliberately keeps Tensura: Skill Obtain as the player-facing kill-skill
        // authority. It provides authored EntityType -> skill drop tables with independent chances and
        // direct native SkillHelper.learnSkill delivery. Re:Monster owns the complementary non-player
        // path so MCA villagers, subordinates, pets and wild compatible mobs can gradually acquire the
        // real skills/profile skills of what they defeat. Never process players here, even if the
        // external addon is temporarily missing, or the two semantics can silently diverge by pack.
        if (learner instanceof ServerPlayer) {
            return;
        }

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
