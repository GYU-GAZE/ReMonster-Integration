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
        if (dungeonWeight < 0) {
            throw new IllegalArgumentException("Dungeon weight cannot be negative");
        }
    }
}
