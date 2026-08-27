package com.gyugaze.remonsterintegration.evolution;

import com.gyugaze.remonsterintegration.tensura.TensuraExistenceBridge;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.ModList;

import java.util.List;
import java.util.Optional;

public final class EvolutionCommands {
    private EvolutionCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("remonster")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("species")
                                .then(Commands.literal("get")
                                        .then(Commands.argument("target", EntityArgument.entity())
                                                .executes(context -> getSpecies(
                                                        context.getSource(),
                                                        EntityArgument.getEntity(context, "target")
                                                ))))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("target", EntityArgument.entity())
                                                .then(Commands.argument("species", ResourceLocationArgument.id())
                                                        .executes(context -> setSpecies(
                                                                context.getSource(),
                                                                EntityArgument.getEntity(context, "target"),
                                                                ResourceLocationArgument.getId(context, "species")
                                                        )))))
                                .then(Commands.literal("clear")
                                        .then(Commands.argument("target", EntityArgument.entity())
                                                .executes(context -> clearSpecies(
                                                        context.getSource(),
                                                        EntityArgument.getEntity(context, "target")
                                                )))))
                        .then(Commands.literal("evolve")
                                .then(Commands.argument("target", EntityArgument.entity())
                                        .then(Commands.argument("species", ResourceLocationArgument.id())
                                                .executes(context -> evolve(
                                                        context.getSource(),
                                                        EntityArgument.getEntity(context, "target"),
                                                        ResourceLocationArgument.getId(context, "species")
                                                )))))
                        .then(Commands.literal("evolutions")
                                .then(Commands.argument("target", EntityArgument.entity())
                                        .executes(context -> listEvolutions(
                                                context.getSource(),
                                                EntityArgument.getEntity(context, "target")
                                        ))))
                        .then(Commands.literal("townstead_roots")
                                .executes(context -> listTownsteadRoots(context.getSource())))
        );
    }

    private static int getSpecies(CommandSourceStack source, Entity target) {
        LivingEntity living = requireLiving(source, target);
        if (living == null) {
            return 0;
        }

        String species = SpeciesStateService.getSpecies(living)
                .map(ResourceLocation::toString)
                .orElse("unset");
        source.sendSuccess(() -> Component.literal(
                "[Re:Monster] species " + living.getUUID() + " = " + species
                        + " | phenotypeSeed=" + SpeciesStateService.getPhenotypeSeed(living)
        ), false);
        return 1;
    }

    private static int setSpecies(CommandSourceStack source, Entity target, ResourceLocation species) {
        LivingEntity living = requireLiving(source, target);
        if (living == null) {
            return 0;
        }

        SpeciesStateService.setSpecies(living, species);
        source.sendSuccess(() -> Component.literal(
                "[Re:Monster] semantic species set: " + living.getUUID() + " -> " + species
        ), true);
        return 1;
    }

    private static int clearSpecies(CommandSourceStack source, Entity target) {
        LivingEntity living = requireLiving(source, target);
        if (living == null) {
            return 0;
        }
        SpeciesStateService.clearSpecies(living);
        source.sendSuccess(() -> Component.literal(
                "[Re:Monster] cleared semantic species from " + living.getUUID()
        ), true);
        return 1;
    }

    private static int evolve(CommandSourceStack source, Entity target, ResourceLocation species) {
        LivingEntity living = requireLiving(source, target);
        if (living == null) {
            return 0;
        }

        EvolutionResult result = EvolutionService.evolve(living, species);
        if (!result.success()) {
            source.sendFailure(Component.literal("[Re:Monster] evolution failed: " + result.failure()));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(
                "[Re:Monster] evolved " + living.getUUID()
                        + ": " + result.fromSpecies() + " -> " + result.toSpecies()
                        + " | body=" + result.bodyResult()
        ), true);
        return 1;
    }

    private static int listEvolutions(CommandSourceStack source, Entity target) {
        LivingEntity living = requireLiving(source, target);
        if (living == null) {
            return 0;
        }

        Optional<ResourceLocation> current = SpeciesStateService.getSpecies(living);
        if (current.isEmpty()) {
            source.sendFailure(Component.literal("[Re:Monster] target has no semantic species state"));
            return 0;
        }

        double progressionEp = TensuraExistenceBridge.getBaseMaxEp(living);
        List<EvolutionDefinition> definitions = EvolutionRegistry.availableFrom(current.get());
        source.sendSuccess(() -> Component.literal(
                "[Re:Monster] " + current.get()
                        + " | baseMaxEP=" + progressionEp
                        + " | evolution edges=" + definitions.size()
        ), false);

        for (EvolutionDefinition definition : definitions) {
            source.sendSuccess(() -> Component.literal(
                    " -> " + definition.toSpecies()
                            + " | minBaseMaxEP=" + definition.minimumEp()
                            + (progressionEp >= definition.minimumEp() ? " | READY" : " | LOCKED")
            ), false);
        }
        return 1;
    }

    private static int listTownsteadRoots(CommandSourceStack source) {
        if (!ModList.get().isLoaded("townstead")) {
            source.sendFailure(Component.literal("[Re:Monster] Townstead is not loaded"));
            return 0;
        }

        List<ResourceLocation> roots = TownsteadEvolutionCompat.loadedRoots();
        source.sendSuccess(() -> Component.literal(
                "[Re:Monster] loaded Townstead roots (" + roots.size() + "): " + roots
        ), false);
        return 1;
    }

    private static LivingEntity requireLiving(CommandSourceStack source, Entity entity) {
        if (entity instanceof LivingEntity living) {
            return living;
        }
        source.sendFailure(Component.literal("[Re:Monster] target must be a living entity"));
        return null;
    }
}
