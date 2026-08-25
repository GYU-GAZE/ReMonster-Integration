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
import java.util.Set;

/**
 * Pure service for applying Mob Profile skill-learning rewards.
 *
 * Runtime skills are deliberately included alongside profile-declared skills. This allows existing
 * Tensura compatibility addons to remain authoritative for skill assignment while Re:Monster can
 * still consume those skills for acquisition-learning rewards.
 *
 * Tensura also keeps resistance/proficiency entries in normal LivingEntity skill storage even when
 * the entity was not explicitly granted those resistances as its combat abilities. Those ResistSkill
 * entries must not be interpreted as kill-learning rewards simply because they appear in storage.
 * Explicit profile skills remain authoritative and may still opt into a resistance deliberately.
 *
 * Event ownership (direct player kill, subordinate kill, clone kill, etc.) is intentionally kept
 * outside this class so kill attribution can be tested independently.
 */
public final class KillLearningService {
    private KillLearningService() {
    }

    public static List<ResourceLocation> grantFromDefeatedEntity(
            ServerPlayer player,
            LivingEntity defeatedEntity,
            MobProfile defeatedMob,
            double baseLearningPoints
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(defeatedEntity, "defeatedEntity");
        Objects.requireNonNull(defeatedMob, "defeatedMob");

        if (baseLearningPoints <= 0.0 || defeatedMob.learningMultiplier() <= 0.0) {
            return List.of();
        }

        double points = baseLearningPoints * defeatedMob.learningMultiplier();
        Set<ResourceLocation> effectiveSkills = new LinkedHashSet<>(defeatedMob.skills());
        TensuraSkillBridge.getStorage(defeatedEntity).getLearnedSkills().stream()
                .filter(instance -> !(instance.getSkill() instanceof ResistSkill))
                .map(ManasSkillInstance::getSkillId)
                .forEach(effectiveSkills::add);

        List<ResourceLocation> progressed = new ArrayList<>();
        for (ResourceLocation skillId : effectiveSkills) {
            if (TensuraSkillBridge.addAcquisitionLearning(player, skillId, points)) {
                progressed.add(skillId);
            }
        }

        return List.copyOf(progressed);
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
            if (TensuraSkillBridge.addAcquisitionLearning(player, skillId, points)) {
                progressed.add(skillId);
            }
        }

        return List.copyOf(progressed);
    }
}
