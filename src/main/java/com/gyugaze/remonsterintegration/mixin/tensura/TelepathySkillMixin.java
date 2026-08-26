package com.gyugaze.remonsterintegration.mixin.tensura;

import com.gyugaze.remonsterintegration.subordinate.SubordinateMovementMode;
import com.gyugaze.remonsterintegration.subordinate.SubordinateMovementService;
import com.gyugaze.remonsterintegration.tensura.TensuraSubordinateBridge;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.skill.common.TelepathySkill;
import io.github.manasmods.tensura.entity.template.TensuraTamableEntity;
import io.github.manasmods.tensura.entity.template.subclass.ISubordinate;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Completes Telepathy/Thought Communication command cycling for ownership-only external entities.
 * Real TensuraTamableEntity instances always keep their own ISubordinate state so animation,
 * wandering, sitting pose, follow and teleport remain one synchronized state machine.
 */
@Mixin(value = TelepathySkill.class, remap = false)
public abstract class TelepathySkillMixin {
    @Redirect(
            method = "telepathy",
            at = @At(
                    value = "INVOKE",
                    target = "Lio/github/manasmods/tensura/entity/template/subclass/ISubordinate;cycleCommands(Lnet/minecraft/world/entity/Mob;Lnet/minecraft/world/entity/player/Player;)V"
            ),
            remap = false,
            require = 0
    )
    private static void remonster$cycleCommand(
            ISubordinate subordinate,
            Mob target,
            Player player
    ) {
        if (target instanceof TensuraTamableEntity) {
            subordinate.cycleCommands(target, player);
            return;
        }

        if (SubordinateMovementService.supportsExternal(target)) {
            SubordinateMovementService.cycleExternal(target, player)
                    .ifPresent(mode -> sendCommandMessage(player, target, mode));
            return;
        }

        subordinate.cycleCommands(target, player);
    }

    /**
     * Entities such as MCA villagers and AbstractHorse do not reach ISubordinate.cycleCommands at all.
     * Handle them after upstream Telepathy has performed its normal cast/mastery/sound work.
     */
    @Inject(method = "telepathy", at = @At("RETURN"), remap = false, require = 0)
    private static void remonster$cycleOwnershipOnlySubordinate(
            ManasSkillInstance instance,
            LivingEntity user,
            Mob target,
            CallbackInfo ci
    ) {
        if (!(user instanceof Player player)) {
            return;
        }
        if (target instanceof ISubordinate) {
            return;
        }
        if (!SubordinateMovementService.supportsExternal(target)) {
            return;
        }
        if (!TensuraSubordinateBridge.isSubordinateOf(user, target)) {
            return;
        }

        SubordinateMovementService.cycleExternal(target, player)
                .ifPresent(mode -> sendCommandMessage(player, target, mode));
    }

    private static void sendCommandMessage(Player player, Mob target, SubordinateMovementMode mode) {
        String key = switch (mode) {
            case FOLLOW -> "tensura.message.pet.follow";
            case WANDER -> "tensura.message.pet.wander";
            case STAY -> "tensura.message.pet.stay";
        };
        player.displayClientMessage(
                Component.translatable(key, target.getDisplayName()).withStyle(ChatFormatting.AQUA),
                true
        );
    }
}
