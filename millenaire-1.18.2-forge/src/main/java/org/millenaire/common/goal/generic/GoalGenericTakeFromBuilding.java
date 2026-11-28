/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.init.Blocks
 */
package org.millenaire.common.goal.generic;

import java.util.HashMap;
import java.util.List;
import net.minecraft.world.level.block.Blocks;
import org.millenaire.common.annotedparameters.AnnotedParameter;
import org.millenaire.common.annotedparameters.ConfigAnnotations;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.goal.Goal;
import org.millenaire.common.goal.generic.GoalGeneric;
import org.millenaire.common.item.InvItem;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.village.Building;

public class GoalGenericTakeFromBuilding
extends GoalGeneric {
    public static final String GOAL_TYPE = "takefrombuilding";
    @ConfigAnnotations.ConfigField(type=AnnotedParameter.ParameterType.INTEGER, defaultValue="4")
    @ConfigAnnotations.FieldDocumentation(explanation="Minimum number of items to gather in one go.")
    public int minimumpickup;
    @ConfigAnnotations.ConfigField(type=AnnotedParameter.ParameterType.INVITEM_NUMBER_ADD, paramName="collect_good")
    @ConfigAnnotations.FieldDocumentation(explanation="Goods to be picked up from the target building, with maximum quantity to have in inventory.")
    public HashMap<InvItem, Integer> collectGoods = new HashMap();

    @Override
    public void applyDefaultSettings() {
        this.lookAtGoal = true;
        this.icon = InvItem.createInvItem(Blocks.FURNACE);
    }

    private boolean urgentBrickPickup(MillVillager villager) {
        return "gatherfrombrickkiln".equalsIgnoreCase(this.key)
                && this.collectGoods.keySet().stream().anyMatch(item ->
                    org.millenaire.common.goal.BrickProductionDemand.constructionMissing(villager,item)>0);
    }

    @Override
    public int priority(MillVillager villager) throws Exception {
        if (urgentBrickPickup(villager)) return 820;
        if ("gatherfrombrickkiln".equalsIgnoreCase(this.key)) return 400;
        return super.priority(villager);
    }

    @Override
    public Goal.GoalInformation getDestination(MillVillager villager) throws Exception {
        List<Building> buildings = this.getBuildings(villager);
        Building best = null;
        org.millenaire.common.utilities.Point bestPoint = null;
        for (Building dest : buildings) {
            if (!this.isDestPossible(villager, dest)) continue;
            int totalCount = 0;
            for (InvItem ii : this.collectGoods.keySet()) {
                int quantityNeeded = this.collectGoods.get(ii) - villager.countInv(ii);
                if (quantityNeeded <= 0) continue;
                totalCount += Math.min(quantityNeeded, dest.nbGoodAvailable(ii, false, false, false));
            }
            if (totalCount < (urgentBrickPickup(villager)?1:this.minimumpickup)) continue;
            var point=dest.getResManager().getSellingPos();
            if (point != null && (bestPoint == null || point.horizontalDistanceToSquared(villager)<bestPoint.horizontalDistanceToSquared(villager))) {
                best=dest; bestPoint=point;
            }
        }
        return best==null?null:this.packDest(bestPoint,best);
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

    @Override
    public boolean performAction(MillVillager villager) throws Exception {
        Building dest = villager.getGoalBuildingDest();
        if (dest != null) {
            for (InvItem ii : this.collectGoods.keySet()) {
                int quantityNeeded = this.collectGoods.get(ii) - villager.countInv(ii);
                if (quantityNeeded <= 0) continue;
                villager.takeFromBuilding(dest, ii.getItem(), ii.meta, Math.min(quantityNeeded, dest.nbGoodAvailable(ii, false, false, false)));
            }
        }
        return true;
    }

    @Override
    public boolean validateGoal() {
        if (this.collectGoods.size() == 0) {
            MillLog.error(this, "Generic take from building goals require at least one good to collect.");
            return false;
        }
        return true;
    }
}

