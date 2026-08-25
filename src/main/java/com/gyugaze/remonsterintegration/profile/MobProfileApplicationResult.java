package com.gyugaze.remonsterintegration.profile;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record MobProfileApplicationResult(
        boolean epChanged,
        double previousEp,
        double resultingEp,
        boolean aggroRangeChanged,
        double previousAggroRange,
        double resultingAggroRange,
        List<ResourceLocation> addedSkills
) {
    public MobProfileApplicationResult {
        addedSkills = List.copyOf(addedSkills);
    }

    public boolean changedAnything() {
        return epChanged || aggroRangeChanged || !addedSkills.isEmpty();
    }
}
