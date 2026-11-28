/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.init.Blocks
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.util.EnumHand
 */
package org.millenaire.common.goal;

import java.util.ArrayList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import org.millenaire.common.block.MillBlocks;
import org.millenaire.common.config.DocumentedElement;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.goal.Goal;
import org.millenaire.common.item.InvItem;
import org.millenaire.common.item.MillItems;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.utilities.WorldUtilities;
import org.millenaire.common.village.Building;

@DocumentedElement.Documentation(value="Go and dry bricks in a building with the kiln tag.")
public class GoalIndianDryBrick
extends Goal {
    // A failed refill must not strand a full delivery batch behind an
    // unreachable point. The normal delivery goal remains eligible.
    private final java.util.Map<MillVillager,Long> refillRetryAfter = new java.util.WeakHashMap<>();

    private boolean refillTrip(MillVillager villager) {
        return BrickProductionDemand.refillBeforeDelivery(villager)
                || BrickProductionDemand.refillAfterGather(villager,this.key.equals(villager.goalKey));
    }

    private Building nearestLoadedKiln(MillVillager villager) {
        Building remembered = BrickProductionDemand.lastGatherKiln(villager);
        if (remembered != null) return remembered;
        Building best = null;
        double distance = Double.POSITIVE_INFINITY;
        for (Building kiln : villager.getTownHall().getBuildingsWithTag("brickkiln")) {
            for (Point spot : kiln.getResManager().brickspot) {
                if (spot == null || !villager.level.hasChunkAt(spot.getBlockPos())) continue;
                double d = spot.horizontalDistanceToSquared(villager);
                if (d < distance) { distance = d; best = kiln; }
            }
        }
        return best;
    }

    public GoalIndianDryBrick() {
        this.maxSimultaneousInBuilding = 1;
        this.tags.add("tag_construction");
        this.icon = InvItem.createInvItem(MillBlocks.BS_WET_BRICK);
    }

    @Override
    public int actionDuration(MillVillager villager) {
        return 20;
    }

    @Override
    public Goal.GoalInformation getDestination(MillVillager villager) throws MillLog.MillenaireException {
        boolean refill = refillTrip(villager);
        if (BrickProductionDemand.deliveryDue(villager) && !refill) return null;
        if (refill && villager.level.getGameTime() < refillRetryAfter.getOrDefault(villager,0L)) return null;
        var demand = BrickProductionDemand.inspect(villager);
        long drying = BrickProductionDemand.inDryingArea(villager);
        if (!demand.shouldForm(drying)) {
            GoalDiagnostics.note(villager,this,"BRICK_TARGET_COVERED shortage="+demand.shortage()+" drying="+drying);
            return null;
        }
        boolean minimumBricksNotRequired = refill || demand.urgent() || (villager.goalKey != null && villager.goalKey.equals(this.key) ? true : (!villager.lastGoalTime.containsKey(this) ? true : villager.level.getGameTime() > villager.lastGoalTime.get(this) + 2000L));
        ArrayList<Point> vp = new ArrayList<Point>();
        ArrayList<Point> buildingp = new ArrayList<Point>();
        // Refill only the closest working kiln, rather than crossing the village
        // to fill a different kiln before delivering the already collected batch.
        Building local = refill ? nearestLoadedKiln(villager) : null;
        for (Building kiln : villager.getTownHall().getBuildingsWithTag("brickkiln")) {
            if (refill && kiln != local) continue;
            Point p;
            if (!this.validateDest(villager, kiln)) continue;
            int nb = kiln.getResManager().getNbEmptyBrickLocation();
            boolean validTarget = false;
            if (nb > 0 && minimumBricksNotRequired) {
                validTarget = true;
            }
            if (nb > 4) {
                validTarget = true;
            }
            if (!validTarget) continue;
            p = null;
            for (Point spot : kiln.getResManager().brickspot) {
                if (spot == null || !villager.level.hasChunkAt(spot.getBlockPos())) continue;
                if (!villager.level.getBlockState(spot.getBlockPos()).isAir()) continue;
                if (p == null || spot.horizontalDistanceToSquared(villager) < p.horizontalDistanceToSquared(villager)) p = spot;
            }
            if (p == null) continue;
            vp.add(p);
            buildingp.add(kiln.getPos());
        }
        if (vp.isEmpty()) {
            return null;
        }
        Point p = (Point)vp.get(0);
        Point buildingP = (Point)buildingp.get(0);
        for (int i = 1; i < vp.size(); ++i) {
            if (!(((Point)vp.get(i)).horizontalDistanceToSquared((Entity)villager) < p.horizontalDistanceToSquared((Entity)villager))) continue;
            p = (Point)vp.get(i);
            buildingP = (Point)buildingp.get(i);
        }
        return this.packDest(p, buildingP);
    }

    @Override
    public ItemStack[] getHeldItemsTravelling(MillVillager villager) {
        return new ItemStack[]{org.millenaire.common.item.LegacyItemStacks.stack((Item)MillItems.BRICK_MOULD, 1, 0)};
    }

    @Override
    public boolean isPossibleSpecific(MillVillager villager) throws MillLog.MillenaireException {
        return this.getDestination(villager) != null;
    }

    @Override
    public boolean lookAtGoal() {
        return true;
    }

    @Override
    public boolean performAction(MillVillager villager) throws MillLog.MillenaireException {
        if (villager.level.isClientSide || villager.getGoalDestPoint() == null || villager.getGoalBuildingDest() == null
                || !villager.level.hasChunkAt(villager.getGoalDestPoint().getBlockPos())
                || !villager.getGoalBuildingDest().getResManager().brickspot.contains(villager.getGoalDestPoint())) return true;
        if (BrickProductionDemand.deliveryDue(villager) && !refillTrip(villager)) return true;
        if (!BrickProductionDemand.inspect(villager).shouldForm(BrickProductionDemand.inDryingArea(villager))) return true;
        if (WorldUtilities.getBlock(villager.level, villager.getGoalDestPoint()) == Blocks.AIR) {
            villager.setBlockstate(villager.getGoalDestPoint(), MillBlocks.BS_WET_BRICK);
            if (villager.level.getBlockState(villager.getGoalDestPoint().getBlockPos()).getBlock() != MillBlocks.WET_BRICK) {
                if (refillTrip(villager)) refillRetryAfter.put(villager,villager.level.getGameTime()+200L);
                return true;
            }
            villager.swing(InteractionHand.MAIN_HAND);
        }
        if (villager.getGoalBuildingDest().getResManager().getNbEmptyBrickLocation() > 0) {
            Goal.GoalInformation next = this.getDestination(villager);
            if (next == null) {
                BrickProductionDemand.clearGatherKiln(villager);
                return true;
            }
            villager.setGoalInformation(next);
            return false;
        }
        BrickProductionDemand.clearGatherKiln(villager);
        return true;
    }

    @Override
    public int priority(MillVillager villager) {
        var demand = BrickProductionDemand.inspect(villager);
        if (!demand.shouldForm(BrickProductionDemand.inDryingArea(villager))) return 0;
        if (refillTrip(villager)) return BrickProductionPolicy.REFILL_BEFORE_DELIVERY_PRIORITY;
        int p = demand.priority(false);
        for (MillVillager v : villager.getTownHall().getKnownVillagers()) {
            if (v == villager || !this.key.equals(v.goalKey)) continue;
            p = Math.max(1, p / 2);
        }
        return p;
    }

    @Override
    public boolean unreachableDestination(MillVillager villager) {
        if (refillTrip(villager)) refillRetryAfter.put(villager,villager.level.getGameTime()+200L);
        return false;
    }
}

