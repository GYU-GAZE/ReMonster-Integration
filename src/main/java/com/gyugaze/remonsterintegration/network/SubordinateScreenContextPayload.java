package com.gyugaze.remonsterintegration.network;

import com.gyugaze.remonsterintegration.ReMonsterIntegration;
import com.gyugaze.remonsterintegration.client.CompanionScreenContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server-authoritative hint that the next native screen belongs to an owned subordinate.
 *
 * This exists because some integrations (notably MCA) do not expose/sync Tensura ownership to the
 * client early enough for the local interaction hook to prove ownership before their GUI opens.
 */
public record SubordinateScreenContextPayload(int entityId) implements CustomPacketPayload {
    public static final Type<SubordinateScreenContextPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ReMonsterIntegration.MOD_ID, "subordinate_screen_context")
    );

    public static final StreamCodec<FriendlyByteBuf, SubordinateScreenContextPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeVarInt(payload.entityId()),
            buf -> new SubordinateScreenContextPayload(buf.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SubordinateScreenContextPayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return;
        }
        context.enqueueWork(() -> CompanionScreenContext.captureAuthorized(payload.entityId()));
    }
}
