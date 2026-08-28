package com.gyugaze.remonsterintegration.tensura;

import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.impl.SkillStorage;
import io.github.manasmods.tensura.ability.SkillHelper;
import io.github.manasmods.tensura.ability.TensuraSkill;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.ability.skill.resist.ResistSkill;
import io.github.manasmods.tensura.registry.skill.CommonSkills;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.ability.IAbility;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Boundary for ManasCore/Tensura skill storage and Tensura's native acquisition-learning flow.
 */
public final class TensuraSkillBridge {
    private static final int EXTERNAL_AI_ABILITY_SLOT = 0;

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
     * Movement buttons are an alternate UI for Tensura's Telepathy command. Possession of the real
     * Telepathy skill is therefore the authorization source of truth. Tensura itself owns the agreed
     * 2,000 EP acquisition milestone; Re:Monster does not add a second live-EP check after the skill
     * has already been learned.
     */
    public static boolean hasTelepathyMovementAccess(LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        return getSkillInstance(entity, CommonSkills.TELEPATHY.getId()).isPresent();
    }

    /** Uses the exact gate used by ThoughtCommunicationSkill#nextMode in Tensura 2.0.1.2. */
    public static boolean hasMasteredThoughtCommunication(LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        return getSkillInstance(entity, CommonSkills.THOUGHT_COMMUNICATION.getId())
                .map(instance -> instance.isMastered(entity))
                .orElse(false);
    }

    /**
     * Skills this exact entity currently possesses, excluding Tensura's baseline resistance/
     * proficiency storage entries. This is the authoritative runtime list used by Re:Monster for
     * subordinate UI and kill-learning.
     */
    public static List<ManasSkillInstance> getRuntimeNonResistanceSkills(LivingEntity entity) {
        return getStorage(entity).getLearnedSkills().stream()
                .filter(instance -> !(instance.getSkill() instanceof ResistSkill))
                .sorted(Comparator.comparing(instance -> instance.getSkillId().toString()))
                .toList();
    }

    public static List<ResourceLocation> getRuntimeNonResistanceSkillIds(LivingEntity entity) {
        return getRuntimeNonResistanceSkills(entity).stream()
                .map(ManasSkillInstance::getSkillId)
                .toList();
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
     * Tensura rejects ACTIVATE_SKILL unless the requested skill/mode is present in the entity's active
     * ability preset. Players and native Tensura mobs normally populate those presets through their UI/
     * AI, but an arbitrary external mob has no such setup. Re:Monster owns slot 0 only for its fallback
     * external NPC caster and rewrites that slot immediately before pressing the chosen skill.
     */
    public static boolean prepareExternalNpcAbilitySlot(
            LivingEntity entity,
            ManasSkillInstance instance,
            int mode
    ) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(instance, "instance");

        IAbility ability = TensuraStorages.getAbilityFrom(entity);
        ability.setAbilitySlot(EXTERNAL_AI_ABILITY_SLOT, instance.getSkill(), mode);
        ability.markDirty();
        return ability.isAbilityInActivePreset(EXTERNAL_AI_ABILITY_SLOT, instance.getSkill(), mode);
    }

    public static int externalNpcAbilitySlot() {
        return EXTERNAL_AI_ABILITY_SLOT;
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
     * Creates Tensura's native negative-mastery acquisition instance when a kill exposes a skill the
     * learner has never seen before.
     *
     * Re:Monster deliberately auto-starts only COMMON and EXTRA skills here. RESISTANCE entries are
     * handled by Tensura's resistance systems, INTRINSIC skills belong to race/body identity, and
     * UNIQUE/ULTIMATE skills need explicit pack progression rules rather than being copied from any
     * random kill.
     *
     * Existing skill instances are never replaced.
     */
    public static boolean ensureKillLearningInstance(LivingEntity learner, ResourceLocation skillId) {
        Objects.requireNonNull(learner, "learner");
        Objects.requireNonNull(skillId, "skillId");

        Optional<ManasSkillInstance> existing = getSkillInstance(learner, skillId);
        if (existing.isPresent()) {
            return existing.get().getMastery() < 0.0;
        }

        ManasSkill registered = SkillAPI.getSkillRegistry().get(skillId);
        if (!(registered instanceof Skill tensuraSkill)) {
            return false;
        }

        Skill.SkillType type = tensuraSkill.getType();
        if (type != Skill.SkillType.COMMON && type != Skill.SkillType.EXTRA) {
            return false;
        }

        return SkillHelper.learnSkill(learner, tensuraSkill.createLearningInstance(learner));
    }

    /**
     * Kill-learning is an earned progression source, not an attempted manual cast. Tensura's normal
     * addLearnPoint method charges acquisition energy on every call; arbitrary external mobs can then
     * create many acquisition entries but permanently stall them after their small current pool is
     * depleted. We keep the real negative-mastery representation and SkillHelper unlock transition,
     * but advance the earned kill points directly instead of charging a cast/learning energy cost for
     * every kill.
     */
    public static boolean addOrStartKillLearning(
            LivingEntity learner,
            ResourceLocation skillId,
            double points
    ) {
        Objects.requireNonNull(learner, "learner");
        Objects.requireNonNull(skillId, "skillId");

        if (points <= 0.0D) {
            return false;
        }
        if (getSkillInstance(learner, skillId).isEmpty() && !ensureKillLearningInstance(learner, skillId)) {
            return false;
        }

        Optional<ManasSkillInstance> optional = getSkillInstance(learner, skillId);
        if (optional.isEmpty()) {
            return false;
        }
        ManasSkillInstance instance = optional.get();
        double oldMastery = instance.getMastery();
        if (oldMastery >= 0.0D) {
            return false;
        }

        double nextMastery = oldMastery + points;
        SkillStorage storage = getStorage(learner);
        if (nextMastery < 0.0D) {
            instance.setMastery(nextMastery);
            instance.markDirty();
            storage.markDirty();
            return true;
        }

        // Let Tensura/ManasCore perform the actual acquisition -> learned replacement so unlock hooks,
        // onLearnSkill behavior and storage synchronization remain authoritative.
        ManasSkillInstance completed = instance.copy();
        completed.setMastery(0.0D);
        boolean learned = SkillHelper.learnSkill(learner, completed);
        if (learned) {
            storage.markDirty();
        }
        return learned;
    }

    /**
     * Legacy native-cost path retained for callers that explicitly want Tensura's acquisition energy
     * charge rather than kill-earned progression.
     */
    public static boolean addOrStartAcquisitionLearning(
            LivingEntity learner,
            ResourceLocation skillId,
            double points
    ) {
        Objects.requireNonNull(learner, "learner");
        Objects.requireNonNull(skillId, "skillId");

        if (points <= 0.0) {
            return false;
        }

        if (getSkillInstance(learner, skillId).isEmpty() && !ensureKillLearningInstance(learner, skillId)) {
            return false;
        }

        return addAcquisitionLearning(learner, skillId, points);
    }

    /**
     * Adds points to an acquisition instance through Tensura's native cost-bearing learning primitive.
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

        return tensuraSkill.addLearnPoint(instance, learner, 0, points);
    }
}
