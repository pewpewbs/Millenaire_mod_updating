/*
 * Decompiled with CFR 0.152.
 */
package org.millenaire.common.goal;

import org.millenaire.common.config.DocumentedElement;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.goal.Goal;
import org.millenaire.common.item.InvItem;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.village.Building;

@DocumentedElement.Documentation(value="Takes items from the villager's house or the town hall that are needed in a shop. Paired with deliverresourcesshop that delivers them.")
public class GoalGetResourcesForShops
extends Goal {
    public GoalGetResourcesForShops() {
        this.travelBookShow = false;
    }

    @Override
    public int actionDuration(MillVillager villager) {
        return 40;
    }

    @Override
    public Goal.GoalInformation getDestination(MillVillager villager) {
        return this.getDestination(villager, false);
    }

    private int pickupTotal(MillVillager villager, Building source, java.util.List<Building> shops) {
        if (!LocalLogistics.usable(source)) return 0;
        long total = 0;
        for (InvItem item : source.stockKeys()) total += LocalLogistics.shopPickup(villager, source, item, shops);
        return (int)Math.min(Integer.MAX_VALUE,total);
    }

    public Goal.GoalInformation getDestination(MillVillager villager, boolean test) {
        if (villager.getHouse() == null || villager.getTownHall() == null) return null;
        boolean delayOver = !test || !villager.lastGoalTime.containsKey(this)
                || villager.level.getGameTime() > villager.lastGoalTime.get(this) + 2000L;
        var shops = villager.getTownHall().getShops();
        for (Building source : new Building[]{villager.getHouse(), villager.getTownHall()}) {
            int amount = pickupTotal(villager, source, shops);
            if (amount > 0 && (delayOver || amount > 16))
                return this.packDest(source.getResManager().getSellingPos(), source);
        }
        GoalDiagnostics.note(villager,this,"NO_EXPORTABLE_STOCK_OR_SHOP_CAPACITY");
        return null;
    }

    @Override
    public boolean isPossibleSpecific(MillVillager villager) {
        return this.getDestination(villager, true) != null;
    }

    @Override
    public boolean isWithinActionRange(MillVillager villager,
            org.millenaire.common.utilities.Point destination) {
        return WorkAccess.canAccessStock(villager, destination, this.range(villager));
    }

    @Override
    public boolean performAction(MillVillager villager) {
        Building source = villager.getGoalBuildingDest();
        if (source != villager.getHouse() && source != villager.getTownHall()) return true;
        if (!LocalLogistics.access(this,villager,source)) return false;
        var shops = villager.getTownHall().getShops();
        for (InvItem item : source.stockKeys()) {
            int amount = LocalLogistics.shopPickup(villager,source,item,shops);
            if (amount > 0) villager.takeFromBuilding(source,item.getItem(),item.meta,amount);
        }
        return true;
    }

    @Override
    public int priority(MillVillager villager) {
        if (villager.getHouse() == null || villager.getTownHall() == null) return 0;
        var shops = villager.getTownHall().getShops();
        long total = pickupTotal(villager,villager.getHouse(),shops);
        if (villager.getHouse() != villager.getTownHall())
            total += pickupTotal(villager,villager.getTownHall(),shops);
        return (int)Math.min(Integer.MAX_VALUE,total*5);
    }
}
