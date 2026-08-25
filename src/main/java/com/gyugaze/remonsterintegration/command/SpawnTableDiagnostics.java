package com.gyugaze.remonsterintegration.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.MobSpawnSettings;

import java.util.Locale;

/**
 * Reads the final runtime biome spawn table after datapacks and NeoForge biome modifiers have been
 * applied. This is intentionally different from inspecting source JSON: if an entity is present here
 * but still never appears naturally, the remaining suspects are spawn rules, mob caps, placement
 * checks or runtime cancellation such as InControl.
 */
public final class SpawnTableDiagnostics {
    private SpawnTableDiagnostics() {
    }

    public static int inspectCurrentBiome(CommandSourceStack source, String namespace) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        String normalizedNamespace = namespace.toLowerCase(Locale.ROOT);
        var biomeHolder = player.serverLevel().getBiome(player.blockPosition());
        String biomeId = biomeHolder.unwrapKey()
                .map(key -> key.location().toString())
                .orElse("<unregistered biome>");

        source.sendSuccess(() -> Component.literal(
                "[Re:Monster] runtime spawn table | biome=" + biomeId + " | namespace=" + normalizedNamespace
        ), false);

        int matches = 0;
        for (MobCategory category : MobCategory.values()) {
            for (MobSpawnSettings.SpawnerData spawn : biomeHolder.value().getMobSettings().getMobs(category).unwrap()) {
                ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(spawn.type);
                if (id == null || !id.getNamespace().equals(normalizedNamespace)) {
                    continue;
                }

                matches++;
                int weight = spawn.getWeight().asInt();
                source.sendSuccess(() -> Component.literal(
                        category.getName() + " | " + id
                                + " | weight=" + weight
                                + " | group=" + spawn.minCount + "-" + spawn.maxCount
                ), false);
            }
        }

        if (matches == 0) {
            source.sendSuccess(() -> Component.literal(
                    "NO entries from namespace '" + normalizedNamespace + "' exist in this biome's final spawn table"
            ), false);
        } else {
            int finalMatches = matches;
            source.sendSuccess(() -> Component.literal("total matching spawn entries: " + finalMatches), false);
        }

        return matches;
    }
}
