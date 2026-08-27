package com.gyugaze.remonsterintegration.network;

import com.gyugaze.remonsterintegration.ReMonsterIntegration;
import com.gyugaze.remonsterintegration.evolution.EvolutionResult;
import com.gyugaze.remonsterintegration.evolution.EvolutionService;
import com.gyugaze.remonsterintegration.tensura.TensuraSubordinateBridge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client intent to evolve one owned subordinate into a selected graph branch. */
public record SubordinateEvolutionCommandPayload(
        int entityId,
        ResourceLocation targetSpecies
) implements CustomPacketPayload {
    public static final Type<SubordinateEvolutionCommandPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ReMonsterIntegration.MOD_ID, "subordinate_evolution_command")
    );

    public static final StreamCodec<FriendlyByteBuf, SubordinateEvolutionCommandPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.entityId());
                buf.writeResourceLocation(payload.targetSpecies());
            },
            buf -> new SubordinateEvolutionCommandPayload(
                    buf.readVarInt(),
                    buf.readResourceLocation()
            )
    );

    private static final double MAX_DISTANCE_SQR = 32.0D * 32.0D;

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SubordinateEvolutionCommandPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            Entity entity = player.serverLevel().getEntity(payload.entityId());
            if (!(entity instanceof LivingEntity target) || !target.isAlive()) {
                player.sendSystemMessage(Component.literal("[Re:Monster] Evolution failed: subordinate is unavailable"));
                return;
            }
            if (player.distanceToSqr(target) > MAX_DISTANCE_SQR) {
                player.sendSystemMessage(Component.literal("[Re:Monster] Evolution failed: subordinate is too far away"));
                return;
            }
            if (!TensuraSubordinateBridge.isSubordinateOf(player, target)) {
                player.sendSystemMessage(Component.literal("[Re:Monster] Evolution failed: target is not your subordinate"));
                return;
            }

            EvolutionResult result = EvolutionService.evolve(target, payload.targetSpecies());
            if (!result.success()) {
                player.sendSystemMessage(Component.literal("[Re:Monster] Evolution failed: " + result.failure()));
                SubordinateEvolutionOptionsRequestPayload.sendSnapshot(player, target);
                return;
            }

            player.sendSystemMessage(Component.literal(
                    "[Re:Monster] " + target.getDisplayName().getString()
                            + " evolved: " + result.fromSpecies() + " -> " + result.toSpecies()
            ));
            SubordinateEvolutionOptionsRequestPayload.sendSnapshot(player, target);
        });
    }
}
