package com.gyugaze.remonsterintegration.evolution;

import com.gyugaze.remonsterintegration.tensura.TensuraExistenceBridge;
import com.gyugaze.remonsterintegration.tensura.TensuraSkillBridge;
import com.gyugaze.remonsterintegration.tensura.TensuraSubordinateBridge;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;

public final class EvolutionRequirementService {
    private EvolutionRequirementService() {
    }

    public static Evaluation evaluate(LivingEntity entity, EvolutionDefinition definition) {
        double ep = TensuraExistenceBridge.getBaseMaxEp(entity);
        if (ep < definition.minimumEp()) {
            return Evaluation.failure("EP requirement not met: " + ep + " / " + definition.minimumEp());
        }

        EvolutionConditions conditions = definition.conditions();

        for (ResourceLocation skillId : conditions.requiredSkills()) {
            boolean learned = TensuraSkillBridge.getSkillInstance(entity, skillId)
                    .map(instance -> instance.getMastery() >= 0.0D)
                    .orElse(false);
            if (!learned) {
                return Evaluation.failure("Required learned skill missing: " + skillId);
            }
        }

        for (ResourceLocation skillId : conditions.forbiddenSkills()) {
            if (TensuraSkillBridge.getSkillInstance(entity, skillId).isPresent()) {
                return Evaluation.failure("Forbidden skill present: " + skillId);
            }
        }

        if (!conditions.anyBiomeTags().isEmpty()) {
            boolean matched = conditions.anyBiomeTags().stream().anyMatch(id ->
                    entity.level().getBiome(entity.blockPosition()).is(TagKey.create(Registries.BIOME, id))
            );
            if (!matched) {
                return Evaluation.failure("Current biome does not match any required biome tag");
            }
        }

        for (ResourceLocation id : conditions.forbiddenBiomeTags()) {
            TagKey<Biome> tag = TagKey.create(Registries.BIOME, id);
            if (entity.level().getBiome(entity.blockPosition()).is(tag)) {
                return Evaluation.failure("Current biome matches forbidden biome tag: " + id);
            }
        }

        ResourceLocation dimension = entity.level().dimension().location();
        if (!conditions.dimensions().isEmpty() && !conditions.dimensions().contains(dimension)) {
            return Evaluation.failure("Current dimension is not eligible");
        }
        if (conditions.forbiddenDimensions().contains(dimension)) {
            return Evaluation.failure("Current dimension is forbidden: " + dimension);
        }

        int y = entity.blockPosition().getY();
        if (conditions.minY() != null && y < conditions.minY()) {
            return Evaluation.failure("Y level is below minimum: " + y + " / " + conditions.minY());
        }
        if (conditions.maxY() != null && y > conditions.maxY()) {
            return Evaluation.failure("Y level is above maximum: " + y + " / " + conditions.maxY());
        }

        boolean owned = TensuraSubordinateBridge.getResolvedOwnerUuid(entity).isPresent()
                || entity instanceof OwnableEntity ownable && ownable.getOwnerUUID() != null;
        if (conditions.ownership() == EvolutionConditions.OwnershipRequirement.WILD && owned) {
            return Evaluation.failure("Evolution requires a wild entity");
        }
        if (conditions.ownership() == EvolutionConditions.OwnershipRequirement.OWNED && !owned) {
            return Evaluation.failure("Evolution requires an owned entity");
        }

        int blockLight = entity.level().getBrightness(LightLayer.BLOCK, entity.blockPosition());
        if (conditions.minBlockLight() != null && blockLight < conditions.minBlockLight()) {
            return Evaluation.failure("Block light is below minimum: " + blockLight + " / " + conditions.minBlockLight());
        }
        if (conditions.maxBlockLight() != null && blockLight > conditions.maxBlockLight()) {
            return Evaluation.failure("Block light is above maximum: " + blockLight + " / " + conditions.maxBlockLight());
        }

        return Evaluation.success();
    }

    public static boolean isSatisfied(LivingEntity entity, EvolutionDefinition definition) {
        return evaluate(entity, definition).eligible();
    }

    public record Evaluation(boolean eligible, String failureReason) {
        public static Evaluation success() {
            return new Evaluation(true, "");
        }

        public static Evaluation failure(String reason) {
            return new Evaluation(false, reason);
        }
    }
}
