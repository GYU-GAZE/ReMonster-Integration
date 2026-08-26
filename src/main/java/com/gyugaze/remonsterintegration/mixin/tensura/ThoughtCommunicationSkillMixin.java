package com.gyugaze.remonsterintegration.mixin.tensura;

import com.gyugaze.remonsterintegration.subordinate.SubordinateTargetingMode;
import com.gyugaze.remonsterintegration.subordinate.SubordinateTargetingService;
import com.gyugaze.remonsterintegration.tensura.TensuraSkillBridge;
import com.gyugaze.remonsterintegration.tensura.TensuraSubordinateBridge;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.skill.common.ThoughtCommunicationSkill;
import io.github.manasmods.tensura.config.ability.skill.CommonSkillConfig;
import io.github.manasmods.tensura.entity.template.subclass.ISubordinate;
import io.github.manasmods.tensura.util.ObjectSelectionHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Extends native Thought Communication selection to ownership-only Re:Monster subordinates. */
@Mixin(value = ThoughtCommunicationSkill.class, remap = false)
public abstract class ThoughtCommunicationSkillMixin {
    @Shadow
    @Final
    private static CommonSkillConfig.ThoughtCommunication CONFIG;

    /**
     * Shift + movement mode uses MCA/Tensura's normal group-command loop. The native list is based on
     * Mob#isAlliedTo, which does not necessarily know about Re:Monster's canonical Tensura owner for
     * MCA and third-party entities, so union those server-authorized subordinates into the same list.
     */
    @Redirect(
            method = "movementBehaviour",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;"
            ),
            remap = false,
            require = 0
    )
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static List remonster$includeExternalMovementGroup(
            Level level,
            Class entityClass,
            AABB bounds,
            Predicate predicate,
            ManasSkillInstance instance,
            LivingEntity user
    ) {
        return includeOwnedSubordinates(level, entityClass, bounds, predicate, user);
    }

    /** Same union for Shift + targeting mode, preserving Tensura's own command cycle/messages/sound. */
    @Redirect(
            method = "targetingBehaviour",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;"
            ),
            remap = false,
            require = 0
    )
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static List remonster$includeExternalTargetingGroup(
            Level level,
            Class entityClass,
            AABB bounds,
            Predicate predicate,
            ManasSkillInstance instance,
            LivingEntity user
    ) {
        return includeOwnedSubordinates(level, entityClass, bounds, predicate, user);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static List includeOwnedSubordinates(
            Level level,
            Class entityClass,
            AABB bounds,
            Predicate predicate,
            LivingEntity user
    ) {
        List result = new ArrayList(level.getEntitiesOfClass(entityClass, bounds, predicate));
        List<Mob> owned = level.getEntitiesOfClass(
                Mob.class,
                bounds,
                candidate -> candidate != user && TensuraSubordinateBridge.isSubordinateOf(user, candidate)
        );
        for (Mob candidate : owned) {
            if (!result.contains(candidate)) {
                result.add(candidate);
            }
        }
        return result;
    }

    /**
     * Native targetingTelepathy deliberately calls cycleBehaviour only for ISubordinate, but still
     * performs the normal swing/cast sound for any valid Tensura subordinate. Fill only that missing
     * state mutation for ownership-only MCA/vanilla/modded Mob targets.
     */
    @Inject(method = "targetingTelepathy", at = @At("RETURN"), remap = false, require = 0)
    private static void remonster$cycleExternalTargeting(LivingEntity user, CallbackInfo ci) {
        if (user.level().isClientSide() || !TensuraSkillBridge.hasMasteredThoughtCommunication(user)) {
            return;
        }

        Mob target = ObjectSelectionHelper.getTargetingEntity(
                Mob.class,
                user,
                CONFIG.telepathyRadius,
                0.2D,
                false,
                false,
                false
        );
        if (target == null || target instanceof ISubordinate) {
            return;
        }
        if (!TensuraSubordinateBridge.isSubordinateOf(user, target)) {
            return;
        }

        SubordinateTargetingMode next = switch (SubordinateTargetingService.getMode(target)) {
            case NEUTRAL -> SubordinateTargetingMode.PASSIVE;
            case PASSIVE -> SubordinateTargetingMode.AGGRESSIVE;
            case AGGRESSIVE -> SubordinateTargetingMode.PROTECT;
            case PROTECT -> SubordinateTargetingMode.NEUTRAL;
        };
        SubordinateTargetingService.setMode(target, next);
    }
}
