package org.millenaire.common.goal;

import org.millenaire.common.block.MillBlocks;
import org.millenaire.common.buildingplan.BuildingPlan;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.item.InvItem;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.village.Building;

/** Read-only village demand shared by Indian and Seljuk brickmaking goals. */
final class BrickProductionDemand {
    private BrickProductionDemand() {}

    private static long required(BuildingPlan plan, InvItem item) {
        if (plan == null || plan.resCost == null) return 0;
        long total = 0;
        for (var entry : plan.resCost.entrySet())
            if (item.matches(entry.getKey())) total += Math.max(0, entry.getValue());
        return total;
    }

    private static BuildingPlan pendingPlan(Building townHall) {
        BuildingPlan plan = townHall.getCurrentGoalBuildingPlan();
        if (plan == null) return null;
        // Already-started projects have their own construction reservations.
        for (var construction : townHall.getConstructionsInProgress()) {
            var location = construction.getBuildingLocation();
            if (location != null && townHall.buildingGoalLocation != null
                    && location.isSameLocation(townHall.buildingGoalLocation)) return null;
        }
        return plan;
    }

    private static boolean carriesForStorage(MillVillager worker, InvItem item) {
        if (worker == null || worker.getGoals() == null
                || "construction".equalsIgnoreCase(worker.goalKey)
                || "getresourcesforbuild".equalsIgnoreCase(worker.goalKey)) return false;
        for (InvItem good : worker.getGoodsToBringBackHome())
            if (good.matches(item)) return true;
        return false;
    }

    static BrickProductionPolicy.Demand inspect(MillVillager villager) {
        Building townHall = villager.getTownHall();
        if (townHall == null) return new BrickProductionPolicy.Demand(0, 0);
        InvItem raw = InvItem.createInvItem(MillBlocks.BS_MUD_BRICK);
        InvItem fired = InvItem.createInvItem(MillBlocks.PAINTED_BRICK_WHITE);
        BuildingPlan plan = pendingPlan(townHall);
        boolean makesFired = villager.getGoals() != null
                && villager.getGoals().contains(Goal.goals.get("cookindianbrick"));
        long rawRequired = required(plan, raw);
        long firedRequired = makesFired ? required(plan, fired) : 0;
        long otherRaw = 0, otherFired = 0;
        // Count stored and transportable supplies once; don't generate another
        // batch solely because the previous one hasn't reached the town hall.
        for (Building building : townHall.getBuildings()) {
            if (building == townHall || building.getPos() == null
                    || !villager.level.hasChunkAt(building.getPos().getBlockPos())) continue;
            otherRaw += building.countGoods(raw);
            if (makesFired) otherFired += building.countGoods(fired);
        }
        for (MillVillager worker : townHall.getKnownVillagers()) {
            if (carriesForStorage(worker, raw)) otherRaw += worker.countInv(raw);
            if (makesFired && carriesForStorage(worker, fired)) otherFired += worker.countInv(fired);
        }
        if (!townHall.getKnownVillagers().contains(villager)) {
            if (carriesForStorage(villager, raw)) otherRaw += villager.countInv(raw);
            if (makesFired && carriesForStorage(villager, fired)) otherFired += villager.countInv(fired);
        }
        var tradeGood = townHall.culture.getTradeGood(raw);
        long reserve = tradeGood == null ? BrickProductionPolicy.RESERVE
                : Math.max(BrickProductionPolicy.RESERVE, tradeGood.targetQuantity);
        return BrickProductionPolicy.demand(rawRequired, townHall.availableForCurrentProject(raw), otherRaw,
                firedRequired, makesFired ? townHall.availableForCurrentProject(fired) : 0, otherFired, reserve);
    }

    /** Count only existing wet or dried blocks on registered, loaded drying spots. */
    static long inDryingArea(MillVillager villager) {
        Building townHall = villager.getTownHall();
        if (townHall == null) return 0;
        java.util.Set<Point> counted = new java.util.HashSet<>();
        long total = 0;
        for (Building kiln : townHall.getBuildingsWithTag("brickkiln")) {
            for (Point point : kiln.getResManager().brickspot) {
                if (point == null || !villager.level.hasChunkAt(point.getBlockPos()) || !counted.add(point)) continue;
                var state = villager.level.getBlockState(point.getBlockPos());
                if (state.getBlock() == MillBlocks.WET_BRICK || state == MillBlocks.BS_MUD_BRICK) total++;
            }
        }
        return total;
    }
}
