package com.gyugaze.remonsterintegration.profile;

import com.gyugaze.remonsterintegration.ReMonsterIntegration;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * Runtime bridge from entity creation/loading into profile-defined Tensura state.
 */
public final class MobProfileEntityEvents {
    private MobProfileEntityEvents() {
    }

    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity living)) {
            return;
        }

        MobProfileRegistry.get(living).ifPresent(profile -> {
            MobProfileApplicationResult result = MobProfileApplicationService.apply(living, profile);
            if (result.changedAnything()) {
                ReMonsterIntegration.LOGGER.debug(
                        "Applied Mob Profile {} to {}: EP {} -> {}, added skills {}",
                        profile.entityId(),
                        living.getUUID(),
                        result.previousEp(),
                        result.resultingEp(),
                        result.addedSkills()
                );
            }
        });
    }
}
