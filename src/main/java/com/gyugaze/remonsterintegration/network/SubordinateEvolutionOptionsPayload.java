package com.gyugaze.remonsterintegration.network;

import com.gyugaze.remonsterintegration.ReMonsterIntegration;
import com.gyugaze.remonsterintegration.client.ClientSubordinateEvolutionCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/** Server-authoritative species/EP/evolution-edge snapshot for the evolution chooser. */
public record SubordinateEvolutionOptionsPayload(
        int entityId,
        ResourceLocation currentSpecies,
        double baseMaxEp,
        List<Option> options
) implements CustomPacketPayload {
    public static final Type<SubordinateEvolutionOptionsPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ReMonsterIntegration.MOD_ID, "subordinate_evolution_options")
    );

    public static final StreamCodec<FriendlyByteBuf, SubordinateEvolutionOptionsPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.entityId());
                buf.writeBoolean(payload.currentSpecies() != null);
                if (payload.currentSpecies() != null) {
                    buf.writeResourceLocation(payload.currentSpecies());
                }
                buf.writeDouble(payload.baseMaxEp());
                buf.writeVarInt(payload.options().size());
                for (Option option : payload.options()) {
                    buf.writeResourceLocation(option.targetSpecies());
                    buf.writeDouble(option.minimumEp());
                    buf.writeBoolean(option.ready());
                }
            },
            buf -> {
                int entityId = buf.readVarInt();
                ResourceLocation currentSpecies = buf.readBoolean() ? buf.readResourceLocation() : null;
                double baseMaxEp = buf.readDouble();
                int count = Math.max(0, Math.min(buf.readVarInt(), 256));
                List<Option> options = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    options.add(new Option(
                            buf.readResourceLocation(),
                            buf.readDouble(),
                            buf.readBoolean()
                    ));
                }
                return new SubordinateEvolutionOptionsPayload(entityId, currentSpecies, baseMaxEp, List.copyOf(options));
            }
    );

    public SubordinateEvolutionOptionsPayload {
        options = List.copyOf(options);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SubordinateEvolutionOptionsPayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return;
        }
        context.enqueueWork(() -> ClientSubordinateEvolutionCache.accept(payload));
    }

    public record Option(
            ResourceLocation targetSpecies,
            double minimumEp,
            boolean ready
    ) {
    }
}
