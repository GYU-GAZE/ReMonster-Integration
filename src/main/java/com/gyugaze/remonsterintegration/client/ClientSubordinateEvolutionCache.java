package com.gyugaze.remonsterintegration.client;

import com.gyugaze.remonsterintegration.network.SubordinateEvolutionOptionsPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Short-lived client cache for server-authoritative subordinate evolution choices. */
public final class ClientSubordinateEvolutionCache {
    private static final Map<Integer, Snapshot> SNAPSHOTS = new ConcurrentHashMap<>();

    private ClientSubordinateEvolutionCache() {
    }

    public static void accept(SubordinateEvolutionOptionsPayload payload) {
        SNAPSHOTS.put(
                payload.entityId(),
                new Snapshot(
                        payload.currentSpecies(),
                        payload.baseMaxEp(),
                        payload.options(),
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
            ResourceLocation currentSpecies,
            double baseMaxEp,
            List<SubordinateEvolutionOptionsPayload.Option> options,
            long receivedAtMs
    ) {
        public Snapshot {
            options = List.copyOf(options);
        }
    }
}
