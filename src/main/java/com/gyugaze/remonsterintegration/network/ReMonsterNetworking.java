package com.gyugaze.remonsterintegration.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ReMonsterNetworking {
    private static final String PROTOCOL_VERSION = "1";

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
                SubordinateSnapshotRequestPayload.TYPE,
                SubordinateSnapshotRequestPayload.STREAM_CODEC,
                SubordinateSnapshotRequestPayload::handle
        );
        registrar.playToClient(
                SubordinateSnapshotPayload.TYPE,
                SubordinateSnapshotPayload.STREAM_CODEC,
                SubordinateSnapshotPayload::handle
        );
        registrar.playToClient(
                SubordinateScreenContextPayload.TYPE,
                SubordinateScreenContextPayload.STREAM_CODEC,
                SubordinateScreenContextPayload::handle
        );
    }
}
