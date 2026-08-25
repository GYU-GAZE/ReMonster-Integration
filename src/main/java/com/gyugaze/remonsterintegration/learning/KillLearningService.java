package com.gyugaze.remonsterintegration.learning;

import com.gyugaze.remonsterintegration.profile.MobProfile;
import com.gyugaze.remonsterintegration.tensura.TensuraSkillBridge;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.skill.resist.ResistSkill;
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
 * Runtime skills are deliberately included alongside profile-declared skills. This allows Tensura's
 * own entity data and existing compatibility addons to remain authoritative for skill assignment
 * while Re:Monster can consume those skills for acquisition-learning rewards.
 *
 * A Mob Profile is optional. This matters for vanilla mobs such as fish and for modded mobs that an
 * existing Tensura compatibility addon already enriches before Re:Monster has a curated profile.
 * When no profile exists, the default learning multiplier is 1.0.
 *
 * Tensura also keeps resistance/proficiency entries in normal LivingEntity skill storage even when
 * the entity was not explicitly granted those resistances as its combat abilities. Those ResistSkill
 * entries must not be interpreted as kill-learning rewards simply because they appear in storage.
 * Explicit profile skills remain authoritative and may still opt into a resistance deliberately.
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

        TensuraSkillBridge.getStorage(defeatedEntity).getLearnedSkills().stream()
                .filter(instance -> !(instance.getSkill() instanceof ResistSkill))
                .map(ManasSkillInstance::getSkillId)
                .forEach(effectiveSkills::add);

        List<ResourceLocation> progressed = new ArrayList<>();
        for (ResourceLocation skillId : effectiveSkills) {
            if (TensuraSkillBridge.addOrStartAcquisitionLearning(player, skillId, points)) {
                progressed.add(skillId);
            }
        }

        return List.copyOf(progressed);
    }

    /**
     * Compatibility overload for callers that already require a Mob Profile.
     */
    public static List<ResourceLocation> grantFromDefeatedEntity(
            ServerPlayer player,
            LivingEntity defeatedEntity,
            MobProfile defeatedMob,
            double baseLearningPoints
    ) {
        return grantFromDefeatedEntity(player, defeatedEntity, Optional.of(defeatedMob), baseLearningPoints);
    }

    /**
     * Compatibility overload retained for callers/tests that only want profile-declared skills.
     */
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
