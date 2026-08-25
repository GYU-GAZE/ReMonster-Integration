package com.gyugaze.remonsterintegration.learning;

import com.gyugaze.remonsterintegration.ReMonsterIntegration;
import com.gyugaze.remonsterintegration.compat.MobChampionsBridge;
import com.gyugaze.remonsterintegration.profile.MobProfileRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.List;
import java.util.Optional;

/**
 * NeoForge event bridge for profile-driven skill learning.
 */
public final class KillLearningEvents {
    private static final double DEFAULT_BASE_LEARNING_POINTS = 1.0;

    private KillLearningEvents() {
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity defeated = event.getEntity();
        if (defeated.level().isClientSide()) {
            return;
        }

        Optional<ServerPlayer> creditedPlayer = KillCreditResolver.resolve(event.getSource());
        if (creditedPlayer.isEmpty()) {
            return;
        }

        ServerPlayer player = creditedPlayer.get();
        MobProfileRegistry.get(defeated).ifPresent(profile -> {
            double championMultiplier = MobChampionsBridge.getChampionRank(defeated)
                    .map(KillLearningEvents::getChampionLearningMultiplier)
                    .orElse(1.0);

            List<ResourceLocation> progressed = KillLearningService.grantFromProfile(
                    player,
                    profile,
                    DEFAULT_BASE_LEARNING_POINTS * championMultiplier
            );

            if (!progressed.isEmpty()) {
                ReMonsterIntegration.LOGGER.debug(
                        "Player {} gained acquisition-learning progress from {} for {} (MobChampions multiplier x{})",
                        player.getGameProfile().getName(),
                        profile.entityId(),
                        progressed,
                        championMultiplier
                );
            }
        });
    }

    /**
     * Initial reward-only integration with MobChampions.
     *
     * MobChampions remains the sole owner of champion combat stats. Re:Monster Integration only uses
     * the rank as additional reward context so a much harder variant is also more valuable to hunt.
     * These values are deliberately conservative and can move to pack config once runtime balance is known.
     */
    private static double getChampionLearningMultiplier(MobChampionsBridge.ChampionRank rank) {
        return switch (rank) {
            case UNCOMMON -> 1.25;
            case RARE -> 1.5;
            case EPIC -> 2.0;
            case LEGENDARY -> 3.0;
        };
    }
}
