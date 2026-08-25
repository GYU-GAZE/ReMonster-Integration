package com.gyugaze.remonsterintegration.learning;

import com.gyugaze.remonsterintegration.ReMonsterIntegration;
import com.gyugaze.remonsterintegration.profile.MobProfileRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.List;

/**
 * NeoForge event bridge for profile-driven skill learning.
 *
 * Direct player kills are supported first. Subordinate/clone attribution is intentionally a
 * separate resolver so their ownership semantics can be added without contaminating this path.
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

        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }

        MobProfileRegistry.get(defeated).ifPresent(profile -> {
            List<ResourceLocation> progressed = KillLearningService.grantFromProfile(
                    player,
                    profile,
                    DEFAULT_BASE_LEARNING_POINTS
            );

            if (!progressed.isEmpty()) {
                ReMonsterIntegration.LOGGER.debug(
                        "Player {} gained acquisition-learning progress from {} for {}",
                        player.getGameProfile().getName(),
                        profile.entityId(),
                        progressed
                );
            }
        });
    }
}
