/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.item.ItemStack
 */
package org.millenaire.common.goal;

import java.util.ArrayList;
import net.minecraft.world.item.ItemStack;
import org.millenaire.common.config.DocumentedElement;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.goal.Goal;
import org.millenaire.common.item.InvItem;

@DocumentedElement.Documentation(value="Brings resources home from the villager's inventory. Typically used to bring back resources produced by mining, crafting etc.")
public class GoalBringBackResourcesHome
extends Goal {
    public GoalBringBackResourcesHome() {
        this.travelBookShow = false;
    }

    private static boolean isDeliverable(MillVillager villager, InvItem key) {
        for (InvItem configured : villager.getGoodsToBringBackHome()) {
            if (configured.matches(key)) return true;
        }
        // Old culture files usually list white wool only. Shepherds still
        // shear other coloured sheep, and must be able to store that wool.
        return GoalShearSheep.hasShearingGoal(villager) && GoalShearSheep.isSheepWool(key);
    }

    private static int countDeliverableGoods(MillVillager villager) {
        int total = 0;
        for (InvItem key : villager.getInventoryKeys()) {
            if (isDeliverable(villager, key) && villager.getHouse() != null)
                total += Math.min(villager.countInv(key), villager.getHouse().storageRoom(key));
        }
        return total;
    }

    private static boolean fullShearingBatch(MillVillager villager) {
        return GoalShearSheep.hasShearingGoal(villager)
                && GoalShearSheep.woolInInventory(villager) >= GoalShearSheep.WOOL_DELIVERY_BATCH;
    }

    @Override
    public int actionDuration(MillVillager villager) {
        return 40;
    }

    @Override
    public Goal.GoalInformation getDestination(MillVillager villager) {
        return this.packDest(villager.getHouse().getResManager().getSellingPos(), villager.getHouse());
    }

    @Override
    public ItemStack[] getHeldItemsTravelling(MillVillager villager) {
        ArrayList<ItemStack> items = new ArrayList<ItemStack>();
        for (InvItem key : villager.getInventoryKeys()) {
            if (villager.countInv(key) > 0 && isDeliverable(villager, key)) {
                items.add(org.millenaire.common.item.LegacyItemStacks.stack(key.getItem(), 1, key.meta));
            }
        }
        return items.toArray(new ItemStack[items.size()]);
    }

    @Override
    public boolean isPossibleSpecific(MillVillager villager) {
        if (villager.getGoodsToBringBackHome().isEmpty()
                && !GoalShearSheep.hasShearingGoal(villager)) return false;
        int nb = countDeliverableGoods(villager);
        if (nb <= 0) return GoalDiagnostics.reject(villager, this, "NO_DELIVERABLE_GOODS_OR_HOME_STORAGE");
        // Override the old 100-second cooldown once a shepherd has a full
        // batch. Otherwise a large flock repeatedly wins the goal selection.
        if (fullShearingBatch(villager)) return true;
        boolean delayOver = !villager.lastGoalTime.containsKey(this)
                || villager.level.getGameTime() > villager.lastGoalTime.get(this) + 2000L;
        return delayOver || nb > 16;
    }

    @Override
    public boolean lookAtGoal() {
        return true;
    }

    @Override
    public boolean isWithinActionRange(MillVillager villager, org.millenaire.common.utilities.Point destination) {
        return destination != null && destination.distanceTo(villager) < this.range(villager)
                && WorkAccess.canAccessStock(villager, destination, this.range(villager));
    }

    @Override
    public boolean performAction(MillVillager villager) {
        // Never deposit remotely at an intermediate route endpoint.
        org.millenaire.common.utilities.Point destination = villager.getGoalDestPoint();
        if (!LocalLogistics.access(this,villager,villager.getHouse())) return false;
        for (InvItem key : villager.getInventoryKeys()) {
            if (isDeliverable(villager, key) && villager.countInv(key) > 0) {
                int amount = Math.min(256, villager.getHouse().storageRoom(key));
                if (amount > 0) villager.putInBuilding(villager.getHouse(), key.getItem(), key.meta, amount);
            }
        }
        return true;
    }

    @Override
    public int priority(MillVillager villager) {
        int nbGoods = countDeliverableGoods(villager);
        if (fullShearingBatch(villager)) {
            // Breeding has priority 10000. Finished wool batches must take
            // precedence even when animals remain ready for breeding/shearing.
            return 12000 + nbGoods * 3;
        }
        return 10 + nbGoods * 3;
    }
}

