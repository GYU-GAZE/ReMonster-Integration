package com.gyugaze.remonsterintegration.profile;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record MobProfileApplicationResult(
        boolean epChanged,
        double previousEp,
        double resultingEp,
        List<ResourceLocation> addedSkills
) {
    public MobProfileApplicationResult {
        addedSkills = List.copyOf(addedSkills);
    }

    public boolean changedAnything() {
        return epChanged || !addedSkills.isEmpty();
    }
}
