/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.init.Items
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.util.EnumHand
 */
package org.millenaire.common.goal;

import java.util.List;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import org.millenaire.common.config.DocumentedElement;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.goal.Goal;
import org.millenaire.common.item.InvItem;
import org.millenaire.common.utilities.MillCommonUtilities;
import org.millenaire.common.village.Building;

@DocumentedElement.Documentation(value="Fish from fishing holes at home, bringing in standard fish.")
public class GoalFish
extends Goal {


    public GoalFish() {
        this.buildingLimit.put(InvItem.createInvItem(Items.COD, 0), 128);
        this.buildingLimit.put(InvItem.createInvItem(Items.COOKED_COD, 0), 128);
        this.icon = InvItem.createInvItem((Item)Items.FISHING_ROD);
    }

    @Override
    public int actionDuration(MillVillager villager) {
        return 500;
    }

    protected void addFishResults(MillVillager villager) {
        InvItem cod = InvItem.createInvItem(Items.COD, 0);
        Building home = villager.getHouse(), destination = villager.getGoalBuildingDest();
        if (home == null || destination == null) return;
        int limit = this.buildingLimit.getOrDefault(cod, 128);
        long room = (long)limit - home.countGoods(cod) - villager.countInv(cod);
        if (destination != home) room = Math.min(room, (long)limit - destination.countGoods(cod));
        int amount = (int)Math.max(0L, Math.min(room, org.millenaire.common.config.TestTuning.resourceAmount(villager.level, 1)));
        if (amount > 0) villager.addToInv(Items.COD, amount);
    }

    @Override
    public Goal.GoalInformation getDestination(MillVillager villager) throws Exception {
        if (villager.getTownHall() == null) return null;
        List<Building> vb = villager.getTownHall().getBuildingsWithTag("fishingspot");
        Building closest = null;
        for (Building b : vb) {
            if (b.getResManager().fishingspots.isEmpty() || b.getResManager().getSleepingPos() == null) continue;
            if (!hasStockRoom(villager, b)) continue;
            if (closest != null && !(villager.getPos().horizontalDistanceToSquared(b.getResManager().getSleepingPos()) < villager.getPos().horizontalDistanceToSquared(closest.getResManager().getSleepingPos()))) continue;
            closest = b;
        }
        if (closest == null || closest.getResManager().fishingspots.size() == 0) {
            GoalDiagnostics.note(villager, this, "NO_ELIGIBLE_FISHING_SPOT");
            return null;
        }
        return this.packDest(closest.getResManager().fishingspots.get(MillCommonUtilities.randomInt(closest.getResManager().fishingspots.size())), closest);
    }

    private boolean hasStockRoom(MillVillager villager, Building destination) throws Exception {
        if (!validateDest(villager, destination)) return false;
        Building home = villager.getHouse();
        if (home == null || (home != destination && !validateDest(villager, home))) return false;
        for (var entry : this.buildingLimit.entrySet()) {
            InvItem item = entry.getKey();
            // Include unreturned catch so a fishing trip cannot overrun the
            // configured stock target before goods reach the chest.
            long stock = (long)home.countGoods(item) + villager.countInv(item);
            if (stock >= entry.getValue()) {
                if (GoalDiagnostics.recording(villager, this)) GoalDiagnostics.note(villager, this, "FISH_HOME_STOCK_LIMIT " + item + " " + stock + "/" + entry.getValue());
                return false;
            }
            if (destination != home && destination.countGoods(item) >= entry.getValue()) {
                if (GoalDiagnostics.recording(villager, this)) GoalDiagnostics.note(villager, this, "FISH_DESTINATION_STOCK_LIMIT " + item + " limit=" + entry.getValue());
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack[] getHeldItemsTravelling(MillVillager villager) throws Exception {
        return new ItemStack[]{new ItemStack(Items.FISHING_ROD)};
    }

    @Override
    public boolean isPossibleSpecific(MillVillager villager) throws Exception {
        return this.getDestination(villager) != null;
    }

    @Override
    public boolean lookAtGoal() {
        return true;
    }

    @Override
    public boolean performAction(MillVillager villager) throws Exception {
        if (villager.level.isClientSide) return true;
        Building destination = villager.getGoalBuildingDest();
        if (destination == null || !hasStockRoom(villager, destination)) return true;
        this.addFishResults(villager);
        villager.swing(InteractionHand.MAIN_HAND);
        return true;
    }

    @Override
    public int priority(MillVillager villager) throws Exception {
        // Availability already checks the actual fishing destination.
        // Use home stock, not the previously active goal's destination,
        // and do not repeat the fishing-spot search just for a priority.
        Building home = villager.getHouse();
        if (home == null) return 20;
        long raw = (long)home.countGoods(Items.COD) + villager.countInv(InvItem.createInvItem(Items.COD, 0));
        return (int)Math.max(1L, 100L - raw);
    }

    @Override
    public boolean stuckAction(MillVillager villager) throws Exception {
        return this.performAction(villager);
    }
}

