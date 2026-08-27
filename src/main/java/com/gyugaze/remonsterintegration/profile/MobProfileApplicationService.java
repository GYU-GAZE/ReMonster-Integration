package com.gyugaze.remonsterintegration.profile;

import com.gyugaze.remonsterintegration.tensura.TensuraExistenceBridge;
import com.gyugaze.remonsterintegration.tensura.TensuraSkillBridge;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Applies the gameplay-facing Tensura state declared by a Mob Profile.
 *
 * Mob Profile `ep` is progression/capacity EP, represented by Tensura's base max EP. It must not be
 * written into IExistence current EP: current EP is consumable aura + magicule and overfilling it can
 * trigger Tensura's excess-energy loss, Insanity and Magicule Poison mechanics.
 *
 * The default EP policy is deliberately conservative so official compatibility addons can remain
 * authoritative when they already supplied a positive base max EP value.
 */
public final class MobProfileApplicationService {
    private MobProfileApplicationService() {
    }

    public static MobProfileApplicationResult apply(LivingEntity entity, MobProfile profile) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(profile, "profile");

        double previousEp = TensuraExistenceBridge.getBaseMaxEp(entity);
        double targetEp = resolveEp(previousEp, profile);
        boolean epChanged = Double.compare(previousEp, targetEp) != 0;

        if (epChanged) {
            TensuraExistenceBridge.setBaseMaxEpSafely(entity, targetEp);
        }

        AttributeInstance followRange = entity.getAttribute(Attributes.FOLLOW_RANGE);
        double previousAggroRange = followRange == null ? 0.0 : followRange.getBaseValue();
        double targetAggroRange = previousAggroRange;
        boolean aggroRangeChanged = false;

        if (profile.overridesAggroRange() && followRange != null) {
            targetAggroRange = profile.aggroRange();
            aggroRangeChanged = Double.compare(previousAggroRange, targetAggroRange) != 0;
            if (aggroRangeChanged) {
                followRange.setBaseValue(targetAggroRange);
            }
        }

        List<ResourceLocation> addedSkills = new ArrayList<>();
        for (ResourceLocation skillId : profile.skills()) {
            if (TensuraSkillBridge.ensureSkill(entity, skillId)) {
                addedSkills.add(skillId);
            }
        }

        return new MobProfileApplicationResult(
                epChanged,
                previousEp,
                targetEp,
                aggroRangeChanged,
                previousAggroRange,
                targetAggroRange,
                addedSkills
        );
    }

    static double resolveEp(double baseMaxEp, MobProfile profile) {
        if (profile.ep() <= 0.0) {
            return baseMaxEp;
        }

        return switch (profile.epMode()) {
            case FALLBACK -> baseMaxEp > 0.0 ? baseMaxEp : profile.ep();
            case MINIMUM -> Math.max(baseMaxEp, profile.ep());
            case REPLACE -> profile.ep();
        };
    }
}
