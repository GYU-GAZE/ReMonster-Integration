package com.gyugaze.remonsterintegration.learning;

import com.gyugaze.remonsterintegration.profile.MobProfile;
import com.gyugaze.remonsterintegration.tensura.TensuraSkillBridge;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Pure service for applying Mob Profile skill-learning rewards.
 *
 * Event ownership (direct player kill, subordinate kill, clone kill, etc.) is intentionally kept
 * outside this class so kill attribution can be tested independently.
 */
public final class KillLearningService {
    private KillLearningService() {
    }

    public static List<ResourceLocation> grantFromProfile(
            ServerPlayer player,
            MobProfile defeatedMob,
            double baseLearningPoints
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(defeatedMob, "defeatedMob");

        if (baseLearningPoints <= 0.0 || defeatedMob.learningMultiplier() <= 0.0) {
            return List.of();
        }

        double points = baseLearningPoints * defeatedMob.learningMultiplier();
        List<ResourceLocation> progressed = new ArrayList<>();

        for (ResourceLocation skillId : defeatedMob.skills()) {
            if (TensuraSkillBridge.addAcquisitionLearning(player, skillId, points)) {
                progressed.add(skillId);
            }
        }

        return List.copyOf(progressed);
    }
}
