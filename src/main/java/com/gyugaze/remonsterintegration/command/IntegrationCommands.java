package com.gyugaze.remonsterintegration.command;

import com.gyugaze.remonsterintegration.compat.MobChampionsBridge;
import com.gyugaze.remonsterintegration.mca.McaGuardTargetPolicy;
import com.gyugaze.remonsterintegration.mca.McaSubordinateAdapter;
import com.gyugaze.remonsterintegration.profile.MobProfile;
import com.gyugaze.remonsterintegration.profile.MobProfileApplicationResult;
import com.gyugaze.remonsterintegration.profile.MobProfileApplicationService;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
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
                        .then(Commands.literal("profile")
                                .then(Commands.literal("reapply")
                                        .then(Commands.argument("target", EntityArgument.entity())
                                                .executes(context -> reapplyProfile(
                                                        context.getSource(),
                                                        EntityArgument.getEntity(context, "target")
                                                )))))
                        .then(Commands.literal("guard_priority")
                                .then(Commands.argument("guard", EntityArgument.entity())
                                        .then(Commands.argument("target", EntityArgument.entity())
                                                .executes(context -> inspectGuardPriority(
                                                        context.getSource(),
                                                        EntityArgument.getEntity(context, "guard"),
                                                        EntityArgument.getEntity(context, "target")
                                                )))))
                        .then(Commands.literal("mca_subordinate")
                                .then(Commands.literal("assign")
                                        .then(Commands.argument("target", EntityArgument.entity())
                                                .then(Commands.argument("owner", EntityArgument.player())
                                                        .executes(context -> assignMcaSubordinate(
                                                                context.getSource(),
                                                                EntityArgument.getEntity(context, "target"),
                                                                EntityArgument.getPlayer(context, "owner")
                                                        )))))
                                .then(Commands.literal("clear")
                                        .then(Commands.argument("target", EntityArgument.entity())
                                                .executes(context -> clearMcaSubordinate(
                                                        context.getSource(),
                                                        EntityArgument.getEntity(context, "target")
                                                ))))
                                .then(Commands.literal("status")
                                        .then(Commands.argument("target", EntityArgument.entity())
                                                .executes(context -> subordinateStatus(
                                                        context.getSource(),
                                                        EntityArgument.getEntity(context, "target")
                                                )))))
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
                            + " | aggroRange=" + (p.overridesAggroRange() ? p.aggroRange() : "native")
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
        AttributeInstance followRange = living.getAttribute(Attributes.FOLLOW_RANGE);
        Optional<MobChampionsBridge.ChampionRank> championRank = MobChampionsBridge.getChampionRank(living);
        source.sendSuccess(() -> Component.literal("runtime EP: " + ep), false);
        source.sendSuccess(() -> Component.literal(
                "runtime FOLLOW_RANGE: " + (followRange == null ? "n/a" : followRange.getValue())
        ), false);
        source.sendSuccess(() -> Component.literal(
                "MobChampions: " + championRank.map(Enum::name).orElse("normal/not active")
        ), false);
        sendOwnershipStatus(source, living);

        List<String> skills = TensuraSkillBridge.getStorage(living).getLearnedSkills().stream()
                .map(ManasSkillInstance::getSkillId)
                .map(ResourceLocation::toString)
                .sorted(Comparator.naturalOrder())
                .toList();

        source.sendSuccess(() -> Component.literal("runtime skills (" + skills.size() + "): " + skills), false);
        return 1;
    }

    private static int inspectGuardPriority(CommandSourceStack source, Entity guardEntity, Entity targetEntity) {
        if (!(guardEntity instanceof LivingEntity guard) || !(targetEntity instanceof LivingEntity target)) {
            source.sendFailure(Component.literal("guard and target must both be living entities"));
            return 0;
        }

        OptionalInt override = McaGuardTargetPolicy.getPriorityOverride(target, guard);
        String result = override.isPresent()
                ? Integer.toString(override.getAsInt())
                : "delegate to MCA";
        source.sendSuccess(() -> Component.literal(
                "[Re:Monster] guard priority override: " + result
                        + " | activeThreat=" + McaGuardTargetPolicy.isActivelyThreateningProtectedEntity(target, guard)
                        + " | playerOwnedCompanion=" + McaGuardTargetPolicy.isPlayerOwnedCompanion(target)
        ), false);
        return 1;
    }

    private static int subordinateStatus(CommandSourceStack source, Entity target) {
        if (!(target instanceof LivingEntity living)) {
            source.sendFailure(Component.literal("target is not a living entity"));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(
                "[Re:Monster] subordinate status " + BuiltInRegistries.ENTITY_TYPE.getKey(living.getType())
                        + " / " + living.getUUID()
                        + " | MCA=" + McaSubordinateAdapter.isMcaVillager(living)
        ), false);
        sendOwnershipStatus(source, living);
        return 1;
    }

    private static void sendOwnershipStatus(CommandSourceStack source, LivingEntity living) {
        Optional<UUID> resolvedOwner = TensuraSubordinateBridge.getResolvedOwnerUuid(living);
        Optional<UUID> permanentOwner = TensuraSubordinateBridge.getPermanentOwnerUuid(living);
        Optional<UUID> temporaryOwner = TensuraSubordinateBridge.getTemporaryOwnerUuid(living);
        Optional<UUID> summoner = TensuraSubordinateBridge.getSummonerUuid(living);

        source.sendSuccess(() -> Component.literal(
                "Tensura ownership: resolved=" + uuidOrNone(resolvedOwner)
                        + " permanent=" + uuidOrNone(permanentOwner)
                        + " temporary=" + uuidOrNone(temporaryOwner)
                        + " summoner=" + uuidOrNone(summoner)
        ), false);

        String ownableOwner = "n/a";
        if (living instanceof OwnableEntity ownable) {
            LivingEntity vanillaOwner = ownable.getOwner();
            ownableOwner = vanillaOwner == null ? "none" : vanillaOwner.getUUID().toString();
        }
        String finalOwnableOwner = ownableOwner;
        source.sendSuccess(() -> Component.literal("OwnableEntity owner: " + finalOwnableOwner), false);
        source.sendSuccess(() -> Component.literal(
                "player-owned companion: " + McaGuardTargetPolicy.isPlayerOwnedCompanion(living)
        ), false);
    }

    private static String uuidOrNone(Optional<UUID> value) {
        return value.map(UUID::toString).orElse("none");
    }

    private static int reapplyProfile(CommandSourceStack source, Entity target) {
        if (!(target instanceof LivingEntity living)) {
            source.sendFailure(Component.literal("target is not a living entity"));
            return 0;
        }

        Optional<MobProfile> profile = MobProfileRegistry.get(living);
        if (profile.isEmpty()) {
            source.sendFailure(Component.literal("target has no loaded Re:Monster Mob Profile"));
            return 0;
        }

        MobProfileApplicationResult result = MobProfileApplicationService.apply(living, profile.get());
        source.sendSuccess(() -> Component.literal(
                "[Re:Monster] reapplied " + profile.get().entityId()
                        + " | EP " + result.previousEp() + " -> " + result.resultingEp()
                        + " | FOLLOW_RANGE " + result.previousAggroRange() + " -> " + result.resultingAggroRange()
                        + " | addedSkills=" + result.addedSkills()
        ), false);
        return 1;
    }

    private static int assignMcaSubordinate(CommandSourceStack source, Entity target, ServerPlayer owner) {
        if (!(target instanceof LivingEntity living)) {
            source.sendFailure(Component.literal("target is not a living entity"));
            return 0;
        }
        if (!McaSubordinateAdapter.isMcaVillager(living)) {
            source.sendFailure(Component.literal("target is not an MCA villager"));
            return 0;
        }
        if (!McaSubordinateAdapter.assignPermanentOwner(living, owner)) {
            source.sendFailure(Component.literal("Tensura did not resolve the requested permanent owner"));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(
                "[Re:Monster] MCA villager " + living.getUUID() + " is now a Tensura subordinate of " + owner.getGameProfile().getName()
        ), true);
        return 1;
    }

    private static int clearMcaSubordinate(CommandSourceStack source, Entity target) {
        if (!(target instanceof LivingEntity living)) {
            source.sendFailure(Component.literal("target is not a living entity"));
            return 0;
        }
        if (!McaSubordinateAdapter.isMcaVillager(living)) {
            source.sendFailure(Component.literal("target is not an MCA villager"));
            return 0;
        }
        if (!McaSubordinateAdapter.clearPermanentOwner(living)) {
            source.sendFailure(Component.literal("Tensura still resolves an owner after clearing permanent ownership"));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(
                "[Re:Monster] cleared Tensura permanent ownership from MCA villager " + living.getUUID()
        ), true);
        return 1;
    }
}
