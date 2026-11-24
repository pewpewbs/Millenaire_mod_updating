/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.init.Items
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.tileentity.TileEntity
 *  net.minecraft.tileentity.TileEntityFurnace
 */
package org.millenaire.common.goal.generic;

import java.util.List;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import org.millenaire.common.annotedparameters.AnnotedParameter;
import org.millenaire.common.annotedparameters.ConfigAnnotations;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.entity.TileEntityFirePit;
import org.millenaire.common.goal.Goal;
import org.millenaire.common.goal.generic.GoalGeneric;
import org.millenaire.common.item.InvItem;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.village.Building;

public class GoalGenericCooking
extends GoalGeneric {
    public static final String GOAL_TYPE = "cooking";
    @ConfigAnnotations.ConfigField(type=AnnotedParameter.ParameterType.INVITEM)
    @ConfigAnnotations.FieldDocumentation(explanation="The item to be cooked.")
    public InvItem itemToCook = null;
    @ConfigAnnotations.ConfigField(type=AnnotedParameter.ParameterType.INTEGER, defaultValue="16")
    @ConfigAnnotations.FieldDocumentation(explanation="Minimum number of items that can be added to a cooking.")
    public int minimumToCook;

    @Override
    public void applyDefaultSettings() {
        this.lookAtGoal = true;
    }

    private boolean usesWorkplaceBalance() {
        return !this.townHallGoal && this.itemToCook != null
                && this.balanceOutput != null && this.balanceOutput.length > 1
                && this.balanceOutput[0].matches(this.itemToCook);
    }

    @Override
    protected boolean passesResourceBalance(MillVillager villager) {
        // Matching input/output pairs are evaluated against each actual
        // workplace below; unrelated balance pairs retain the original policy.
        return usesWorkplaceBalance() || super.passesResourceBalance(villager);
    }

    private boolean passesWorkplaceBalance(MillVillager villager, Building destination) {
        if (!usesWorkplaceBalance()) return super.passesResourceBalance(villager);
        Building townHall = villager.getTownHall();
        if (townHall == null) return false;
        InvItem input = this.balanceOutput[0], output = this.balanceOutput[1];
        long raw = (long)townHall.nbGoodAvailable(input, false, false, false)
                + villager.countInv(input);
        long cooked = (long)townHall.nbGoodAvailable(output, false, false, false)
                + villager.countInv(output);
        if (destination != townHall) {
            raw += destination.countGoods(input);
            cooked += destination.countGoods(output);
        }
        return raw >= cooked;
    }

    @Override
    public Goal.GoalInformation getDestination(MillVillager villager) throws Exception {
        List<Building> buildings = this.getBuildings(villager);
        for (Building dest : buildings) {
            if (!this.isDestPossible(villager, dest)) continue;
            if (!passesWorkplaceBalance(villager, dest)) {
                org.millenaire.common.goal.GoalDiagnostics.note(villager, this, "WORKPLACE_RESOURCE_BALANCE");
                continue;
            }
            int countGoods = dest.countGoods(this.itemToCook) + villager.countInv(this.itemToCook);
            if (org.millenaire.common.goal.GoalDiagnostics.recording(villager, this)) {
                if (countGoods < this.minimumToCook)
                    org.millenaire.common.goal.GoalDiagnostics.note(villager, this, "INPUT_BELOW_MINIMUM " + this.itemToCook + " " + countGoods + "/" + this.minimumToCook);
                if (dest.getResManager().furnaces.isEmpty() && dest.getResManager().firepits.isEmpty())
                    org.millenaire.common.goal.GoalDiagnostics.note(villager, this, "NO_REGISTERED_FURNACE_OR_FIREPIT");
            }
            for (Point p : dest.getResManager().furnaces) {
                FurnaceBlockEntity furnace = p.getFurnace(villager.level);
                if (furnace == null) continue;
                if (countGoods >= this.minimumToCook && (furnace.getItem(0) == ItemStack.EMPTY || furnace.getItem(0).getItem() == Items.AIR || furnace.getItem(0).getItem() == this.itemToCook.getItem() && furnace.getItem(0).getDamageValue() == this.itemToCook.meta && furnace.getItem(0).getCount() < 32)) {
                    return this.packDest(p, dest);
                }
                // Only schedule output-only visits when the building can actually store the output.
                if (!GoalGenericTendFurnace.canStoreOutput(dest, furnace.getItem(2))) continue;
                return this.packDest(p, dest);
            }
            boolean firepitBurnable = TileEntityFirePit.isFirePitBurnable(villager.level, this.itemToCook.staticStack);
            if (!firepitBurnable) continue;
            for (Point p : dest.getResManager().firepits) {
                ItemStack stack;
                int slotNb;
                TileEntityFirePit firepit = p.getFirePit(villager.level);
                if (firepit == null) continue;
                for (slotNb = 0; slotNb < 3; ++slotNb) {
                    stack = firepit.inputs.getStackInSlot(slotNb);
                    if (countGoods < this.minimumToCook || !stack.isEmpty() && (stack.getItem() != this.itemToCook.getItem() || stack.getDamageValue() != this.itemToCook.meta || stack.getCount() >= 32)) continue;
                    return this.packDest(p, dest);
                }
                for (slotNb = 0; slotNb < 3; ++slotNb) {
                    stack = firepit.outputs.getStackInSlot(slotNb);
                    if (!GoalGenericTendFurnace.canStoreOutput(dest, stack)) continue;
                    return this.packDest(p, dest);
                }
            }
        }
        org.millenaire.common.goal.GoalDiagnostics.note(villager, this, "NO_ELIGIBLE_COOKING_SLOT_OR_COLLECTABLE_OUTPUT");
        return null;
    }

    @Override
    public ItemStack getIcon() {
        if (this.icon != null) {
            return this.icon.getItemStack();
        }
        if (this.itemToCook != null) {
            return this.itemToCook.getItemStack();
        }
        return null;
    }

    @Override
    public String getTypeLabel() {
        return GOAL_TYPE;
    }

    @Override
    public boolean isDestPossibleSpecific(MillVillager villager, Building b) {
        return true;
    }

    @Override
    public boolean isPossibleGenericGoal(MillVillager villager) throws Exception {
        return this.getDestination(villager) != null;
    }

    public Point getWorkPosition(MillVillager villager) {
        Point furnace = villager.getGoalDestPoint();
        if (furnace == null) return null;
        var state = villager.level.getBlockState(furnace.getBlockPos());
        if (state.hasProperty(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING))
            return new Point(furnace.getBlockPos().relative(state.getValue(
                    net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING)));
        return furnace;
    }
    @Override public int range(MillVillager villager) { return 1; }
    @Override public boolean isWithinActionRange(MillVillager villager, Point ignored) {
        Point target = villager.getGoalDestPoint(), work = getWorkPosition(villager);
        if (target == null || work == null) return false;
        var standing = net.minecraft.world.phys.Vec3.atBottomCenterOf(work.getBlockPos());
        return villager.position().distanceToSqr(standing) < 1.5625
                && org.millenaire.common.goal.WorkAccess.visible(villager,
                    net.minecraft.world.phys.Vec3.atCenterOf(target.getBlockPos()), target.getBlockPos());
    }

    @Override
    public boolean performAction(MillVillager villager) throws Exception {
        if (!this.isWithinActionRange(villager, null)) return false;
        BlockEntity tileEntity = villager.getGoalDestPoint().getTileEntity(villager.level);
        Building dest = villager.getGoalBuildingDest();
        boolean firepitBurnable = TileEntityFirePit.isFirePitBurnable(villager.level, this.itemToCook.staticStack);
        if (tileEntity != null && dest != null) {
            if (tileEntity instanceof FurnaceBlockEntity) {
                FurnaceBlockEntity furnace = (FurnaceBlockEntity)tileEntity;
                this.performAction_furnace(dest, furnace, villager);
            } else if (firepitBurnable && tileEntity instanceof TileEntityFirePit) {
                TileEntityFirePit firepit = (TileEntityFirePit)tileEntity;
                this.performAction_firepit(dest, firepit, villager);
            }
        }
        return true;
    }

    private ItemStack fillCookingInput(Building dest, MillVillager villager, ItemStack current, int slotLimit) {
        ItemStack ingredient = org.millenaire.common.item.LegacyItemStacks.stack(
                this.itemToCook.getItem(), 1, this.itemToCook.meta);
        if (!current.isEmpty() && !ItemStack.isSameItemSameTags(current, ingredient)) return current;
        long available = (long)dest.countGoods(this.itemToCook) + villager.countInv(this.itemToCook);
        if (current.isEmpty() && available < this.minimumToCook) return current;
        int capacity = Math.min(slotLimit, ingredient.getMaxStackSize()) - current.getCount();
        int requested = (int)Math.max(0L, Math.min((long)capacity, available));
        if (requested == 0) return current;
        int taken = dest.takeGoods(this.itemToCook, requested);
        if (taken < requested) taken += villager.takeFromInv(this.itemToCook, requested - taken);
        if (taken == 0) return current;
        ItemStack result = current.isEmpty() ? ingredient : current.copy();
        result.setCount(current.getCount() + taken);
        return result;
    }

    private ItemStack storeCookingOutput(Building dest, ItemStack output) {
        if (output.isEmpty() || output.hasTag()) return output;
        int stored = dest.storeGoods(output.getItem(), output.getDamageValue(), output.getCount());
        if (stored == 0) return output;
        ItemStack remainder = output.copy();
        remainder.shrink(stored);
        return remainder;
    }

    private void performAction_firepit(Building dest, TileEntityFirePit firepit, MillVillager villager) {
        for (int slot = 0; slot < 3; slot++) {
            ItemStack old = firepit.inputs.getStackInSlot(slot);
            ItemStack updated = fillCookingInput(dest, villager, old, firepit.inputs.getSlotLimit(slot));
            if (updated != old) firepit.inputs.setStackInSlot(slot, updated);
        }
        for (int slot = 0; slot < 3; slot++) {
            ItemStack old = firepit.outputs.getStackInSlot(slot);
            ItemStack updated = storeCookingOutput(dest, old);
            if (updated != old) firepit.outputs.setStackInSlot(slot, updated);
        }
    }

    private void performAction_furnace(Building dest, FurnaceBlockEntity furnace, MillVillager villager) {
        ItemStack oldInput = furnace.getItem(0);
        ItemStack input = fillCookingInput(dest, villager, oldInput, furnace.getMaxStackSize());
        if (input != oldInput) furnace.setItem(0, input);
        ItemStack oldOutput = furnace.getItem(2);
        ItemStack output = storeCookingOutput(dest, oldOutput);
        if (output != oldOutput) furnace.setItem(2, output);
    }

    @Override
    public boolean validateGoal() {
        if (this.itemToCook == null) {
            MillLog.error(this, "The itemtocook id is mandatory in custom cooking goals.");
            return false;
        }
        return true;
    }
}

