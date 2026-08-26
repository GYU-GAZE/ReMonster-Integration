package com.gyugaze.remonsterintegration.client;

import com.gyugaze.remonsterintegration.network.SubordinateSnapshotPayload;
import com.gyugaze.remonsterintegration.subordinate.SubordinateMovementMode;
import com.gyugaze.remonsterintegration.subordinate.SubordinateTargetingMode;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Tiny client cache for server-authoritative values rendered around native screens. */
public final class ClientSubordinateSnapshotCache {
    private static final Map<Integer, Snapshot> SNAPSHOTS = new ConcurrentHashMap<>();

    private ClientSubordinateSnapshotCache() {
    }

    public static void accept(SubordinateSnapshotPayload payload) {
        SubordinateMovementMode movement = null;
        SubordinateMovementMode[] movementModes = SubordinateMovementMode.values();
        if (payload.movementMode() >= 0 && payload.movementMode() < movementModes.length) {
            movement = movementModes[payload.movementMode()];
        }

        SubordinateTargetingMode targeting = SubordinateTargetingMode.NEUTRAL;
        SubordinateTargetingMode[] targetingModes = SubordinateTargetingMode.values();
        if (payload.targetingMode() >= 0 && payload.targetingMode() < targetingModes.length) {
            targeting = targetingModes[payload.targetingMode()];
        }

        SNAPSHOTS.put(
                payload.entityId(),
                new Snapshot(
                        movement,
                        payload.movementUnlocked(),
                        targeting,
                        payload.targetingUnlocked(),
                        payload.health(),
                        payload.maxHealth(),
                        payload.ep(),
                        payload.spiritualHealth(),
                        System.currentTimeMillis()
                )
        );
    }

    public static Optional<Snapshot> get(int entityId) {
        return Optional.ofNullable(SNAPSHOTS.get(entityId));
    }

    public static void clear(int entityId) {
        SNAPSHOTS.remove(entityId);
    }

    public record Snapshot(
            SubordinateMovementMode movement,
            boolean movementUnlocked,
            SubordinateTargetingMode targeting,
            boolean targetingUnlocked,
            float health,
            float maxHealth,
            double ep,
            double spiritualHealth,
            long receivedAtMs
    ) {
    }
}
