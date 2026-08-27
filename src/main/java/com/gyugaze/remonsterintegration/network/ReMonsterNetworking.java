package com.gyugaze.remonsterintegration.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ReMonsterNetworking {
    private static final String PROTOCOL_VERSION = "2";

    private ReMonsterNetworking() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(
                SubordinateCommandPayload.TYPE,
                SubordinateCommandPayload.STREAM_CODEC,
                SubordinateCommandPayload::handle
        );
        registrar.playToServer(
                SubordinateTargetingCommandPayload.TYPE,
                SubordinateTargetingCommandPayload.STREAM_CODEC,
                SubordinateTargetingCommandPayload::handle
        );
        registrar.playToServer(
                SubordinateSnapshotRequestPayload.TYPE,
                SubordinateSnapshotRequestPayload.STREAM_CODEC,
                SubordinateSnapshotRequestPayload::handle
        );
        registrar.playToServer(
                SubordinateEvolutionOptionsRequestPayload.TYPE,
                SubordinateEvolutionOptionsRequestPayload.STREAM_CODEC,
                SubordinateEvolutionOptionsRequestPayload::handle
        );
        registrar.playToServer(
                SubordinateEvolutionCommandPayload.TYPE,
                SubordinateEvolutionCommandPayload.STREAM_CODEC,
                SubordinateEvolutionCommandPayload::handle
        );
        registrar.playToClient(
                SubordinateSnapshotPayload.TYPE,
                SubordinateSnapshotPayload.STREAM_CODEC,
                SubordinateSnapshotPayload::handle
        );
        registrar.playToClient(
                SubordinateEvolutionOptionsPayload.TYPE,
                SubordinateEvolutionOptionsPayload.STREAM_CODEC,
                SubordinateEvolutionOptionsPayload::handle
        );
        registrar.playToClient(
                SubordinateScreenContextPayload.TYPE,
                SubordinateScreenContextPayload.STREAM_CODEC,
                SubordinateScreenContextPayload::handle
        );
    }
}
