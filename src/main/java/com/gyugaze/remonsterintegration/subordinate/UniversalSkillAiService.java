package com.gyugaze.remonsterintegration.subordinate;

import com.gyugaze.remonsterintegration.ReMonsterIntegration;
import com.gyugaze.remonsterintegration.tensura.TensuraSkillBridge;
import com.gyugaze.remonsterintegration.tensura.TensuraSubordinateBridge;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.impl.SkillStorage;
import io.github.manasmods.manascore.skill.impl.TickingSkill;
import io.github.manasmods.tensura.ability.skill.resist.ResistSkill;
import io.github.manasmods.tensura.entity.template.subclass.ISubordinate;
import io.github.manasmods.tensura.registry.skill.CommonSkills;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Lightweight active-skill driver only for compatibility subordinates that lack native Tensura AI.
 *
 * Native ISubordinate entities remain entirely under Tensura/Better Subordinates combat automation.
 * ManasCore remains the authority for activation events, requirements, cooldowns, held-skill ticks,
 * release behavior, energy cost and mastery. Re:Monster only supplies the missing NPC decision for
 * MCA/external owned mobs: while they have a real combat target, face it and ask native SkillStorage
 * to activate one usable learned skill.
 *
 * Passive skills are not emulated here. SkillStorage already ticks learned passive behavior for every
 * LivingEntity, including MCA and external mobs. Likewise, communication skills are explicitly kept
 * out of combat automation because their input semantics are commands/UI rather than attacks.
 */
public final class UniversalSkillAiService {
    private static final String CURSOR_KEY = "remonster_skill_ai_cursor";
    private static final String NEXT_ATTEMPT_KEY = "remonster_skill_ai_next_attempt";
    private static final int ATTEMPT_INTERVAL_TICKS = 10;
    private static final int FAILED_ACTIVATION_BACKOFF_TICKS = 5;
    private static final double MAX_AI_RANGE = 24.0D;
    private static final Set<ResourceLocation> NEVER_AUTO_ACTIVATE = Set.of(
            CommonSkills.TELEPATHY.getId(),
            CommonSkills.THOUGHT_COMMUNICATION.getId()
    );

    private UniversalSkillAiService() {
    }

    /** Called from the common Mob tick mixin; exits immediately for mobs outside the compatibility gap. */
    public static void tick(Mob mob) {
        if (mob.level().isClientSide() || !mob.isAlive() || mob instanceof ISubordinate) {
            return;
        }
        if (TensuraSubordinateBridge.getResolvedOwner(mob).isEmpty()) {
            return;
        }
        if (SubordinateMovementService.getMode(mob).orElse(SubordinateMovementMode.WANDER)
                == SubordinateMovementMode.STAY) {
            return;
        }
        if (SubordinateTargetingService.getMode(mob) == SubordinateTargetingMode.PASSIVE) {
            return;
        }

        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }

        double followRange = Math.max(8.0D, mob.getAttributeValue(Attributes.FOLLOW_RANGE));
        double useRange = Math.min(MAX_AI_RANGE, followRange);
        if (mob.distanceToSqr(target) > useRange * useRange) {
            return;
        }

        CompoundTag data = mob.getPersistentData();
        long gameTime = mob.level().getGameTime();
        if (gameTime < data.getLong(NEXT_ATTEMPT_KEY)) {
            return;
        }
        data.putLong(NEXT_ATTEMPT_KEY, gameTime + ATTEMPT_INTERVAL_TICKS);

        SkillStorage storage = TensuraSkillBridge.getStorage(mob);
        List<ManasSkillInstance> candidates = storage.getLearnedSkills().stream()
                .filter(instance -> instance.getMastery() >= 0.0D)
                .filter(instance -> !(instance.getSkill() instanceof ResistSkill))
                .filter(instance -> !NEVER_AUTO_ACTIVATE.contains(instance.getSkillId()))
                .filter(instance -> instance.getModes() > 0)
                .sorted(Comparator.comparing(instance -> instance.getSkillId().toString()))
                .toList();
        if (candidates.isEmpty()) {
            return;
        }

        int cursor = Math.floorMod(data.getInt(CURSOR_KEY), candidates.size());
        for (int offset = 0; offset < candidates.size(); offset++) {
            int index = (cursor + offset) % candidates.size();
            ManasSkillInstance instance = candidates.get(index);
            if (TickingSkill.isTickingSkill(mob, instance.getSkill())) {
                continue;
            }
            if (!instance.canInteractSkill(mob) || !instance.canActivateSkill(mob, 0) || instance.onCoolDown(0)) {
                continue;
            }

            mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
            data.putInt(CURSOR_KEY, (index + 1) % candidates.size());
            try {
                if (!storage.startHoldSkill(instance, 0, 0)) {
                    data.putLong(NEXT_ATTEMPT_KEY, gameTime + FAILED_ACTIVATION_BACKOFF_TICKS);
                }
            } catch (RuntimeException | LinkageError exception) {
                // A third-party/Tensura skill may still assume player-only input despite passing the
                // public activation gates. One incompatible skill must never crash compatibility AI.
                data.putLong(NEXT_ATTEMPT_KEY, gameTime + ATTEMPT_INTERVAL_TICKS);
                ReMonsterIntegration.LOGGER.debug(
                        "Skill {} rejected universal NPC activation for {}",
                        instance.getSkillId(),
                        mob.getUUID(),
                        exception
                );
            }
            return;
        }

        data.putInt(CURSOR_KEY, (cursor + 1) % candidates.size());
    }
}
