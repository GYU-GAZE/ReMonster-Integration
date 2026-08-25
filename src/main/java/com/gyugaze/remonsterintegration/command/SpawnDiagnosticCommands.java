package com.gyugaze.remonsterintegration.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * Kept separate from the main diagnostic command file so spawn-table debugging can evolve without
 * bloating normal entity inspection.
 */
public final class SpawnDiagnosticCommands {
    private SpawnDiagnosticCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("remonster")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("spawn_table")
                                .executes(context -> SpawnTableDiagnostics.inspectCurrentBiome(
                                        context.getSource(),
                                        "tensura"
                                ))
                                .then(Commands.argument("namespace", StringArgumentType.word())
                                        .executes(context -> SpawnTableDiagnostics.inspectCurrentBiome(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "namespace")
                                        ))))
        );
    }
}
