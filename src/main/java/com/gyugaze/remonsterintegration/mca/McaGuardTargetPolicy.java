package com.gyugaze.remonsterintegration.mca;

import com.gyugaze.remonsterintegration.profile.MobProfile;
import com.gyugaze.remonsterintegration.profile.MobProfileRegistry;
import com.gyugaze.remonsterintegration.tensura.TensuraSubordinateBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.npc.Villager;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * Project-owned decision layer injected ahead of MCA's GuardEnemiesSensor priority logic.
 *
 * Returning an empty OptionalInt delegates to MCA unchanged. A present value overrides MCA's
 * priority for this target instance, allowing dynamic ownership/subordinate immunity that MCA's
 * type-only config cannot express.
 */
public final class McaGuardTargetPolicy {
    public static final int ACTIVE_THREAT_PRIORITY = 9;
    public static final int IGNORE_PRIORITY = -1;

    private McaGuardTargetPolicy() {
    }

    public static OptionalInt getPriorityOverride(LivingEntity target, LivingEntity guard) {
        if (target == null) {
            return OptionalInt.empty();
        }

        // Self-defence and village-defence always win over ownership immunity.
        if (isActivelyThreateningProtectedEntity(target, guard)) {
            return OptionalInt.of(ACTIVE_THREAT_PRIORITY);
        }

        // Friendly ownership is instance-specific and must be checked before profile/type hostility.
        // This covers Tensura subordinates plus normal/modded OwnableEntity companions such as
        // Wyrmroost dragons without introducing a hard dependency on those mods.
        if (isPlayerOwnedCompanion(target)) {
            return OptionalInt.of(IGNORE_PRIORITY);
        }

        Optional<MobProfile> profile = MobProfileRegistry.get(target);
        if (profile.isEmpty()) {
            return OptionalInt.empty();
        }

        MobProfile value = profile.get();
        if (!value.guardHostile()) {
            return OptionalInt.of(IGNORE_PRIORITY);
        }

        return OptionalInt.of(switch (value.tier()) {
            case NORMAL -> 3;
            case ELITE -> 4;
            case BOSS -> 5;
            case CALAMITY -> 6;
        });
    }

    /**
     * True when this concrete entity instance belongs to a player through either Tensura's
     * ExistenceStorage ownership or Minecraft's generic OwnableEntity contract.
     */
    public static boolean isPlayerOwnedCompanion(LivingEntity target) {
        if (TensuraSubordinateBridge.getResolvedOwner(target)
                .filter(ServerPlayer.class::isInstance)
                .isPresent()) {
            return true;
        }

        if (target instanceof OwnableEntity ownable) {
            return ownable.getOwner() instanceof ServerPlayer;
        }

        return false;
    }

    /**
     * Kept as a compatibility-friendly semantic alias for callers interested specifically in the
     * Tensura subordinate use case.
     */
    public static boolean isPlayerOwnedSubordinate(LivingEntity target) {
        return TensuraSubordinateBridge.getResolvedOwner(target)
                .filter(ServerPlayer.class::isInstance)
                .isPresent();
    }

    public static boolean isActivelyThreateningProtectedEntity(LivingEntity target, LivingEntity guard) {
        if (!(target instanceof Mob mob)) {
            return false;
        }

        LivingEntity attackTarget = mob.getTarget();
        if (attackTarget == null) {
            return false;
        }

        if (guard != null && attackTarget == guard) {
            return true;
        }

        // MCA villagers extend vanilla Villager, so this protects guards and ordinary MCA villagers
        // without introducing a hard MCA compile dependency in the policy layer.
        return attackTarget instanceof Villager;
    }
}
