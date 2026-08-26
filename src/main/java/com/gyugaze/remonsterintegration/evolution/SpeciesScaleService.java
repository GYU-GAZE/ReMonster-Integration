package com.gyugaze.remonsterintegration.evolution;

import com.gyugaze.remonsterintegration.ReMonsterIntegration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Applies Re:Monster semantic-species size without overwriting other mods' scale state. */
public final class SpeciesScaleService {
    private static final ResourceLocation SPECIES_SCALE_ID = ResourceLocation.fromNamespaceAndPath(
            ReMonsterIntegration.MOD_ID,
            "species_scale"
    );
    private static final double EPSILON = 1.0E-6D;

    private SpeciesScaleService() {
    }

    /**
     * Applies a multiplicative scale owned by Re:Monster Integration.
     *
     * <p>A dedicated attribute modifier is used instead of changing the SCALE base value, so
     * Tensura, equipment, effects, and other mods remain free to contribute their own size
     * modifiers. Reapplying a species replaces only this mod's modifier.</p>
     */
    public static void apply(LivingEntity entity, double scale) {
        AttributeInstance scaleAttribute = entity.getAttribute(Attributes.SCALE);
        if (scaleAttribute == null) {
            return;
        }

        scaleAttribute.removeModifier(SPECIES_SCALE_ID);
        if (Math.abs(scale - 1.0D) > EPSILON) {
            scaleAttribute.addPermanentModifier(new AttributeModifier(
                    SPECIES_SCALE_ID,
                    scale - 1.0D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            ));
        }
        entity.refreshDimensions();
    }

    /** Reapplies the configured scale for the entity's persisted semantic species. */
    public static void applyCurrent(LivingEntity entity) {
        double scale = SpeciesStateService.getSpecies(entity)
                .flatMap(SpeciesBodyProfileRegistry::get)
                .map(SpeciesBodyProfile::scale)
                .orElse(1.0D);
        apply(entity, scale);
    }
}
