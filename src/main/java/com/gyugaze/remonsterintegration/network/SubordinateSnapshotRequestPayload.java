package com.gyugaze.remonsterintegration.network;

import com.gyugaze.remonsterintegration.ReMonsterIntegration;
import com.gyugaze.remonsterintegration.subordinate.SubordinateMovementService;
import com.gyugaze.remonsterintegration.subordinate.SubordinateTargetingService;
import com.gyugaze.remonsterintegration.tensura.TensuraExistenceBridge;
import com.gyugaze.remonsterintegration.tensura.TensuraSkillBridge;
import com.gyugaze.remonsterintegration.tensura.TensuraSubordinateBridge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Requests authoritative subordinate status for client chrome attached to a native screen. */
public record SubordinateSnapshotRequestPayload(int entityId) implements CustomPacketPayload {
    public static final Type<SubordinateSnapshotRequestPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ReMonsterIntegration.MOD_ID, "subordinate_snapshot_request")
    );

    public static final StreamCodec<FriendlyByteBuf, SubordinateSnapshotRequestPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeVarInt(payload.entityId()),
            buf -> new SubordinateSnapshotRequestPayload(buf.readVarInt())
    );

    private static final double MAX_DISTANCE_SQR = 32.0D * 32.0D;

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SubordinateSnapshotRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            Entity entity = player.serverLevel().getEntity(payload.entityId());
            if (!(entity instanceof LivingEntity target) || !target.isAlive()) {
                return;
            }
            if (player.distanceToSqr(target) > MAX_DISTANCE_SQR) {
                return;
            }
            if (!TensuraSubordinateBridge.isSubordinateOf(player, target)) {
                return;
            }

            int movement = SubordinateMovementService.getMode(target)
                    .map(Enum::ordinal)
                    .orElse(-1);
            int targeting = SubordinateTargetingService.getMode(target).ordinal();
            boolean targetingUnlocked = TensuraSkillBridge.hasMasteredThoughtCommunication(player);
            PacketDistributor.sendToPlayer(
                    player,
                    new SubordinateSnapshotPayload(
                            target.getId(),
                            movement,
                            targeting,
                            targetingUnlocked,
                            target.getHealth(),
                            target.getMaxHealth(),
                            TensuraExistenceBridge.getEp(target),
                            TensuraExistenceBridge.getSpiritualHealth(target)
                    )
            );
        });
    }
}
