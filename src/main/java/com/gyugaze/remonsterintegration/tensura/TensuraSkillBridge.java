package com.gyugaze.remonsterintegration.tensura;

import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.impl.SkillStorage;
import io.github.manasmods.tensura.ability.TensuraSkill;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.Objects;
import java.util.Optional;

/**
 * Boundary for ManasCore/Tensura skill storage and Tensura's native acquisition-learning flow.
 */
public final class TensuraSkillBridge {
    private TensuraSkillBridge() {
    }

    public static SkillStorage getStorage(LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        return SkillAPI.getSkillsFrom(entity);
    }

    public static Optional<ManasSkillInstance> getSkillInstance(LivingEntity entity, ResourceLocation skillId) {
        Objects.requireNonNull(skillId, "skillId");
        return getStorage(entity).getSkill(skillId);
    }

    /**
     * Adds a registered skill only when the entity does not already possess it.
     * No chat message is produced for Mob Profile assignment.
     *
     * @return true when a new skill was actually learned.
     */
    public static boolean ensureSkill(LivingEntity entity, ResourceLocation skillId) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(skillId, "skillId");

        SkillStorage storage = getStorage(entity);
        if (storage.getSkill(skillId).isPresent()) {
            return false;
        }

        ManasSkill skill = SkillAPI.getSkillRegistry().get(skillId);
        if (skill == null) {
            return false;
        }

        return storage.learnSkill(skill, null);
    }

    /**
     * Tensura uses negative mastery for a skill instance that is currently being acquired/learned.
     */
    public static boolean isAcquisitionLearning(LivingEntity entity, ResourceLocation skillId) {
        return getSkillInstance(entity, skillId)
                .map(instance -> instance.getMastery() < 0.0)
                .orElse(false);
    }

    /**
     * Adds points through Tensura's own acquisition-learning method so its events, penalties,
     * messages, sync and completion behavior remain authoritative.
     *
     * @return true when Tensura accepted the learning-point attempt.
     */
    public static boolean addAcquisitionLearning(
            LivingEntity learner,
            ResourceLocation skillId,
            double points
    ) {
        Objects.requireNonNull(learner, "learner");
        Objects.requireNonNull(skillId, "skillId");

        if (points <= 0.0) {
            return false;
        }

        Optional<ManasSkillInstance> optionalInstance = getSkillInstance(learner, skillId);
        if (optionalInstance.isEmpty()) {
            return false;
        }

        ManasSkillInstance instance = optionalInstance.get();
        if (instance.getMastery() >= 0.0) {
            return false;
        }

        if (!(instance.getSkill() instanceof TensuraSkill tensuraSkill)) {
            return false;
        }

        // Acquisition learning is tied to the skill itself. Mode 0 is the default acquisition context;
        // mode-specialized skills will be validated in runtime compatibility tests.
        return tensuraSkill.addLearnPoint(instance, learner, 0, points);
    }
}
