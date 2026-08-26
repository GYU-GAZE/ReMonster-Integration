package com.gyugaze.remonsterintegration.network;

import com.gyugaze.remonsterintegration.ReMonsterIntegration;
import com.gyugaze.remonsterintegration.subordinate.SubordinateTargetingMode;
import com.gyugaze.remonsterintegration.subordinate.SubordinateTargetingService;
import com.gyugaze.remonsterintegration.tensura.TensuraSkillBridge;
import com.gyugaze.remonsterintegration.tensura.TensuraSubordinateBridge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server-authoritative Thought Communication targeting command. */
public record SubordinateTargetingCommandPayload(int entityId, int targetingMode) implements CustomPacketPayload {
    public static final Type<SubordinateTargetingCommandPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ReMonsterIntegration.MOD_ID, "subordinate_targeting_command")
    );

    public static final StreamCodec<FriendlyByteBuf, SubordinateTargetingCommandPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.entityId());
                buf.writeVarInt(payload.targetingMode());
            },
            buf -> new SubordinateTargetingCommandPayload(buf.readVarInt(), buf.readVarInt())
    );

    private static final double MAX_COMMAND_DISTANCE_SQR = 16.0D * 16.0D;

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SubordinateTargetingCommandPayload payload, IPayloadContext context) {
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
            if (!TensuraSkillBridge.hasMasteredThoughtCommunication(player)) {
                return;
            }

            SubordinateTargetingMode[] modes = SubordinateTargetingMode.values();
            if (payload.targetingMode() < 0 || payload.targetingMode() >= modes.length) {
                return;
            }

            SubordinateTargetingService.setMode(target, modes[payload.targetingMode()]);
        });
    }
}
