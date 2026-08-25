package com.gyugaze.remonsterintegration.profile;

import com.gyugaze.remonsterintegration.ecology.EcologyProfile;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Set;

public record MobProfile(
        ResourceLocation entityId,
        MobDisposition disposition,
        MobTier tier,
        double ep,
        EpApplicationMode epMode,
        List<ResourceLocation> skills,
        double learningMultiplier,
        boolean guardHostile,
        double aggroRange,
        int dungeonWeight,
        Set<String> themes,
        EcologyProfile ecology
) {
    public MobProfile {
        skills = List.copyOf(skills);
        themes = Set.copyOf(themes);

        if (ep < 0) {
            throw new IllegalArgumentException("EP cannot be negative");
        }
        if (learningMultiplier < 0) {
            throw new IllegalArgumentException("Learning multiplier cannot be negative");
        }
        if (!Double.isFinite(aggroRange) || aggroRange < 0) {
            throw new IllegalArgumentException("Aggro range must be a finite non-negative number");
        }
        if (dungeonWeight < 0) {
            throw new IllegalArgumentException("Dungeon weight cannot be negative");
        }
    }

    /**
     * A value of 0 preserves the entity's native FOLLOW_RANGE.
     * Positive values explicitly normalize its target-acquisition and ally-alert radius.
     */
    public boolean overridesAggroRange() {
        return aggroRange > 0.0;
    }
}
