package com.gyugaze.remonsterintegration.learning;

import com.gyugaze.remonsterintegration.profile.MobProfile;
import com.gyugaze.remonsterintegration.tensura.TensuraSkillBridge;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Kill-driven Tensura acquisition learning for any living combatant. */
public final class KillLearningService {
    private KillLearningService() {
    }

    public static List<ResourceLocation> grantFromDefeatedEntity(
            LivingEntity learner,
            LivingEntity defeatedEntity,
            Optional<MobProfile> defeatedMob,
            double baseLearningPoints
    ) {
        Objects.requireNonNull(learner, "learner");
        Objects.requireNonNull(defeatedEntity, "defeatedEntity");
        Objects.requireNonNull(defeatedMob, "defeatedMob");

        double profileMultiplier = defeatedMob.map(MobProfile::learningMultiplier).orElse(1.0);
        if (baseLearningPoints <= 0.0 || profileMultiplier <= 0.0) {
            return List.of();
        }

        double points = baseLearningPoints * profileMultiplier;
        Set<ResourceLocation> effectiveSkills = new LinkedHashSet<>();
        defeatedMob.map(MobProfile::skills).ifPresent(effectiveSkills::addAll);
        effectiveSkills.addAll(TensuraSkillBridge.getRuntimeNonResistanceSkillIds(defeatedEntity));

        List<ResourceLocation> progressed = new ArrayList<>();
        for (ResourceLocation skillId : effectiveSkills) {
            if (TensuraSkillBridge.addOrStartAcquisitionLearning(learner, skillId, points)) {
                progressed.add(skillId);
            }
        }

        return List.copyOf(progressed);
    }

    public static List<ResourceLocation> grantFromDefeatedEntity(
            LivingEntity learner,
            LivingEntity defeatedEntity,
            MobProfile defeatedMob,
            double baseLearningPoints
    ) {
        return grantFromDefeatedEntity(learner, defeatedEntity, Optional.of(defeatedMob), baseLearningPoints);
    }

    public static List<ResourceLocation> grantFromProfile(
            LivingEntity learner,
            MobProfile defeatedMob,
            double baseLearningPoints
    ) {
        Objects.requireNonNull(learner, "learner");
        Objects.requireNonNull(defeatedMob, "defeatedMob");

        if (baseLearningPoints <= 0.0 || defeatedMob.learningMultiplier() <= 0.0) {
            return List.of();
        }

        double points = baseLearningPoints * defeatedMob.learningMultiplier();
        List<ResourceLocation> progressed = new ArrayList<>();
        for (ResourceLocation skillId : defeatedMob.skills()) {
            if (TensuraSkillBridge.addOrStartAcquisitionLearning(learner, skillId, points)) {
                progressed.add(skillId);
            }
        }

        return List.copyOf(progressed);
    }
}
