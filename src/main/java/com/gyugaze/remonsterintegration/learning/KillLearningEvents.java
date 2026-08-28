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
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.List;
import java.util.Optional;

/** NeoForge event bridge for universal kill-driven skill learning. */
public final class KillLearningEvents {
    private static final String LEGACY_SKILL_OBTAIN_MOD_ID = "tensura_skillobtain";
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
        // Legacy Tensura: Skill Obtain directly grants configured skills to players on kill. While that
        // mod is still present during pack migration, let it own the player path so one kill cannot both
        // direct-grant and advance a second Re:Monster acquisition for the same event. Non-player
        // LivingEntity learners remain Re:Monster-owned. The target pack should remove the legacy addon.
        if (learner instanceof ServerPlayer && ModList.get().isLoaded(LEGACY_SKILL_OBTAIN_MOD_ID)) {
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
