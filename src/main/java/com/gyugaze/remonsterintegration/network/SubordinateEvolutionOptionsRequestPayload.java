package com.gyugaze.remonsterintegration.network;

import com.gyugaze.remonsterintegration.ReMonsterIntegration;
import com.gyugaze.remonsterintegration.evolution.EvolutionDefinition;
import com.gyugaze.remonsterintegration.evolution.EvolutionRegistry;
import com.gyugaze.remonsterintegration.evolution.SpeciesStateService;
import com.gyugaze.remonsterintegration.tensura.TensuraExistenceBridge;
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

import java.util.List;
import java.util.Optional;

/** Requests server-authoritative evolution choices for one owned subordinate. */
public record SubordinateEvolutionOptionsRequestPayload(int entityId) implements CustomPacketPayload {
    public static final Type<SubordinateEvolutionOptionsRequestPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ReMonsterIntegration.MOD_ID, "subordinate_evolution_options_request")
    );

    public static final StreamCodec<FriendlyByteBuf, SubordinateEvolutionOptionsRequestPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeVarInt(payload.entityId()),
            buf -> new SubordinateEvolutionOptionsRequestPayload(buf.readVarInt())
    );

    private static final double MAX_DISTANCE_SQR = 32.0D * 32.0D;

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SubordinateEvolutionOptionsRequestPayload payload, IPayloadContext context) {
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

            sendSnapshot(player, target);
        });
    }

    public static void sendSnapshot(ServerPlayer player, LivingEntity target) {
        Optional<ResourceLocation> species = SpeciesStateService.getSpecies(target);
        double baseMaxEp = TensuraExistenceBridge.getBaseMaxEp(target);
        List<SubordinateEvolutionOptionsPayload.Option> options = species
                .map(EvolutionRegistry::availableFrom)
                .orElseGet(List::of)
                .stream()
                .map(definition -> toOption(definition, baseMaxEp))
                .toList();

        PacketDistributor.sendToPlayer(
                player,
                new SubordinateEvolutionOptionsPayload(
                        target.getId(),
                        species.orElse(null),
                        baseMaxEp,
                        options
                )
        );
    }

    private static SubordinateEvolutionOptionsPayload.Option toOption(
            EvolutionDefinition definition,
            double baseMaxEp
    ) {
        return new SubordinateEvolutionOptionsPayload.Option(
                definition.toSpecies(),
                definition.minimumEp(),
                baseMaxEp >= definition.minimumEp()
        );
    }
}
