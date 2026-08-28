package com.gyugaze.remonsterintegration.mixin.mca;

import com.gyugaze.remonsterintegration.ReMonsterIntegration;
import com.gyugaze.remonsterintegration.mca.McaSubordinateAdapter;
import com.gyugaze.remonsterintegration.tensura.TensuraSubordinateBridge;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Owned MCA villagers are driven by Re:Monster's generic subordinate combat loop, which uses the
 * vanilla main-hand interaction channel for item use. MCA civilians may project inventory equipment
 * into a left-handed/offhand dominant slot, leaving a visibly held bow that the generic driver cannot
 * actually draw. For owned MCA subordinates only, project the chosen inventory bow into MAINHAND.
 * Ordinary MCA civilians/guards keep MCA's native handedness path untouched.
 */
@Mixin(value = McaSubordinateAdapter.class, remap = false)
public abstract class McaOwnedBowProjectionMixin {
    @Inject(method = "equipBestBowFromInventory", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void remonster$projectOwnedBowToUseHand(
            LivingEntity villager,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!McaSubordinateAdapter.isMcaVillager(villager)
                || McaSubordinateAdapter.isGuard(villager)
                || TensuraSubordinateBridge.getResolvedOwner(villager).isEmpty()) {
            return;
        }

        try {
            Object inventoryValue = villager.getClass().getMethod("getInventory").invoke(villager);
            if (!(inventoryValue instanceof Container inventory)) {
                cir.setReturnValue(false);
                return;
            }

            ItemStack best = ItemStack.EMPTY;
            int bestMaxDamage = -1;
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack candidate = inventory.getItem(i);
                if (candidate.getItem() instanceof BowItem && candidate.getMaxDamage() > bestMaxDamage) {
                    best = candidate;
                    bestMaxDamage = candidate.getMaxDamage();
                }
            }

            if (best.isEmpty()) {
                cir.setReturnValue(false);
                return;
            }

            villager.setItemSlot(EquipmentSlot.MAINHAND, best);
            cir.setReturnValue(true);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            ReMonsterIntegration.LOGGER.debug(
                    "Could not project owned MCA subordinate bow for {}",
                    villager.getUUID(),
                    exception
            );
            cir.setReturnValue(false);
        }
    }
}
