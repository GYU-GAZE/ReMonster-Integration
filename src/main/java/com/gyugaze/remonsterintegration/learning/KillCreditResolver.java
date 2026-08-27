package com.gyugaze.remonsterintegration.learning;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

/** Resolves the exact living combatant that should gain individual kill-learning progress. */
public final class KillCreditResolver {
    private KillCreditResolver() {
    }

    /**
     * DamageSource#getEntity already resolves the owning shooter for ordinary projectiles. Keeping
     * the exact living attacker matters for universal character growth: a wild mob or subordinate
     * that survives a fight should develop its own individual skill history instead of silently
     * donating every learning event to a player owner.
     */
    public static Optional<LivingEntity> resolveLearner(DamageSource source) {
        Entity attacker = source.getEntity();
        return attacker instanceof LivingEntity living && living.isAlive()
                ? Optional.of(living)
                : Optional.empty();
    }
}
