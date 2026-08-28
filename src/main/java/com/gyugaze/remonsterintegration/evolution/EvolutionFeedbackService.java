package com.gyugaze.remonsterintegration.evolution;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

/** Shared evolution presentation for players, MCA villagers and ordinary mobs. */
public final class EvolutionFeedbackService {
    private static final double MESSAGE_RADIUS = 64.0D;

    private EvolutionFeedbackService() {
    }

    public static void announce(
            LivingEntity evolvedEntity,
            Component actorName,
            ResourceLocation targetSpecies
    ) {
        if (!(evolvedEntity.level() instanceof ServerLevel level)) {
            return;
        }

        double centerY = evolvedEntity.getY() + Math.max(0.5D, evolvedEntity.getBbHeight() * 0.5D);
        double horizontalSpread = Math.max(0.45D, evolvedEntity.getBbWidth() * 0.75D);
        double verticalSpread = Math.max(0.65D, evolvedEntity.getBbHeight() * 0.55D);

        level.sendParticles(
                ParticleTypes.REVERSE_PORTAL,
                evolvedEntity.getX(), centerY, evolvedEntity.getZ(),
                70,
                horizontalSpread, verticalSpread, horizontalSpread,
                0.18D
        );
        level.sendParticles(
                ParticleTypes.TOTEM_OF_UNDYING,
                evolvedEntity.getX(), centerY, evolvedEntity.getZ(),
                55,
                horizontalSpread, verticalSpread, horizontalSpread,
                0.12D
        );
        level.sendParticles(
                ParticleTypes.END_ROD,
                evolvedEntity.getX(), centerY, evolvedEntity.getZ(),
                28,
                horizontalSpread * 0.65D, verticalSpread, horizontalSpread * 0.65D,
                0.08D
        );
        level.sendParticles(
                ParticleTypes.POOF,
                evolvedEntity.getX(), centerY, evolvedEntity.getZ(),
                24,
                horizontalSpread, verticalSpread * 0.7D, horizontalSpread,
                0.05D
        );

        level.playSound(
                null,
                evolvedEntity.blockPosition(),
                SoundEvents.TOTEM_USE,
                SoundSource.NEUTRAL,
                1.1F,
                1.0F
        );
        level.playSound(
                null,
                evolvedEntity.blockPosition(),
                SoundEvents.PLAYER_LEVELUP,
                SoundSource.NEUTRAL,
                0.85F,
                0.92F
        );

        MutableComponent message = Component.empty()
                .append(actorName.copy().withStyle(ChatFormatting.GOLD))
                .append(Component.literal(" evolved into ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(displaySpecies(targetSpecies)).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
                .append(Component.literal("!").withStyle(ChatFormatting.YELLOW));

        double radiusSqr = MESSAGE_RADIUS * MESSAGE_RADIUS;
        for (ServerPlayer viewer : level.players()) {
            if (viewer.distanceToSqr(evolvedEntity) <= radiusSqr) {
                viewer.sendSystemMessage(message);
            }
        }
    }

    static String displaySpecies(ResourceLocation species) {
        String path = species.getPath().replace('_', ' ').replace('-', ' ');
        StringBuilder result = new StringBuilder(path.length());
        boolean capitalize = true;
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (Character.isWhitespace(c)) {
                result.append(c);
                capitalize = true;
            } else if (capitalize) {
                result.append(Character.toUpperCase(c));
                capitalize = false;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
