package org.millenaire.common.goal.generic;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import org.millenaire.common.annotedparameters.AnnotedParameter;
import org.millenaire.common.annotedparameters.ConfigAnnotations;
import org.millenaire.common.block.WoodVariant;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.entity.TileEntityFirePit;
import org.millenaire.common.goal.Goal;
import org.millenaire.common.item.InvItem;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.village.Building;

public class GoalGenericTendFurnace extends GoalGeneric {
    public static final String GOAL_TYPE = "tendfurnace";
    @ConfigAnnotations.ConfigField(type=AnnotedParameter.ParameterType.INTEGER, defaultValue="4")
    @ConfigAnnotations.FieldDocumentation(explanation="Minimum number of wood to put back in one go.")
    public int minimumFuel;

    @Override public void applyDefaultSettings() {
        this.lookAtGoal = true;
        this.icon = InvItem.createInvItem(Blocks.FURNACE);
    }
    private static ItemStack fuel(BlockEntity entity) {
        if (entity instanceof FurnaceBlockEntity furnace) return furnace.getItem(1);
        if (entity instanceof TileEntityFirePit firepit) return firepit.fuel.getStackInSlot(0);
        return ItemStack.EMPTY;
    }
    private static WoodVariant fuelWood(ItemStack stack) {
        for (WoodVariant wood : WoodVariant.values()) if (stack.is(wood.planks().asItem())) return wood;
        return null;
    }
    private int woodCount(MillVillager villager, Building destination, WoodVariant wood) {
        var logs = wood.log().defaultBlockState();
        Building house = villager.getHouse();
        long count = (long)destination.countGoods(logs) + villager.countInv(logs);
        if (house != null && house != destination) count += house.countGoods(logs);
        return (int)Math.min(Integer.MAX_VALUE, Math.max(0L, count));
    }
    private WoodVariant chooseWood(MillVillager villager, Building destination, ItemStack current) {
        if (!current.isEmpty()) return fuelWood(current);
        WoodVariant best = null;
        int most = 0;
        for (WoodVariant wood : WoodVariant.values()) {
            int available = woodCount(villager, destination, wood);
            if (available > most) { best = wood; most = available; }
        }
        return best;
    }
    /** Output recovery is independent of recipe quotas, input batches and fuel. */
    static boolean canStoreOutput(Building destination, ItemStack output) {
        if (output.isEmpty() || output.hasTag()) return false;
        for (Point point : destination.getResManager().chests) {
            var chest = point.getMillChest(destination.world);
            if (chest == null) continue;
            for (int i = 0; i < chest.getContainerSize(); i++) {
                ItemStack stack = chest.getItem(i);
                if (stack.isEmpty() || (ItemStack.isSameItemSameTags(stack, output)
                        && stack.getCount() < stack.getMaxStackSize())) return true;
            }
        }
        return false;
    }

    private static ItemStack storeOutput(Building destination, ItemStack output) {
        if (!canStoreOutput(destination, output)) return output;
        int stored = destination.storeGoods(output.getItem(), output.getDamageValue(), output.getCount());
        if (stored <= 0) return output;
        ItemStack remainder = output.copy();
        remainder.shrink(stored);
        return remainder;
    }

    private static boolean hasCollectableOutput(Building destination, BlockEntity entity) {
        if (entity instanceof FurnaceBlockEntity furnace)
            return canStoreOutput(destination, furnace.getItem(2));
        if (entity instanceof TileEntityFirePit firepit) {
            for (int i = 0; i < 3; i++)
                if (canStoreOutput(destination, firepit.outputs.getStackInSlot(i))) return true;
        }
        return false;
    }

    private static void collectOutput(Building destination, BlockEntity entity) {
        if (entity instanceof FurnaceBlockEntity furnace) {
            ItemStack old = furnace.getItem(2), remainder = storeOutput(destination, old);
            if (old != remainder) { furnace.setItem(2, remainder); furnace.setChanged(); }
        } else if (entity instanceof TileEntityFirePit firepit) {
            for (int i = 0; i < 3; i++) {
                ItemStack old = firepit.outputs.getStackInSlot(i), remainder = storeOutput(destination, old);
                if (old != remainder) { firepit.outputs.setStackInSlot(i, remainder); firepit.setChanged(); }
            }
        }
    }

    private boolean needsFuel(MillVillager villager, Building destination, BlockEntity entity) {
        if (!(entity instanceof FurnaceBlockEntity) && !(entity instanceof TileEntityFirePit)) return false;
        ItemStack current = fuel(entity);
        if (current.getCount() >= 32) return false;
        WoodVariant wood = chooseWood(villager, destination, current);
        return wood != null && woodCount(villager, destination, wood) >= Math.max(1, minimumFuel);
    }
    @Override public Goal.GoalInformation getDestination(MillVillager villager) throws Exception {
        for (Building destination : getBuildings(villager)) {
            if (!isDestPossible(villager, destination)) continue;
            // Empty finished output even when fuel is already sufficient.
            for (Point point : destination.getResManager().furnaces)
                if (hasCollectableOutput(destination, point.getFurnace(villager.level))) return packDest(point, destination);
            for (Point point : destination.getResManager().firepits)
                if (hasCollectableOutput(destination, point.getFirePit(villager.level))) return packDest(point, destination);
            for (Point point : destination.getResManager().furnaces)
                if (needsFuel(villager, destination, point.getFurnace(villager.level))) return packDest(point, destination);
            for (Point point : destination.getResManager().firepits)
                if (needsFuel(villager, destination, point.getFirePit(villager.level))) return packDest(point, destination);
        }
        return null;
    }
    @Override public ItemStack[] getHeldItemsTravelling(MillVillager villager) {
        Building destination = villager.getGoalBuildingDest();
        Point point = villager.getGoalDestPoint();
        if (destination == null || point == null) return null;
        BlockEntity entity = point.getTileEntity(villager.level);
        if (!(entity instanceof FurnaceBlockEntity) && !(entity instanceof TileEntityFirePit)) return null;
        WoodVariant wood = chooseWood(villager, destination, fuel(entity));
        return wood == null ? null : new ItemStack[]{new ItemStack(wood.planks())};
    }
    @Override public String getTypeLabel() { return GOAL_TYPE; }
    @Override public boolean isDestPossibleSpecific(MillVillager villager, Building building) { return true; }
    @Override public boolean isPossibleGenericGoal(MillVillager villager) throws Exception { return getDestination(villager) != null; }
    @Override public boolean validateGoal() { return true; }

    @Override public boolean performAction(MillVillager villager) throws Exception {
        Building destination = villager.getGoalBuildingDest();
        Point point = villager.getGoalDestPoint();
        if (destination == null || point == null) return true;
        BlockEntity entity = point.getTileEntity(villager.level);
        if (!(entity instanceof FurnaceBlockEntity) && !(entity instanceof TileEntityFirePit)) return true;
        collectOutput(destination, entity);
        if (!needsFuel(villager, destination, entity)) return true;
        ItemStack current = fuel(entity);
        WoodVariant wood = chooseWood(villager, destination, current);
        if (wood == null) return true;
        int limit = Math.min(64, current.isEmpty() ? 64 : current.getMaxStackSize());
        if (entity instanceof TileEntityFirePit firepit) limit = Math.min(limit, firepit.fuel.getSlotLimit(0));
        int requestedLogs = Math.min(Math.max(0, limit - current.getCount()) / 4, woodCount(villager, destination, wood));
        if (requestedLogs <= 0) return true;
        var logs = wood.log().defaultBlockState();
        int taken = destination.takeGoods(logs, requestedLogs);
        if (taken < requestedLogs) taken += villager.takeFromInv(logs, requestedLogs - taken);
        Building house = villager.getHouse();
        if (taken < requestedLogs && house != null && house != destination)
            taken += house.takeGoods(logs, requestedLogs - taken);
        if (taken <= 0) return true;
        // Only consumed logs become fuel; stale availability must never create free planks.
        ItemStack replacement = current.isEmpty() ? new ItemStack(wood.planks(), taken * 4) : current.copy();
        if (!current.isEmpty()) replacement.grow(taken * 4);
        if (entity instanceof FurnaceBlockEntity furnace) furnace.setItem(1, replacement);
        else ((TileEntityFirePit)entity).fuel.setStackInSlot(0, replacement);
        entity.setChanged();
        return true;
    }
}
