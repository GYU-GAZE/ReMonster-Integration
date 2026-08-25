package com.gyugaze.remonsterintegration.command;

import com.gyugaze.remonsterintegration.profile.MobProfile;
import com.gyugaze.remonsterintegration.profile.MobProfileRegistry;
import com.gyugaze.remonsterintegration.tensura.TensuraExistenceBridge;
import com.gyugaze.remonsterintegration.tensura.TensuraSkillBridge;
import com.gyugaze.remonsterintegration.tensura.TensuraSubordinateBridge;
import com.mojang.brigadier.CommandDispatcher;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Pack-author diagnostics. These commands are intentionally permission-gated and are not gameplay UI.
 */
public final class IntegrationCommands {
    private IntegrationCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("remonster")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("inspect")
                                .then(Commands.argument("target", EntityArgument.entity())
                                        .executes(context -> inspect(
                                                context.getSource(),
                                                EntityArgument.getEntity(context, "target")
                                        ))))
        );
    }

    private static int inspect(CommandSourceStack source, Entity target) {
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        source.sendSuccess(() -> Component.literal("[Re:Monster] " + entityId + " / " + target.getUUID()), false);

        Optional<MobProfile> profile = MobProfileRegistry.get(entityId);
        if (profile.isEmpty()) {
            source.sendSuccess(() -> Component.literal("profile: none"), false);
        } else {
            MobProfile p = profile.get();
            source.sendSuccess(() -> Component.literal(
                    "profile: " + p.tier() + " / " + p.disposition()
                            + " | EP=" + p.ep() + " (" + p.epMode() + ")"
                            + " | guardHostile=" + p.guardHostile()
            ), false);
            source.sendSuccess(() -> Component.literal(
                    "ecology: natural=" + p.ecology().naturalSpawn()
                            + " habitats=" + p.ecology().habitats()
                            + " rarity=" + p.ecology().rarity()
                            + " weight=" + p.ecology().weight()
                            + " group=" + p.ecology().groupMin() + "-" + p.ecology().groupMax()
            ), false);
        }

        if (!(target instanceof LivingEntity living)) {
            source.sendSuccess(() -> Component.literal("entity is not LivingEntity; Tensura state not applicable"), false);
            return 1;
        }

        double ep = TensuraExistenceBridge.getEp(living);
        Optional<UUID> owner = TensuraSubordinateBridge.getResolvedOwnerUuid(living);
        source.sendSuccess(() -> Component.literal("runtime EP: " + ep), false);
        source.sendSuccess(() -> Component.literal("resolved Tensura owner: " + owner.map(UUID::toString).orElse("none")), false);

        List<String> skills = TensuraSkillBridge.getStorage(living).getLearnedSkills().stream()
                .map(ManasSkillInstance::getSkillId)
                .map(ResourceLocation::toString)
                .sorted(Comparator.naturalOrder())
                .toList();

        source.sendSuccess(() -> Component.literal("runtime skills (" + skills.size() + "): " + skills), false);
        return 1;
    }
}
