package com.gyugaze.remonsterintegration.network;

import com.gyugaze.remonsterintegration.ReMonsterIntegration;
import com.gyugaze.remonsterintegration.client.ClientSubordinateSnapshotCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Authoritative server snapshot used only for the lightweight companion chrome. */
public record SubordinateSnapshotPayload(
        int entityId,
        int movementMode,
        int targetingMode,
        boolean targetingUnlocked,
        float health,
        float maxHealth,
        double ep,
        double spiritualHealth
) implements CustomPacketPayload {
    public static final Type<SubordinateSnapshotPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ReMonsterIntegration.MOD_ID, "subordinate_snapshot")
    );

    public static final StreamCodec<FriendlyByteBuf, SubordinateSnapshotPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.entityId());
                buf.writeVarInt(payload.movementMode());
                buf.writeVarInt(payload.targetingMode());
                buf.writeBoolean(payload.targetingUnlocked());
                buf.writeFloat(payload.health());
                buf.writeFloat(payload.maxHealth());
                buf.writeDouble(payload.ep());
                buf.writeDouble(payload.spiritualHealth());
            },
            buf -> new SubordinateSnapshotPayload(
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readBoolean(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readDouble(),
                    buf.readDouble()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SubordinateSnapshotPayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return;
        }
        context.enqueueWork(() -> ClientSubordinateSnapshotCache.accept(payload));
    }
}
