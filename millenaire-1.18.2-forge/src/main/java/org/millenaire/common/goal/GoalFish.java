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
        villager.addToInv(Items.COD, 1);
    }

    @Override
    public Goal.GoalInformation getDestination(MillVillager villager) throws Exception {
        if (villager.getTownHall() == null) return null;
        List<Building> vb = villager.getTownHall().getBuildingsWithTag("fishingspot");
        Building closest = null;
        for (Building b : vb) {
            if (b.getResManager().fishingspots.isEmpty() || b.getResManager().getSleepingPos() == null) continue;
            if (closest != null && !(villager.getPos().horizontalDistanceToSquared(b.getResManager().getSleepingPos()) < villager.getPos().horizontalDistanceToSquared(closest.getResManager().getSleepingPos()))) continue;
            closest = b;
        }
        if (closest == null || closest.getResManager().fishingspots.size() == 0) {
            return null;
        }
        return this.packDest(closest.getResManager().fishingspots.get(MillCommonUtilities.randomInt(closest.getResManager().fishingspots.size())), closest);
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
        this.addFishResults(villager);
        villager.swing(InteractionHand.MAIN_HAND);
        return true;
    }

    @Override
    public int priority(MillVillager villager) throws Exception {
        if (villager.getGoalBuildingDest() == null) {
            return 20;
        }
        return 100 - villager.getGoalBuildingDest().countGoods(Items.COD);
    }

    @Override
    public boolean stuckAction(MillVillager villager) throws Exception {
        return this.performAction(villager);
    }
}

