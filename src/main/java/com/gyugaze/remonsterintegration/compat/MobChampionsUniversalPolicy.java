package com.gyugaze.remonsterintegration.compat;

import com.gyugaze.remonsterintegration.ReMonsterIntegration;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * Re:Monster policy layer for MobChampions eligibility.
 *
 * MobChampions itself is already generic over LivingEntity/Mob and only limits random champion
 * creation through its configured entity-id whitelist. Re:Monster broadens that gate to ordinary
 * Mob bodies from any namespace while retaining an explicit datapack opt-out tag for helpers,
 * scripted bosses or other entities that should never receive random champion ranks.
 */
public final class MobChampionsUniversalPolicy {
    public static final TagKey<EntityType<?>> EXCLUDED = TagKey.create(
            Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(ReMonsterIntegration.MOD_ID, "mobchampions_excluded")
    );

    private MobChampionsUniversalPolicy() {
    }

    public static boolean isEligible(LivingEntity entity) {
        return entity instanceof Mob && !entity.getType().is(EXCLUDED);
    }

    public static boolean allowConfiguredOrUniversal(
            java.util.List<?> configuredWhitelist,
            Object entityTypeId,
            LivingEntity entity
    ) {
        return configuredWhitelist.contains(entityTypeId) || isEligible(entity);
    }
}
