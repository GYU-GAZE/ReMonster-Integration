package com.gyugaze.remonsterintegration.command;

import com.gyugaze.remonsterintegration.compat.BetterSubordinatesBridge;
import com.gyugaze.remonsterintegration.evolution.EvolutionDefinition;
import com.gyugaze.remonsterintegration.evolution.EvolutionRegistry;
import com.gyugaze.remonsterintegration.evolution.SpeciesStateService;
import com.gyugaze.remonsterintegration.mca.McaSubordinateAdapter;
import com.gyugaze.remonsterintegration.tensura.TensuraExistenceBridge;
import com.gyugaze.remonsterintegration.tensura.TensuraSkillBridge;
import com.gyugaze.remonsterintegration.tensura.TensuraSubordinateBridge;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Pack-author diagnostics for the native subordinate progression stack. */
public final class SubordinateProgressionCommands {
    private SubordinateProgressionCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("remonster")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("progression")
                                .then(Commands.literal("status")
                                        .then(Commands.argument("target", EntityArgument.entity())
                                                .executes(context -> status(
                                                        context.getSource(),
                                                        EntityArgument.getEntity(context, "target")
                                                ))))
                                .then(Commands.literal("set_ep")
                                        .then(Commands.argument("target", EntityArgument.entity())
                                                .then(Commands.argument("ep", DoubleArgumentType.doubleArg(0.0D))
                                                        .executes(context -> setEp(
                                                                context.getSource(),
                                                                EntityArgument.getEntity(context, "target"),
                                                                DoubleArgumentType.getDouble(context, "ep")
                                                        )))))
                                .then(Commands.literal("add_ep")
                                        .then(Commands.argument("target", EntityArgument.entity())
                                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0D))
                                                        .executes(context -> addEp(
                                                                context.getSource(),
                                                                EntityArgument.getEntity(context, "target"),
                                                                DoubleArgumentType.getDouble(context, "amount")
                                                        )))))
                        )
        );
    }

    private static int status(CommandSourceStack source, Entity target) {
        if (!(target instanceof LivingEntity living)) {
            source.sendFailure(Component.literal("target is not a living entity"));
            return 0;
        }

        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType());
        Optional<ResourceLocation> species = SpeciesStateService.getSpecies(living);
        Optional<UUID> owner = TensuraSubordinateBridge.getResolvedOwnerUuid(living);
        double ep = TensuraExistenceBridge.getEp(living);
        double gainedEp = TensuraExistenceBridge.getGainedEp(living);

        source.sendSuccess(() -> Component.literal(
                "[Re:Monster] progression " + entityId + " / " + living.getUUID()
        ), false);
        source.sendSuccess(() -> Component.literal(
                "MCA=" + McaSubordinateAdapter.isMcaVillager(living)
                        + " | owner=" + owner.map(UUID::toString).orElse("none")
                        + " | species=" + species.map(ResourceLocation::toString).orElse("unset")
        ), false);
        source.sendSuccess(() -> Component.literal(
                "Tensura EP: current=" + ep + " | gained=" + gainedEp
        ), false);

        BetterSubordinatesBridge.ProgressionSnapshot progression =
                BetterSubordinatesBridge.getProgressionSnapshot(living);
        source.sendSuccess(() -> Component.literal(
                "Better Subs storage: " + progression.status().displayName()
                        + (progression.storageClass().isEmpty() ? "" : " | " + progression.storageClass())
        ), false);
        if (!progression.values().isEmpty()) {
            source.sendSuccess(() -> Component.literal(
                    "Better Subs counters: " + formatCounters(progression.values())
            ), false);
        }

        List<ManasSkillInstance> skills = TensuraSkillBridge.getRuntimeNonResistanceSkills(living);
        source.sendSuccess(() -> Component.literal("Tensura skills (" + skills.size() + "):"), false);
        if (skills.isEmpty()) {
            source.sendSuccess(() -> Component.literal("  none"), false);
        } else {
            for (ManasSkillInstance instance : skills) {
                source.sendSuccess(() -> Component.literal(
                        "  " + instance.getSkillId()
                                + " | mastery=" + instance.getMastery()
                                + " | mastered=" + instance.isMastered(living)
                ), false);
            }
        }

        if (species.isPresent()) {
            List<EvolutionDefinition> edges = EvolutionRegistry.availableFrom(species.get());
            source.sendSuccess(() -> Component.literal("evolution edges (" + edges.size() + "):"), false);
            if (edges.isEmpty()) {
                source.sendSuccess(() -> Component.literal("  none"), false);
            } else {
                for (EvolutionDefinition edge : edges) {
                    boolean ready = ep >= edge.minimumEp();
                    source.sendSuccess(() -> Component.literal(
                            "  -> " + edge.toSpecies()
                                    + " | minEP=" + edge.minimumEp()
                                    + " | " + (ready ? "READY" : "LOCKED")
                    ), false);
                }
            }
        }
        return 1;
    }

    private static String formatCounters(Map<String, String> values) {
        return values.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .reduce((left, right) -> left + " | " + right)
                .orElse("none");
    }

    private static int setEp(CommandSourceStack source, Entity target, double ep) {
        if (!(target instanceof LivingEntity living)) {
            source.sendFailure(Component.literal("target is not a living entity"));
            return 0;
        }

        double previous = TensuraExistenceBridge.getEp(living);
        TensuraExistenceBridge.setEp(living, ep);
        source.sendSuccess(() -> Component.literal(
                "[Re:Monster] debug EP " + living.getUUID() + ": " + previous + " -> " + ep
                        + " | Better Subordinates remains responsible for reacting to the new threshold"
        ), true);
        return 1;
    }

    private static int addEp(CommandSourceStack source, Entity target, double amount) {
        if (!(target instanceof LivingEntity living)) {
            source.sendFailure(Component.literal("target is not a living entity"));
            return 0;
        }

        double previous = TensuraExistenceBridge.getEp(living);
        double resulting = previous + amount;
        TensuraExistenceBridge.setEp(living, resulting);
        source.sendSuccess(() -> Component.literal(
                "[Re:Monster] debug EP " + living.getUUID() + ": " + previous + " -> " + resulting
                        + " | Better Subordinates remains responsible for reacting to the new threshold"
        ), true);
        return 1;
    }
}
