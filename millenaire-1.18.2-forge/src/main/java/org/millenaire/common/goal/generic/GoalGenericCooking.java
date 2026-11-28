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

    private int carriedForCooking(MillVillager villager) {
        int count = villager.countInv(this.itemToCook);
        if (org.millenaire.common.goal.BrickProductionDemand.isRawBrick(this.itemToCook))
            count = Math.max(0,count-org.millenaire.common.goal.BrickProductionDemand.constructionMissing(villager,this.itemToCook));
        return count;
    }

    private boolean brickKilnSupply(MillVillager villager) {
        return "cookindianbrick".equals(this.key)
                && org.millenaire.common.goal.BrickProductionDemand.brickWorker(villager)
                && villager.getTownHall()!=null;
    }

    private int availableBrickSupply(MillVillager villager) {
        if(!brickKilnSupply(villager))return 0;
        Building th=villager.getTownHall();
        Point stock=th.getResManager().getSellingPos();
        if(stock==null || !villager.level.hasChunkAt(stock.getBlockPos()))return 0;
        // The non-construction availability path already reserves active
        // and pending building plans, household and trade reservations.
        return th.nbGoodAvailable(this.itemToCook,false,false,false);
    }

    private boolean atBrickSupply(MillVillager villager) {
        return villager != null && "cookindianbrick".equals(this.key)
                && villager.townHallPoint != null
                && villager.townHallPoint.equals(villager.getGoalBuildingDestPoint());
    }

    @Override public String labelKey(MillVillager villager) {
        return atBrickSupply(villager) ? "getbrickforfiring" : super.labelKey(villager);
    }

    @Override public String labelKeyWhileTravelling(MillVillager villager) {
        return atBrickSupply(villager) ? "getbrickforfiring" : super.labelKeyWhileTravelling(villager);
    }

    @Override
    public int priority(MillVillager villager) throws Exception {
        if (brickKilnSupply(villager)) {
            // Finish a raw harvest, refill and deliver it to the town hall
            // first (1700/1600). Direct firing of eligible cargo (925) is
            // a fallback when a raw delivery is not currently due.
            if (carriedForCooking(villager)>0
                    && org.millenaire.common.goal.BrickProductionDemand.constructionMissing(villager,this.itemToCook)==0
                    && this.getDestination(villager, false, true)!=null) return 925;
            if (carriedForCooking(villager)>=this.minimumToCook
                    || availableBrickSupply(villager)>=this.minimumToCook) return 850;
        }
        return super.priority(villager);
    }

    @Override
    public Goal.GoalInformation getDestination(MillVillager villager) throws Exception {
        return getDestination(villager, true, false);
    }

    /** Input-only probes must not mistake a finished output for firing capacity. */
    private Goal.GoalInformation getDestination(MillVillager villager, boolean allowSupply, boolean inputOnly) throws Exception {
        List<Building> buildings = this.getBuildings(villager);
        Point best = null;
        Building bestBuilding = null;
        Point bestInput = null;
        Building bestInputBuilding = null;
        boolean supplyNeeded=false;
        boolean brick=brickKilnSupply(villager);
        int supply=allowSupply?availableBrickSupply(villager):0;
        for (Building dest : buildings) {
            if (!this.isDestPossible(villager, dest)) continue;
            boolean allowInput = passesWorkplaceBalance(villager, dest);
            if (!allowInput)
                org.millenaire.common.goal.GoalDiagnostics.note(villager, this, "WORKPLACE_RESOURCE_BALANCE");
            int countGoods = dest.countGoods(this.itemToCook) + carriedForCooking(villager);
            if (org.millenaire.common.goal.GoalDiagnostics.recording(villager, this)) {
                if (countGoods < this.minimumToCook)
                    org.millenaire.common.goal.GoalDiagnostics.note(villager, this, "INPUT_BELOW_MINIMUM " + this.itemToCook + " " + countGoods + "/" + this.minimumToCook);
                if (dest.getResManager().furnaces.isEmpty() && dest.getResManager().firepits.isEmpty())
                    org.millenaire.common.goal.GoalDiagnostics.note(villager, this, "NO_REGISTERED_FURNACE_OR_FIREPIT");
            }
            for (Point p : dest.getResManager().furnaces) {
                if (p==null || !villager.level.hasChunkAt(p.getBlockPos())) continue;
                FurnaceBlockEntity furnace = p.getFurnace(villager.level);
                if (furnace == null) continue;
                ItemStack input = furnace.getItem(0);
                boolean inputSlot=allowInput && inputSlotDue(input, furnace.getMaxStackSize());
                int required = minimumBatch(furnace.getMaxStackSize());
                boolean canFill = countGoods >= required && inputSlot;
                if (canFill && (bestInput == null || p.horizontalDistanceToSquared(villager)<bestInput.horizontalDistanceToSquared(villager))) {
                    bestInput=p; bestInputBuilding=dest;
                }
                if (inputSlot && countGoods<required
                        && countGoods+(long)supply>=required && dest!=villager.getTownHall())
                    supplyNeeded=true;
                if (!canFill && !(FurnaceBatchAccess.furnaceOutputDue(villager.level, furnace)
                        && GoalGenericTendFurnace.canStoreOutput(dest, furnace.getItem(2)))) continue;
                if (best == null || p.horizontalDistanceToSquared(villager)<best.horizontalDistanceToSquared(villager)) {
                    best=p; bestBuilding=dest;
                }
            }
            boolean firepitBurnable = TileEntityFirePit.isFirePitBurnable(villager.level, this.itemToCook.staticStack);
            if (!firepitBurnable) continue;
            for (Point p : dest.getResManager().firepits) {
                if (p==null || !villager.level.hasChunkAt(p.getBlockPos())) continue;
                ItemStack stack;
                int slotNb;
                TileEntityFirePit firepit = p.getFirePit(villager.level);
                if (firepit == null) continue;
                for (slotNb = 0; slotNb < 3; ++slotNb) {
                    stack = firepit.inputs.getStackInSlot(slotNb);
                    int limit = firepit.inputs.getSlotLimit(slotNb);
                    if (!allowInput || countGoods < minimumBatch(limit) || !inputSlotDue(stack, limit)) continue;
                    if (bestInput == null || p.horizontalDistanceToSquared(villager)<bestInput.horizontalDistanceToSquared(villager)) {
                        bestInput=p; bestInputBuilding=dest;
                    }
                    if (best == null || p.horizontalDistanceToSquared(villager)<best.horizontalDistanceToSquared(villager)) {
                        best=p; bestBuilding=dest;
                    }
                }
                for (slotNb = 0; slotNb < 3; ++slotNb) {
                    stack = firepit.outputs.getStackInSlot(slotNb);
                    if (!FurnaceBatchAccess.firepitOutputDue(villager.level, firepit, slotNb)
                            || !GoalGenericTendFurnace.canStoreOutput(dest, stack)) continue;
                    if (best == null || p.horizontalDistanceToSquared(villager)<best.horizontalDistanceToSquared(villager)) {
                        best=p; bestBuilding=dest;
                    }
                }
            }
        }
        if ((brick || inputOnly) && bestInput != null) return this.packDest(bestInput,bestInputBuilding);
        if (inputOnly && !allowSupply) return null;
        if (!inputOnly && best != null) return this.packDest(best,bestBuilding);
        if(supplyNeeded && supply>0){
            Building th=villager.getTownHall();
            return this.packDest(th.getResManager().getSellingPos(),th);
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
        if (atBrickSupply(villager)) return furnace;
        var state = villager.level.getBlockState(furnace.getBlockPos());
        if (state.hasProperty(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING))
            return new Point(furnace.getBlockPos().relative(state.getValue(
                    net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING)));
        return furnace;
    }
    @Override public int range(MillVillager villager) { return 1; }
    @Override public boolean isWithinActionRange(MillVillager villager, Point ignored) {
        Point target = villager.getGoalDestPoint(), work = getWorkPosition(villager);
        if(atBrickSupply(villager))
            return target!=null && target.distanceTo(villager)<2.0
                    && org.millenaire.common.goal.WorkAccess.canAccessStock(villager,target,2.0);
        if (target == null || work == null) return false;
        var standing = net.minecraft.world.phys.Vec3.atBottomCenterOf(work.getBlockPos());
        return villager.position().distanceToSqr(standing) < 1.5625
                && org.millenaire.common.goal.WorkAccess.visible(villager,
                    net.minecraft.world.phys.Vec3.atCenterOf(target.getBlockPos()), target.getBlockPos());
    }

    @Override
    public boolean performAction(MillVillager villager) throws Exception {
        if (villager.level.isClientSide || this.itemToCook == null) return true;
        Building dest = villager.getGoalBuildingDest();
        Point target = villager.getGoalDestPoint();
        if(atBrickSupply(villager)){
            if(dest==null || target==null || !target.equals(dest.getResManager().getSellingPos())
                    || !villager.level.hasChunkAt(target.getBlockPos()))return true;
            if(!this.isWithinActionRange(villager,null))return false;
            // Revalidate actual input capacity, not merely collectable output.
            // If local input appeared, the resource trip is no longer needed.
            if(!this.passesStockLimits(villager))return true;
            Goal.GoalInformation next=this.getDestination(villager, true, true);
            if(next==null)return true;
            if(!dest.getPos().equals(next.getDestBuildingPos())){
                villager.setGoalInformation(next);
                return false;
            }
            int amount=Math.min(availableBrickSupply(villager),Math.max(0,64-villager.countInv(this.itemToCook)));
            if(amount<=0 || villager.takeFromBuilding(dest,this.itemToCook.getItem(),this.itemToCook.meta,amount)<=0)return true;
            next=this.getDestination(villager, false, true);
            if(next==null || dest.getPos().equals(next.getDestBuildingPos()))return true;
            villager.setGoalInformation(next);
            return false;
        }
        if (dest == null || target == null || !this.getBuildings(villager).contains(dest)
                || !villager.level.hasChunkAt(target.getBlockPos())
                || !(dest.getResManager().furnaces.contains(target) || dest.getResManager().firepits.contains(target))) return true;
        if (!this.isWithinActionRange(villager, null)) return false;
        BlockEntity tileEntity = target.getTileEntity(villager.level);
        // A stale stock limit must stop new input, never strand cooked output.
        boolean allowInput = this.passesStockLimits(villager) && this.validateDest(villager,dest)
                && this.passesWorkplaceBalance(villager,dest);
        boolean firepitBurnable = TileEntityFirePit.isFirePitBurnable(villager.level, this.itemToCook.staticStack);
        if (tileEntity != null && dest != null) {
            if (tileEntity instanceof FurnaceBlockEntity) {
                FurnaceBlockEntity furnace = (FurnaceBlockEntity)tileEntity;
                this.performAction_furnace(dest, furnace, villager, allowInput);
            } else if (firepitBurnable && tileEntity instanceof TileEntityFirePit) {
                TileEntityFirePit firepit = (TileEntityFirePit)tileEntity;
                this.performAction_firepit(dest, firepit, villager, allowInput);
            }
            // A cooking visit can also service fuel using the resident's own configured tending goal.
            for (Goal goal : villager.getGoals()) {
                if (goal instanceof GoalGenericTendFurnace tending)
                    tending.serviceFuelAt(villager, dest, target, tileEntity);
            }
        }
        return true;
    }

    private int minimumBatch(int slotLimit) {
        return FurnaceBatchPolicy.minimumBatch(this.minimumToCook,
                FurnaceBatchPolicy.capacity(slotLimit, this.itemToCook.staticStack.getMaxStackSize()));
    }

    private boolean inputSlotDue(ItemStack current, int slotLimit) {
        ItemStack ingredient = org.millenaire.common.item.LegacyItemStacks.stack(
                this.itemToCook.getItem(), 1, this.itemToCook.meta);
        return (current.isEmpty() || ItemStack.isSameItemSameTags(current, ingredient))
                && FurnaceBatchPolicy.inputDue(current.getCount(),
                    FurnaceBatchPolicy.capacity(slotLimit, ingredient.getMaxStackSize()));
    }

    /** Called only during a visit to the same registered cooking device. */
    void serviceInputAt(MillVillager villager, Building dest, Point target, BlockEntity entity) throws Exception {
        if (itemToCook == null || !getBuildings(villager).contains(dest) || !passesStockLimits(villager)
                || !passesResourceBalance(villager) || !passesWorkplaceBalance(villager, dest)
                || !isDestPossible(villager, dest)) return;
        if (entity instanceof FurnaceBlockEntity furnace && dest.getResManager().furnaces.contains(target)) {
            ItemStack old = furnace.getItem(0);
            ItemStack updated = fillCookingInput(dest, villager, old, furnace.getMaxStackSize());
            if (updated != old) { furnace.setItem(0, updated); furnace.setChanged(); }
        } else if (entity instanceof TileEntityFirePit firepit && dest.getResManager().firepits.contains(target)
                && TileEntityFirePit.isFirePitBurnable(villager.level, itemToCook.staticStack)) {
            for (int slot = 0; slot < 3; slot++) {
                ItemStack old = firepit.inputs.getStackInSlot(slot);
                ItemStack updated = fillCookingInput(dest, villager, old, firepit.inputs.getSlotLimit(slot));
                if (updated != old) firepit.inputs.setStackInSlot(slot, updated);
            }
        }
    }

    private ItemStack fillCookingInput(Building dest, MillVillager villager, ItemStack current, int slotLimit) {
        ItemStack ingredient = org.millenaire.common.item.LegacyItemStacks.stack(
                this.itemToCook.getItem(), 1, this.itemToCook.meta);
        if (!inputSlotDue(current, slotLimit)) return current;
        long available = (long)dest.countGoods(this.itemToCook) + carriedForCooking(villager);
        if (available < minimumBatch(slotLimit)) return current;
        int capacity = FurnaceBatchPolicy.capacity(slotLimit, ingredient.getMaxStackSize()) - current.getCount();
        int requested = (int)Math.max(0L, Math.min((long)capacity, available));
        if (requested == 0) return current;
        int taken = 0;
        if (brickKilnSupply(villager)) {
            taken = villager.takeFromInv(this.itemToCook, Math.min(requested,carriedForCooking(villager)));
            if (taken < requested) taken += dest.takeGoods(this.itemToCook, requested - taken);
        } else {
            taken = dest.takeGoods(this.itemToCook, requested);
            if (taken < requested) taken += villager.takeFromInv(this.itemToCook, Math.min(requested - taken,carriedForCooking(villager)));
        }
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

    private void performAction_firepit(Building dest, TileEntityFirePit firepit, MillVillager villager, boolean allowInput) {
        if (allowInput) for (int slot = 0; slot < 3; slot++) {
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

    private void performAction_furnace(Building dest, FurnaceBlockEntity furnace, MillVillager villager, boolean allowInput) {
        if (allowInput) {
            ItemStack oldInput = furnace.getItem(0);
            ItemStack input = fillCookingInput(dest, villager, oldInput, furnace.getMaxStackSize());
            if (input != oldInput) { furnace.setItem(0, input); furnace.setChanged(); }
        }
        ItemStack oldOutput = furnace.getItem(2);
        ItemStack output = storeCookingOutput(dest, oldOutput);
        if (output != oldOutput) { furnace.setItem(2, output); furnace.setChanged(); }
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

