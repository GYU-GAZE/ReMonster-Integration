package com.gyugaze.remonsterintegration.menu;

import com.gyugaze.remonsterintegration.subordinate.SubordinateMovementMode;
import com.gyugaze.remonsterintegration.subordinate.SubordinateMovementService;
import com.gyugaze.remonsterintegration.tensura.TensuraExistenceBridge;
import com.gyugaze.remonsterintegration.tensura.TensuraSkillBridge;
import com.gyugaze.remonsterintegration.tensura.TensuraSubordinateBridge;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Slot-less server-authoritative menu for the common Re:Monster subordinate overview.
 *
 * Equipment is intentionally adapter-specific. The common screen owns identity, HP/EP/SHP,
 * movement state and the exact non-resistance Tensura skills this subordinate currently possesses.
 */
public final class SubordinateOverviewMenu extends AbstractContainerMenu {
    public static final int BUTTON_FOLLOW = 0;
    public static final int BUTTON_WANDER = 1;
    public static final int BUTTON_STAY = 2;

    private static final int DATA_MOVEMENT = 0;
    private static final int DATA_EP = 1;
    private static final int DATA_SHP = 2;
    private static final int DATA_COUNT = 3;
    private static final int MAX_SYNCED_SKILLS = 128;
    private static final double MAX_USE_DISTANCE_SQR = 12.0D * 12.0D;

    private final Inventory playerInventory;
    private final int targetEntityId;
    private final ContainerData data;
    private final List<ResourceLocation> skillIds;

    /** Client factory used by IMenuTypeExtension. */
    public SubordinateOverviewMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(
                containerId,
                playerInventory,
                extraData.readVarInt(),
                new SimpleContainerData(DATA_COUNT),
                readSkillIds(extraData)
        );
    }

    /** Server constructor. */
    public SubordinateOverviewMenu(int containerId, Inventory playerInventory, LivingEntity target) {
        this(
                containerId,
                playerInventory,
                target.getId(),
                createServerData(target),
                TensuraSkillBridge.getRuntimeNonResistanceSkillIds(target)
        );
    }

    private SubordinateOverviewMenu(
            int containerId,
            Inventory playerInventory,
            int targetEntityId,
            ContainerData data,
            List<ResourceLocation> skillIds
    ) {
        super(ReMonsterMenus.SUBORDINATE_OVERVIEW.get(), containerId);
        this.playerInventory = playerInventory;
        this.targetEntityId = targetEntityId;
        this.data = data;
        this.skillIds = List.copyOf(skillIds);
        checkContainerDataCount(data, DATA_COUNT);
        addDataSlots(data);
    }

    public static void writeOpeningData(RegistryFriendlyByteBuf buffer, LivingEntity target) {
        buffer.writeVarInt(target.getId());
        List<ResourceLocation> skills = TensuraSkillBridge.getRuntimeNonResistanceSkillIds(target);
        int count = Math.min(skills.size(), MAX_SYNCED_SKILLS);
        buffer.writeVarInt(count);
        for (int i = 0; i < count; i++) {
            buffer.writeResourceLocation(skills.get(i));
        }
    }

    private static List<ResourceLocation> readSkillIds(RegistryFriendlyByteBuf buffer) {
        int count = Math.min(Math.max(buffer.readVarInt(), 0), MAX_SYNCED_SKILLS);
        List<ResourceLocation> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            result.add(buffer.readResourceLocation());
        }
        return List.copyOf(result);
    }

    private static ContainerData createServerData(LivingEntity target) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case DATA_MOVEMENT -> SubordinateMovementService.getMode(target)
                            .map(Enum::ordinal)
                            .orElse(-1);
                    case DATA_EP -> clampToInt(TensuraExistenceBridge.getEp(target));
                    case DATA_SHP -> clampToInt(TensuraExistenceBridge.getSpiritualHealth(target));
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
    }

    private static int clampToInt(double value) {
        if (!Double.isFinite(value)) {
            return 0;
        }
        if (value >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (value <= Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) Math.round(value);
    }

    public int getTargetEntityId() {
        return targetEntityId;
    }

    public Optional<LivingEntity> getTarget() {
        Entity entity = playerInventory.player.level().getEntity(targetEntityId);
        return entity instanceof LivingEntity living ? Optional.of(living) : Optional.empty();
    }

    public int getEp() {
        return data.get(DATA_EP);
    }

    public int getSpiritualHealth() {
        return data.get(DATA_SHP);
    }

    public List<ResourceLocation> getSkillIds() {
        return skillIds;
    }

    public Optional<SubordinateMovementMode> getMovementMode() {
        int value = data.get(DATA_MOVEMENT);
        SubordinateMovementMode[] values = SubordinateMovementMode.values();
        return value >= 0 && value < values.length ? Optional.of(values[value]) : Optional.empty();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        Optional<LivingEntity> optionalTarget = getTarget();
        if (optionalTarget.isEmpty() || !(optionalTarget.get() instanceof Mob mob)) {
            return false;
        }
        LivingEntity target = optionalTarget.get();
        if (!TensuraSubordinateBridge.isSubordinateOf(player, target)) {
            return false;
        }

        SubordinateMovementMode mode = switch (id) {
            case BUTTON_FOLLOW -> SubordinateMovementMode.FOLLOW;
            case BUTTON_WANDER -> SubordinateMovementMode.WANDER;
            case BUTTON_STAY -> SubordinateMovementMode.STAY;
            default -> null;
        };
        return mode != null && SubordinateMovementService.setMode(mob, mode);
    }

    @Override
    public boolean stillValid(Player player) {
        if (player.level().isClientSide()) {
            return true;
        }
        Optional<LivingEntity> optionalTarget = getTarget();
        if (optionalTarget.isEmpty()) {
            return false;
        }
        LivingEntity target = optionalTarget.get();
        return target.isAlive()
                && player.distanceToSqr(target) <= MAX_USE_DISTANCE_SQR
                && TensuraSubordinateBridge.isSubordinateOf(player, target);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
