package com.gyugaze.remonsterintegration.learning;

import com.gyugaze.remonsterintegration.profile.MobProfile;
import com.gyugaze.remonsterintegration.tensura.TensuraSkillBridge;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Pure service for kill-driven Tensura skill learning.
 *
 * Re:Monster treats skills this exact defeated entity really possesses at runtime as the primary
 * learnable repertoire. This lets dynamic growth, existing compatibility addons and individual mob
 * progression feed the same isekai-style learning loop: if the mob learned a skill, killing it can
 * train/teach that skill under the normal acquisition rules.
 *
 * Mob Profile skills are unioned in as explicit project-owned additions. Baseline resistance/
 * proficiency storage entries are excluded by TensuraSkillBridge.
 */
public final class KillLearningService {
    private KillLearningService() {
    }

    public static List<ResourceLocation> grantFromDefeatedEntity(
            ServerPlayer player,
            LivingEntity defeatedEntity,
            Optional<MobProfile> defeatedMob,
            double baseLearningPoints
    ) {
        Objects.requireNonNull(player, "player");
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
            if (TensuraSkillBridge.addOrStartAcquisitionLearning(player, skillId, points)) {
                progressed.add(skillId);
            }
        }

        return List.copyOf(progressed);
    }

    public static List<ResourceLocation> grantFromDefeatedEntity(
            ServerPlayer player,
            LivingEntity defeatedEntity,
            MobProfile defeatedMob,
            double baseLearningPoints
    ) {
        return grantFromDefeatedEntity(player, defeatedEntity, Optional.of(defeatedMob), baseLearningPoints);
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
            if (TensuraSkillBridge.addOrStartAcquisitionLearning(player, skillId, points)) {
                progressed.add(skillId);
            }
        }

        return List.copyOf(progressed);
    }
}
