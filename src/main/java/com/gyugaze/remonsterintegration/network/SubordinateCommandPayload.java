package com.gyugaze.remonsterintegration.network;

import com.gyugaze.remonsterintegration.ReMonsterIntegration;
import com.gyugaze.remonsterintegration.subordinate.SubordinateMovementMode;
import com.gyugaze.remonsterintegration.subordinate.SubordinateMovementService;
import com.gyugaze.remonsterintegration.tensura.TensuraSubordinateBridge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C2S movement command used by companion chrome attached to native entity screens.
 * The client only requests a mode; ownership, distance and entity validity are always checked again
 * on the server before any AI state is mutated.
 */
public record SubordinateCommandPayload(int entityId, int movementMode) implements CustomPacketPayload {
    public static final Type<SubordinateCommandPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ReMonsterIntegration.MOD_ID, "subordinate_command")
    );

    public static final StreamCodec<FriendlyByteBuf, SubordinateCommandPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.entityId());
                buf.writeVarInt(payload.movementMode());
            },
            buf -> new SubordinateCommandPayload(buf.readVarInt(), buf.readVarInt())
    );

    private static final double MAX_COMMAND_DISTANCE_SQR = 16.0D * 16.0D;

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SubordinateCommandPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            Entity entity = player.serverLevel().getEntity(payload.entityId());
            if (!(entity instanceof Mob target) || !target.isAlive()) {
                return;
            }
            if (player.distanceToSqr(target) > MAX_COMMAND_DISTANCE_SQR) {
                return;
            }
            if (!TensuraSubordinateBridge.isSubordinateOf(player, target)) {
                return;
            }

            SubordinateMovementMode[] modes = SubordinateMovementMode.values();
            if (payload.movementMode() < 0 || payload.movementMode() >= modes.length) {
                return;
            }

            SubordinateMovementService.setMode(target, modes[payload.movementMode()]);
        });
    }
}
