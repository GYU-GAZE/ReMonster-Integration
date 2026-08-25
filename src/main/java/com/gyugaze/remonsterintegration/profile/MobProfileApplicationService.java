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
 * The default EP policy is deliberately conservative so official compatibility addons can remain
 * authoritative when they already supplied a positive EP value.
 */
public final class MobProfileApplicationService {
    private MobProfileApplicationService() {
    }

    public static MobProfileApplicationResult apply(LivingEntity entity, MobProfile profile) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(profile, "profile");

        double previousEp = TensuraExistenceBridge.getEp(entity);
        double targetEp = resolveEp(previousEp, profile);
        boolean epChanged = Double.compare(previousEp, targetEp) != 0;

        if (epChanged) {
            TensuraExistenceBridge.setEp(entity, targetEp);
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

    static double resolveEp(double currentEp, MobProfile profile) {
        if (profile.ep() <= 0.0) {
            return currentEp;
        }

        return switch (profile.epMode()) {
            case FALLBACK -> currentEp > 0.0 ? currentEp : profile.ep();
            case MINIMUM -> Math.max(currentEp, profile.ep());
            case REPLACE -> profile.ep();
        };
    }
}
