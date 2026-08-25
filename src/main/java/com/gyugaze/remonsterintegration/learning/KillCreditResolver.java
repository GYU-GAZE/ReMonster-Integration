package com.gyugaze.remonsterintegration.learning;

import com.gyugaze.remonsterintegration.tensura.TensuraSubordinateBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Resolves the player who should receive integration-side kill credit.
 *
 * DamageSource#getEntity already resolves many projectile owners. For non-player living attackers,
 * Tensura ownership is followed with a depth/cycle guard so subordinate chains can credit their
 * controlling player without hard-coding individual mob classes.
 */
public final class KillCreditResolver {
    private static final int MAX_OWNER_DEPTH = 8;

    private KillCreditResolver() {
    }

    public static Optional<ServerPlayer> resolve(DamageSource source) {
        Entity attacker = source.getEntity();
        if (attacker instanceof ServerPlayer player) {
            return Optional.of(player);
        }
        if (!(attacker instanceof LivingEntity livingAttacker)) {
            return Optional.empty();
        }

        Set<UUID> visited = new HashSet<>();
        LivingEntity current = livingAttacker;

        for (int depth = 0; depth < MAX_OWNER_DEPTH; depth++) {
            if (!visited.add(current.getUUID())) {
                return Optional.empty();
            }

            Optional<LivingEntity> owner = TensuraSubordinateBridge.getResolvedOwner(current);
            if (owner.isEmpty()) {
                return Optional.empty();
            }

            LivingEntity resolvedOwner = owner.get();
            if (resolvedOwner instanceof ServerPlayer player) {
                return Optional.of(player);
            }

            current = resolvedOwner;
        }

        return Optional.empty();
    }
}
