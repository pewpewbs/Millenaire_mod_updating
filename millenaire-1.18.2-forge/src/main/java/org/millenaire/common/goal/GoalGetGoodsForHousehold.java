/*
 * Decompiled with CFR 0.152.
 */
package org.millenaire.common.goal;

import java.util.HashMap;
import java.util.List;
import org.millenaire.common.config.DocumentedElement;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.goal.Goal;
import org.millenaire.common.item.InvItem;
import org.millenaire.common.village.Building;

@DocumentedElement.Documentation(value="Gets goods required by the household (food, inputs for crafting...) from the TH or shops and loads them in the inventory. Paired with delivergoodshousehold, which delivers them.")
public class GoalGetGoodsForHousehold
extends Goal {
    public GoalGetGoodsForHousehold() {
        this.travelBookShow = false;
    }

    @Override
    public Goal.GoalInformation getDestination(MillVillager villager) throws Exception {
        return this.getDestination(villager, false);
    }

    public Goal.GoalInformation getDestination(MillVillager villager, boolean test) throws Exception {
        Building home = villager.getHouse();
        if (!LocalLogistics.usable(home) || villager.getTownHall() == null) return null;
        var targets = LocalLogistics.householdTargets(villager);
        // Retry retained cargo before fetching more. The paired delivery goal
        // runs next, even when a previous delivery only partially succeeded.
        for (var entry : targets.entrySet())
            if (LocalLogistics.householdDelivery(villager,home,entry.getKey(),entry.getValue()) > 0)
                return this.packDest(home.getResManager().getSellingPos(),home);
        boolean delayOver = !test || !villager.lastGoalTime.containsKey(this)
                || villager.level.getGameTime() > villager.lastGoalTime.get(this) + 2000L;
        for (Building source : villager.getTownHall().getBuildings()) {
            long total = 0;
            for (var entry : targets.entrySet()) {
                if (!LocalLogistics.low(home,entry.getKey(),entry.getValue())) continue;
                total += LocalLogistics.householdPickup(villager,source,entry.getKey(),entry.getValue());
            }
            if (total > 0 && (delayOver || total > 16))
                return this.packDest(source.getResManager().getSellingPos(),source);
        }
        GoalDiagnostics.note(villager,this,"NO_HOUSEHOLD_DEFICIT_OR_AVAILABLE_SUPPLY_OR_STORAGE");
        return null;
    }

    @Override
    public boolean isPossibleSpecific(MillVillager villager) throws Exception {
        return this.getDestination(villager, true) != null;
    }

    @Override
    public boolean lookAtGoal() {
        return true;
    }

    @Override
    public String nextGoal(MillVillager villager) throws Exception {
        return Goal.deliverGoodsHousehold.key;
    }

    @Override
    public boolean isWithinActionRange(MillVillager villager,
            org.millenaire.common.utilities.Point destination) {
        return WorkAccess.canAccessStock(villager, destination, this.range(villager));
    }

    @Override
    public boolean performAction(MillVillager villager) throws Exception {
        Building source = villager.getGoalBuildingDest(), home = villager.getHouse();
        if (source == null || home == null) return true;
        if (!LocalLogistics.access(this,villager,source)) return false;
        if (source == home) return true; // nextGoal performs the deposit
        for (var entry : LocalLogistics.householdTargets(villager).entrySet()) {
            InvItem item = entry.getKey();
            int amount = LocalLogistics.householdPickup(villager,source,item,entry.getValue());
            if (amount > 0) villager.takeFromBuilding(source,item.getItem(),item.meta,amount);
        }
        return true;
    }

    @Override
    public int priority(MillVillager villager) throws Exception {
        Building home = villager.getHouse();
        if (!LocalLogistics.usable(home) || villager.getTownHall() == null) return 0;
        var targets = LocalLogistics.householdTargets(villager);
        long carried = 0, total = 0;
        for (var entry : targets.entrySet())
            carried += LocalLogistics.householdDelivery(villager,home,entry.getKey(),entry.getValue());
        if (carried > 0) return (int)Math.min(Integer.MAX_VALUE,100 + carried*20);
        var sources = villager.getTownHall().getBuildings();
        for (var entry : targets.entrySet()) {
            if (!LocalLogistics.low(home,entry.getKey(),entry.getValue())) continue;
            int best = 0;
            for (Building source : sources)
                best = Math.max(best,LocalLogistics.householdPickup(villager,source,entry.getKey(),entry.getValue()));
            total += best;
        }
        return (int)Math.min(Integer.MAX_VALUE,total*20);
    }
}
