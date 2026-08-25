package com.gyugaze.remonsterintegration.tensura;

import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.ep.IExistence;
import io.github.manasmods.tensura.util.SubordinateHelper;
import net.minecraft.world.entity.LivingEntity;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Small boundary around Tensura's ownership/subordinate API.
 *
 * Keeping direct Tensura calls in one place makes MCA and other integrations easier to audit when
 * the target Tensura version changes.
 */
public final class TensuraSubordinateBridge {
    private TensuraSubordinateBridge() {
    }

    public static Optional<UUID> getResolvedOwnerUuid(LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        return Optional.ofNullable(SubordinateHelper.getSubordinateOwnerUUID(entity));
    }

    public static Optional<LivingEntity> getResolvedOwner(LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        return Optional.ofNullable(SubordinateHelper.getSubordinateOwner(entity));
    }

    public static Optional<UUID> getPermanentOwnerUuid(LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        return Optional.ofNullable(TensuraStorages.getExistenceFrom(entity).getPermanentOwner());
    }

    public static Optional<UUID> getTemporaryOwnerUuid(LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        return Optional.ofNullable(TensuraStorages.getExistenceFrom(entity).getTemporaryOwner());
    }

    public static Optional<UUID> getSummonerUuid(LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        return Optional.ofNullable(TensuraStorages.getExistenceFrom(entity).getSummoner());
    }

    public static boolean isSubordinateOf(LivingEntity owner, LivingEntity target) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(target, "target");
        return SubordinateHelper.isSubordinate(owner, target);
    }

    public static boolean isAlly(LivingEntity first, LivingEntity second) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        return SubordinateHelper.isAlly(first, second);
    }

    /**
     * Writes Tensura's persistent owner field without changing the entity's class or AI implementation.
     *
     * This is intentionally narrower than a full "convert to subordinate" operation. Tensura resolves
     * temporary owner and summoner before permanent owner, so callers that want to replace pre-existing
     * transient ownership must do that explicitly.
     */
    public static void setPermanentOwner(LivingEntity target, UUID ownerUuid) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(ownerUuid, "ownerUuid");

        IExistence existence = TensuraStorages.getExistenceFrom(target);
        existence.setPermanentOwner(ownerUuid);
        existence.markDirty();
    }

    public static void setPermanentOwner(LivingEntity target, LivingEntity owner) {
        Objects.requireNonNull(owner, "owner");
        setPermanentOwner(target, owner.getUUID());
    }

    public static void clearPermanentOwner(LivingEntity target) {
        Objects.requireNonNull(target, "target");

        IExistence existence = TensuraStorages.getExistenceFrom(target);
        existence.setPermanentOwner(null);
        existence.markDirty();
    }
}
